package net.lecousin.framework.concurrent.threads;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Cancellable;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.util.AttributesContainer;
import net.lecousin.framework.util.Pair;

/** Task to be executed asynchronously.
 * @param <T> type of result
 * @param <TError> type of error
 */
@SuppressWarnings("squid:S1192") // string Task appears several times
public final class Task<T,TError extends Exception> implements Cancellable {

	public static final int BACKGROUND_VALUE = 100;
	
	/** Priority of a task. */
	public enum Priority {
		URGENT(0),
		IMPORTANT(1),
		RATHER_IMPORTANT(2),
		NORMAL(3),
		RATHER_LOW(4),
		LOW(5),
		BACKGROUND(BACKGROUND_VALUE);
		
		public static final int NB = 6;
		
		Priority(int value) {
			this.value = value;
		}
		
		private int value;
		
		public int getValue() {
			return value;
		}
		
		/** Return the previous priority. */
		public Priority less() {
			switch (this) {
			case URGENT: return IMPORTANT;
			case IMPORTANT: return RATHER_IMPORTANT;
			case RATHER_IMPORTANT: return NORMAL;
			case NORMAL: return RATHER_LOW;
			default: return LOW;
			}
		}
		
		/** Return the next priority. */
		public Priority more() {
			switch (this) {
			case BACKGROUND: return LOW;
			case LOW: return RATHER_LOW;
			case RATHER_LOW: return NORMAL;
			case NORMAL: return RATHER_IMPORTANT;
			case RATHER_IMPORTANT: return IMPORTANT;
			default: return URGENT;
			}
		}
	}
	
	public static class Context implements AttributesContainer {
		private Application application;
		private Map<String, Object> attributes;

		public Application getApplication() {
			return application;
		}

		@Override
		public void setAttribute(String name, Object value) {
			attributes.put(name, value);
		}

		@Override
		public Object getAttribute(String name) {
			return attributes.get(name);
		}

		@Override
		public Object removeAttribute(String name) {
			return attributes.remove(name);
		}

		@Override
		public boolean hasAttribute(String name) {
			return attributes.containsKey(name);
		}
	}
	
	/** Get the priority of the current task. */
	public static Priority getCurrentPriority() {
		Task<?,?> current = Threading.currentTask();
		return current != null ? current.getPriority() : Priority.NORMAL;
	}
	
	/** Get the context of the current task. */
	public static Context getCurrentContext() {
		Task<?,?> current = Threading.currentTask();
		return current != null ? current.context : null;
	}
	
	/** Constructor. */
	public Task(TaskManager manager, String description, Priority priority, Executable<T, TError> executable, Consumer<Pair<T,TError>> ondone) {
		if (manager == null) manager = Threading.getUnmanagedTaskManager();
		Task<?, ?> parent = Threading.currentTask();
		if (parent != null) {
			context = parent.context;
			if (priority == null)
				priority = parent.getPriority();
		} else {
			Application app = LCCore.getApplication();
			if (app == null) throw new IllegalStateException("No application in the context");
			context = new Context();
			context.application = app;
			context.attributes = new HashMap<>(10);
			if (priority == null)
				priority = Priority.NORMAL;
		}
		this.manager = manager;
		this.description = description;
		this.priority = priority;
		this.executable = executable;
		this.ondone = ondone;
		result.onDone(() -> {
			if (result.isCancelled() && status < STATUS_RUNNING) {
				Logger logger = context.application.getDefaultLogger();
				if (logger.debug()) {
					CancelException reason = result.getCancelEvent();
					logger.debug("Task cancelled: " + description + " => "
						+ (reason != null ? reason.getMessage() : "No reason given"));
				}
				cancel(result.getCancelEvent());
			}
			status = STATUS_DONE;
		});
	}
	
	private Context context;
	
	// task manager
	private TaskManager manager; 
	
	// status
	byte status;

	/** not yet started. */
	static final byte STATUS_NOT_STARTED = 0;
	/** started but waiting for a specific time. */
	static final byte STATUS_STARTED_WAITING = 1;
	/** started and put in the ready list of the task manager. */
	static final byte STATUS_STARTED_READY = 2;
	/** started and still running. */
	static final byte STATUS_RUNNING = 3;
	/** started but blocked (waiting for something). */
	static final byte STATUS_BLOCKED = 4;
	/** executed, but some sub-tasks are not yet done. */
	static final byte STATUS_EXECUTED = 5;
	/** done. */
	static final byte STATUS_DONE = 6;
	
	// executable
	private Executable<T, TError> executable;
	
	// result of execution
	private Output result = new Output();
	private CancelException cancelling = null;
	private Consumer<Pair<T,TError>> ondone = null;
	
	// timing and info
	private String description;
	private Priority priority;
	private long executeEvery;
	long nextExecution;
	private long maxBlockingTimeInNanoBeforeToLog = 100000000;
	
	// hold synchronization points
	private List<IAsync<?>> holdSP = null;
	
	
	// --- general info ---
	
	public Context getContext() {
		return context;
	}
	
	public Task<T, TError> resetContext() {
		context.attributes = new HashMap<>(10);
		return this;
	}
	
	public Task<T, TError> setContext(String name, Object value) {
		context.setAttribute(name, value);
		return this;
	}
	
	public String getDescription() {
		return description;
	}
	
	public Application getApplication() {
		return context.application;
	}
	
	public TaskManager getTaskManager() {
		return manager;
	}

	public long getMaxBlockingTimeInNanoBeforeToLog() {
		return maxBlockingTimeInNanoBeforeToLog;
	}
	
	/**
	 * Set the maximum time the time can block before to log a warning.
	 * @param nano nanoseconds
	 * @return this task
	 */
	public Task<T, TError> setMaxBlockingTimeInNanoBeforeToLog(long nano) {
		maxBlockingTimeInNanoBeforeToLog = nano;
		return this;
	}

	
	// --- status, cancel, error, result ---
	
	
	public boolean isDone() {
		return status == STATUS_DONE && result.isDone();
	}
	
	public boolean isSuccessful() {
		return status == STATUS_DONE && result.isSuccessful();
	}
	
	public boolean isCancelling() {
		return cancelling != null || result.isCancelled();
	}
	
	@Override
	public boolean isCancelled() {
		return result.isCancelled();
	}
	
	public boolean isStarted() {
		return status > STATUS_NOT_STARTED;
	}
	
	public boolean isRunning() {
		return status >= STATUS_RUNNING && status < STATUS_DONE;
	}
	
	/** Cancel this task only if not yet started.
	 * @return true if the task has been cancelled
	 */
	public boolean cancelIfExecutionNotStarted(CancelException reason) {
		synchronized (this) {
			if (status < STATUS_RUNNING) {
				cancel(reason);
				return true;
			}
		}
		return false;
	}
	
	/** Cancel this task. */
	@Override
	public void cancel(CancelException reason) {
		if (cancelling != null)
			return;
		if (reason == null) reason = new CancelException("No reason given");
		cancelling = reason;
		if (TaskScheduler.cancel(this) || manager.remove(this) || status == Task.STATUS_NOT_STARTED) {
			status = Task.STATUS_DONE;
			result.cancelled(reason);
		}
	}
	
	@Override
	public CancelException getCancelEvent() { return cancelling != null ? cancelling : result.getCancelEvent(); }

	public AsyncSupplier<T, TError> getOutput() { return result; }
	
	/** Set this task as done with the given result or error. */
	public Task<T, TError> setDone(T result, TError error) {
		this.status = STATUS_DONE;
		if (error == null)
			this.result.unblockSuccess(result);
		else
			this.result.unblockError(error);
		return this;
	}
	
	/** Start the given task once this task is done. */
	public Task<T, TError> ondone(Task<?,?> todo, boolean evenIfErrorOrCancel) {
		if (result.isCancelled()) {
			todo.cancel(result.getCancelEvent());
		} else if (result.hasError()) {
			todo.cancel(new CancelException(result.getError()));
		} else if (result.isDone()) {
			todo.start();
		} else {
			todo.startOn(result, evenIfErrorOrCancel);
		}
		return this;
	}
	
	
	// --- priority ---
	

	@SuppressWarnings("squid:S2886") // no need for synchronized
	public Priority getPriority() { return priority; }
	
	/** Change the priority of this task. */
	public synchronized Task<T, TError> setPriority(Priority priority) {
		if (this.priority == priority) return this;
		if (status == STATUS_STARTED_READY && manager.remove(this)) {
			this.priority = priority;
			while (manager.getTransferTarget() != null)
				manager = manager.getTransferTarget();
			manager.addReady(this);
			return this;
		}
		this.priority = priority;
		return this;
	}
	
	/** Ensure that when this task will be done, successfully or not, the given synchronization point
	 * are unblocked. If some are not, they are cancelled upon task completion.
	 */
	public Task<T,TError> ensureUnblocked(IAsync<?>... sp) {
		if (status == STATUS_DONE) {
			for (int i = 0; i < sp.length; ++i)
				if (!sp[i].isDone())
					sp[i].cancel(new CancelException("Task " + description + " done without unblock this synchronization point"));
			return this;
		}
		if (holdSP == null) holdSP = new ArrayList<>(sp.length + 2);
		Collections.addAll(holdSP, sp);
		return this;
	}
	
	
	// --- schedule ---
	
	public long getRepetitionDelay() {
		return executeEvery;
	}
	
	/** Execute this task at the given time. */
	public Task<T, TError>  executeAt(long time) {
		nextExecution = time;
		return this;
	}
	
	/** Execute this task in the given delay in milliseconds. */
	public Task<T, TError> executeIn(long delay) {
		return executeAt(System.currentTimeMillis() + delay);
	}
	
	/** Execute this task repeatedly. */
	public Task<T, TError>  executeEvery(long delay, long initialDelay) {
		executeEvery = delay;
		return executeIn(initialDelay);
	}
	
	/** Do not execute this task again. */
	public void stopRepeat() {
		executeEvery = 0;
	}
	
	/** Execute again this task in the given delay. */
	public Task<T, TError>  executeAgainIn(long delay) {
		return executeAgainAt(System.currentTimeMillis() + delay);
	}

	/** Execute again this task at the given time. */
	public Task<T, TError>  executeAgainAt(long time) {
		return executeAt(time);
	}
	
	/** Change the next execution time of scheduled or repetitive task. */
	public synchronized Task<T, TError> changeNextExecutionTime(long time) {
		if (status == STATUS_STARTED_WAITING)
			TaskScheduler.changeNextExecutionTime(this, time);
		else
			nextExecution = time;
		return this;
	}
	
	/** Change the next execution time to now. */
	public synchronized void executeNextOccurenceNow() {
		long now = System.currentTimeMillis();
		if (nextExecution > now)
			changeNextExecutionTime(System.currentTimeMillis());
	}
	
	/** Change the next execution time to now with the given priority. */
	public synchronized void executeNextOccurenceNow(Priority priority) {
		setPriority(priority);
		executeNextOccurenceNow();
	}
	
	
	// --- start ---
	
	/** Ask to start the task. This does not start it immediately, but adds it to the queue of tasks to execute. */
	@SuppressWarnings("squid:S00112") // RuntimeException
	public Task<T,TError> start() {
		if (result.isDone()) return this;
		if (cancelling != null) result.cancelled(cancelling);
		synchronized (this) {
			if (result.isDone()) return this;
			if (status != Task.STATUS_NOT_STARTED) {
				if (status >= Task.STATUS_DONE)
					throw new RuntimeException("Task already done: " + description
						+ " with " + (result.getError() != null ? "error " + result.getError().getMessage() : "success"));
				throw new RuntimeException("Task already started (" + status + "): " + description);
			}
			// check if waiting for time
			if (nextExecution > 0) {
				long now = System.currentTimeMillis();
				if (nextExecution > now) {
					TaskScheduler.schedule(this);
					return this;
				}
			}
			// ready to be executed
			sendToTaskManager();
			return this;
		}
	}
	
	
	/** Start this task once the given synchronization point is unblocked. */
	public Task<T, TError> startOn(IAsync<? extends Exception> sp, boolean evenOnErrorOrCancel) {
		sp.onDone(() -> {
			if (evenOnErrorOrCancel) {
				start();
				return;
			}
			if (sp.isCancelled())
				cancel(sp.getCancelEvent());
			else if (sp.hasError()) {
				try {
					@SuppressWarnings("unchecked") TError err = (TError)sp.getError();
					status = STATUS_DONE;
					result.unblockError(err);
				} catch (ClassCastException e) {
					cancel(new CancelException("Error while waiting", sp.getError()));
				}
			} else {
				start();
			}
		});
		return this;
	}
	
	/** Start this task once all the given synchronization points are unblocked. */
	public Task<T, TError> startOn(boolean evenOnErrorOrCancel, IAsync<?>... list) {
		JoinPoint<Exception> jp = new JoinPoint<>();
		for (IAsync<? extends Exception> sp : list)
			if (sp != null)
				jp.addToJoin(sp);
		jp.start();
		jp.onDone(() -> {
			if (evenOnErrorOrCancel) {
				start();
				return;
			}
			if (jp.isCancelled())
				cancel(jp.getCancelEvent());
			else if (jp.hasError()) {
				try {
					@SuppressWarnings("unchecked") TError err = (TError)jp.getError();
					status = STATUS_DONE;
					result.unblockError(err);
				} catch (ClassCastException e) {
					cancel(new CancelException("Error while waiting", jp.getError()));
				}
			} else {
				start();
			}
		});
		return this;
	}
	
	/** Start this task once the given task is done. If the given task is null or already done, the method start() is called. */
	public Task<T, TError> startAfter(Task<?,?> task) {
		if (task == null || task.isDone())
			return start();
		return startOn(task.getOutput(), true);
	}
	
	/** Called by start or TaskScheduler. */
	void sendToTaskManager() {
		while (manager.getTransferTarget() != null)
			manager = manager.getTransferTarget();
		status = Task.STATUS_STARTED_READY;
		manager.addReady(this);
	}

	
	// --- methods for TaskManager ---
	
	
	void transferTo(TaskManager newManager) {
		manager = newManager;
	}

	void cancelledBecauseExecutorDied(CancelException reason) {
		if (cancelling != null)
			reason = cancelling;
		else
			cancelling = reason;
		result.cancel(reason);
		status = STATUS_DONE;
		result.cancelled(reason);
	}
	
	
	// --- methods for TaskExecutor ---
	
	
	@SuppressWarnings({"unchecked", "java:S3776", "java:S2583"})
	void execute() {
		if (cancelling != null) {
			status = STATUS_DONE;
			result.cancelled(cancelling);
			checkSP();
			return;
		}
		T res;
		try { res = executable.execute(this); }
		catch (CancelException e) {
			status = STATUS_DONE;
			cancelling = e;
			result.cancelled(e);
			checkSP();
			return;
		} catch (Exception t) {
			status = STATUS_DONE;
			if (cancelling != null) {
				if (!result.isCancelled()) result.cancelled(cancelling);
				if (context.application.isDebugMode())
					context.application.getDefaultLogger()
						.warn("Task " + description + " error while trying to cancel it: " + t.getMessage()
						+ ", cancellation reason is " + result.getCancelEvent().getMessage(), t);
			} else if (!result.isCancelled()) {
				if (context.application.isDebugMode())
					context.application.getDefaultLogger().error("Task " + description + " error: " + t.getMessage(), t);
				try {
					TError error = (TError)t;
					if (ondone != null) ondone.accept(new Pair<>(null, error));
					result.unblockError(error);
				} catch (ClassCastException e) {
					cancelling = new CancelException("Unexpected exception thrown", t);
					result.cancelled(cancelling);
				} catch (Exception e) {
					cancelling = new CancelException("Unexpected exception thrown", e);
					result.cancelled(cancelling);
				}
			} else {
				if (context.application.isDebugMode())
					context.application.getDefaultLogger()
						.warn("Task " + description + " error after being cancelled: " + t.getMessage()
						+ ", cancellation reason is " + result.getCancelEvent().getMessage(), t);
			}
			checkSP();
			return;
		}
		status = STATUS_DONE;
		try { if (ondone != null) ondone.accept(new Pair<>(res, null)); }
		catch (Exception t) {
			context.application.getDefaultLogger().error("Error while calling ondone on task " + description, t);
		}
		result.unblockSuccess(res);
		checkSP();
	}

	/** Called by a task executor, just after execute method finished. */
	void rescheduleIfNeeded() {
		if (executeEvery > 0 && !TaskScheduler.stopping) {
			synchronized (this) {
				status = Task.STATUS_NOT_STARTED;
				executeIn(executeEvery);
			}
			TaskScheduler.schedule(this);
		} else if (nextExecution > 0 && !TaskScheduler.stopping) {
			synchronized (this) {
				this.status = Task.STATUS_NOT_STARTED;
			}
			TaskScheduler.schedule(this);
		}
	}
	
	private void checkSP() {
		if (holdSP == null) return;
		for (IAsync<?> sp : holdSP)
			if (!sp.isDone())
				sp.cancel(new CancelException("Task " + description + " done without unblocking this synchronization point"));
		holdSP = null;
	}
	
	@Override
	public String toString() {
		return "Task[" + description + "]";
	}
	
	/** Synchronization point holding the result or error of this task. */
	private final class Output extends AsyncSupplier<T, TError> {
		@Override
		public void unblockCancel(CancelException reason) {
			Task.this.cancel(reason);
		}
		
		void cancelled(CancelException reason) {
			super.unblockCancel(reason);
		}
		
		@Override
		public String toString() {
			return "Task result [" + description + "]";
		}
	}
	
	/** Create a CPU task already done. */
	public static <T, TError extends Exception> Task<T, TError> done(T result, TError error) {
		Task<T, TError> t = new Task<>(Threading.getCPUTaskManager(), "", Priority.NORMAL, null, null);
		t.setDone(result, error);
		return t;
	}
	
	/** Create a CPU task. */
	public static <T, TError extends Exception> Task<T, TError> cpu(
		String description, Priority priority, Executable<T, TError> executable, Consumer<Pair<T,TError>> ondone
	) {
		return new Task<>(Threading.getCPUTaskManager(), description, priority, executable, ondone);
	}

	/** Create a CPU task. */
	public static <T, TError extends Exception> Task<T, TError> cpu(String description, Priority priority, Executable<T, TError> executable) {
		return new Task<>(Threading.getCPUTaskManager(), description, priority, executable, null);
	}
	
	/** Create a CPU task. */
	public static <T, TError extends Exception> Task<T, TError> cpu(
		String description, Executable<T, TError> executable, Consumer<Pair<T,TError>> ondone
	) {
		return new Task<>(Threading.getCPUTaskManager(), description, null, executable, ondone);
	}

	/** Create a CPU task. */
	public static <T, TError extends Exception> Task<T, TError> cpu(String description, Executable<T, TError> executable) {
		return new Task<>(Threading.getCPUTaskManager(), description, null, executable, null);
	}
	
	/** Create a task on a file. */
	public static <T, TError extends Exception> Task<T, TError> file(
		File file, String description, Priority priority, Executable<T, TError> executable, Consumer<Pair<T,TError>> ondone
	) {
		return new Task<>(Threading.getDrivesManager().getTaskManager(file), description, priority, executable, ondone)
			.setContext(DrivesThreadingManager.TASK_CONTEXT_FILE_ATTRIBUTE, file);
	}
	
	/** Create a task on a file. */
	public static <T, TError extends Exception> Task<T, TError> file(
		File file, String description, Priority priority, Executable<T, TError> executable
	) {
		return new Task<>(Threading.getDrivesManager().getTaskManager(file), description, priority, executable, null)
			.setContext(DrivesThreadingManager.TASK_CONTEXT_FILE_ATTRIBUTE, file);
	}
	
	/** Create a task using a pool of threads. */
	public static <T, TError extends Exception> Task<T, TError> unmanaged(
		String description, Priority priority, Executable<T, TError> executable, Consumer<Pair<T,TError>> ondone
	) {
		return new Task<>(Threading.getUnmanagedTaskManager(), description, priority, executable, ondone);
	}
	
	/** Create a task using a pool of threads. */
	public static <T, TError extends Exception> Task<T, TError> unmanaged(
		String description, Executable<T, TError> executable, Consumer<Pair<T,TError>> ondone
	) {
		return new Task<>(Threading.getUnmanagedTaskManager(), description, null, executable, ondone);
	}
	
	/** Create a task using a pool of threads. */
	public static <T, TError extends Exception> Task<T, TError> unmanaged(
		String description, Priority priority, Executable<T, TError> executable
	) {
		return new Task<>(Threading.getUnmanagedTaskManager(), description, priority, executable, null);
	}
	
	/** Create a task using a pool of threads. */
	public static <T, TError extends Exception> Task<T, TError> unmanaged(
		String description, Executable<T, TError> executable
	) {
		return new Task<>(Threading.getUnmanagedTaskManager(), description, null, executable, null);
	}
	
}
