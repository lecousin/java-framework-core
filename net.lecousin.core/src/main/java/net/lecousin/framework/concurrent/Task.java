package net.lecousin.framework.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/** Task to be executed asynchronously.
 * @param <T> type of result
 * @param <TError> type of error
 */
public abstract class Task<T,TError extends Exception> {

	public static final byte PRIORITY_TOP = 0;
	public static final byte PRIORITY_URGENT = 1;
	public static final byte PRIORITY_IMPORTANT = 2;
	public static final byte PRIORITY_RATHER_IMPORTANT = 3;
	public static final byte PRIORITY_NORMAL = 4;
	public static final byte PRIORITY_RATHER_LOW = 5;
	public static final byte PRIORITY_LOW = 6;
	public static final byte PRIORITY_BACKGROUND = 7;
	public static final byte NB_PRIORITES = 8;
	
	/** not yet started. */
	public static final byte STATUS_NOT_STARTED = 0;
	/** started but waiting for a specific time. */
	public static final byte STATUS_STARTED_WAITING = 1;
	/** started and put in the ready list of the task manager. */
	public static final byte STATUS_STARTED_READY = 2;
	/** started and still running. */
	public static final byte STATUS_RUNNING = 3;
	/** started but blocked (waiting for something). */
	public static final byte STATUS_BLOCKED = 4;
	/** executed, but some sub-tasks are not yet done. */
	public static final byte STATUS_EXECUTED = 5;
	/** done. */
	public static final byte STATUS_DONE = 6;
	
	// sub-classes
	
	/** Task using only CPU resource.
	 * @param <T> type of result
	 * @param <TError> type of error
	 */
	public abstract static class Cpu<T,TError extends Exception> extends Task<T,TError> {
		/** Constructor. */
		public Cpu(String description, byte priority, RunnableWithParameter<Pair<T,TError>> ondone) {
			super(Threading.getCPUTaskManager(), description, priority, ondone);
		}
		
		/** Constructor. */
		public Cpu(String description, byte priority) {
			super(Threading.getCPUTaskManager(), description, priority);
		}

		/** Task using only CPU resource and holding a parameter.
		 * @param <TParam> type of parameter
		 * @param <TResult> type of result
		 * @param <TError> type of error
		 */
		public abstract static class Parameter<TParam,TResult,TError extends Exception>
		extends Task.Parameter<TParam,TResult,TError> {
			/** Constructor. */
			public Parameter(String description, byte priority, RunnableWithParameter<Pair<TResult,TError>> ondone) {
				super(Threading.getCPUTaskManager(), description, priority, ondone);
			}

			/** Constructor. */
			public Parameter(String description, byte priority) {
				super(Threading.getCPUTaskManager(), description, priority);
			}
		}
		
		/** CPU task from a Runnable. */
		public static class FromRunnable extends Task.Cpu<Void,NoException> {
			/** Constructor. */
			public FromRunnable(
				Runnable runnable, String description, byte priority, RunnableWithParameter<Pair<Void,NoException>> ondone
			) {
				super(description, priority, ondone);
				this.runnable = runnable;
			}
			
			/** Constructor. */
			public FromRunnable(Runnable runnable, String description, byte priority) {
				super(description, priority);
				this.runnable = runnable;
			}

			private Runnable runnable;
			
			@Override
			public Void run() {
				runnable.run();
				return null;
			}
		}
	}
	
	/** Task using a only a file resource.
	 * @param <T> type of result
	 * @param <TError> type of error
	 */
	public abstract static class OnFile<T,TError extends Exception> extends Task<T,TError> {
		/** Constructor. */
		public OnFile(java.io.File file, String description, byte priority, RunnableWithParameter<Pair<T,TError>> ondone) {
			super(Threading.getDrivesTaskManager().getTaskManager(file), description, priority, ondone);
		}
		
		/** Constructor. */
		public OnFile(java.io.File file, String description, byte priority) {
			super(Threading.getDrivesTaskManager().getTaskManager(file), description, priority);
		}
		
		/** Constructor. */
		public OnFile(TaskManager manager, String description, byte priority, RunnableWithParameter<Pair<T,TError>> ondone) {
			super(manager, description, priority, ondone);
		}
		
		/** Constructor. */
		public OnFile(TaskManager manager, String description, byte priority) {
			super(manager, description, priority);
		}

		/** Task using only a file resource and holding a parameter.
		 * @param <TParam> type of parameter
		 * @param <TResult> type of result
		 * @param <TError> type of error
		 */
		public abstract static class Parameter<TParam,TResult,TError extends Exception> extends Task.Parameter<TParam,TResult,TError> {
			/** Constructor. */
			public Parameter(java.io.File file, String description, byte priority, RunnableWithParameter<Pair<TResult,TError>> ondone) {
				super(Threading.getDrivesTaskManager().getTaskManager(file), description, priority, ondone);
			}

			/** Constructor. */
			public Parameter(java.io.File file, String description, byte priority) {
				super(Threading.getDrivesTaskManager().getTaskManager(file), description, priority);
			}
		}
	}
	
	/** Task holding a parameter.
	 * @param <TParam> type of parameter
	 * @param <TResult> type of result
	 * @param <TError> type of error
	 */
	public abstract static class Parameter<TParam,TResult,TError extends Exception> extends Task<TResult,TError> {
		/** Constructor. */
		public Parameter(TaskManager tm, String description, byte priority, RunnableWithParameter<Pair<TResult,TError>> ondone) {
			super(tm, description, priority, ondone);
		}

		/** Constructor. */
		public Parameter(TaskManager tm, String description, byte priority) {
			super(tm, description, priority);
		}
		
		private TParam parameter;
		
		public TParam getParameter() { return parameter; }
		
		public void setParameter(TParam parameter) { this.parameter = parameter; }
		
		/** Set the value of the parameter and start the task. */
		public void start(TParam parameter) {
			setParameter(parameter);
			start();
		}
	}
	
	/** Task already done with a result or an error.
	 * @param <T> type of result
	 * @param <TError> type of error
	 */
	public static class Done<T,TError extends Exception> extends Task<T,TError> {
		/** Constructor. */
		public Done(T result, TError error) {
			super(Threading.getCPUTaskManager(), "", PRIORITY_NORMAL, null);
			setDone(result, error);
		}
		
		@Override
		public T run() {
			return null;
		}
		
		@Override
		public Task<T,TError> start() {
			return this;
		}
	}
	
	// constructors
	
	/** Constructor. */
	public Task(TaskManager manager, String description, byte priority) {
		this(manager, description, priority, null);
	}

	/** Constructor. */
	public Task(TaskManager manager, String description, byte priority, RunnableWithParameter<Pair<T,TError>> ondone) {
		this.app = LCCore.getApplication();
		this.manager = manager;
		this.description = description;
		this.priority = priority;
		this.ondone = ondone;
		result.listenInline(new Runnable() {
			@Override
			public void run() {
				if (app.isDebugMode() && Threading.traceTaskDone)
					Threading.logger.info("Task done: " + description);
				if (result.isCancelled() && status < STATUS_RUNNING) {
					Logger logger = app.getLoggerFactory().getLogger("Threading");
					if (logger.debug()) {
						CancelException reason = result.getCancelEvent();
						logger.debug("Task cancelled: " + description + " => "
							+ (reason != null ? reason.getMessage() : "No reason given"));
					}
					cancel(result.getCancelEvent());
				}
				status = STATUS_DONE;
			}
		});
		if (app.isDebugMode() && Threading.traceTasksNotDone)
			ThreadingDebugHelper.newTask(this);
	}
	
	/** Constructor. */
	public Task(Object resource, String description, byte priority, RunnableWithParameter<Pair<T,TError>> ondone) {
		this(Threading.get(resource), description, priority, ondone);
	}

	/** Constructor. */
	public Task(Object resource, String description, byte priority) {
		this(Threading.get(resource), description, priority, null);
	}
	
	// application
	Application app;
	
	// task manager
	TaskManager manager; 
	
	// status
	byte status;
	
	// result of execution
	SyncDone result = new SyncDone();
	RunnableWithParameter<Pair<T,TError>> ondone = null;
	
	// timing and info
	String description;
	byte priority;
	long executeEvery;
	long nextExecution;
	
	public byte getStatus() { return status; }
	
	public String getDescription() { return description; }
	
	protected void setDescription(String descr) { description = descr; }
	
	public Application getApplication() { return app; }
	
	public TaskManager getTaskManager() { return manager; }
	
	/** Method to implement to execute the task. */
	public abstract T run() throws TError, CancelException;
	
	@SuppressFBWarnings(value = "UWF_NULL_FIELD", justification = "To be set by sub-classes if needed")
	protected JoinPoint<TError> taskJoin = null;
	
	@SuppressWarnings("unchecked")
	void execute() {
		T res;
		try { res = run(); }
		catch (CancelException e) {
			status = STATUS_DONE;
			result.unblockCancel(e);
			return;
		}
		catch (Throwable t) {
			status = STATUS_DONE;
			if (!result.isCancelled()) { // if cancelled, the error is probably due to the cancellation
				if (app.isDebugMode()) app.getDefaultLogger().error("Task " + description + " error: " + t.getMessage(), t);
				try {
					TError error = (TError)t;
					if (ondone != null) ondone.run(new Pair<>(null, error));
					result.unblockError(error);
				} catch (ClassCastException e) {
					result.unblockCancel(new CancelException("Unexpected exception thrown", t));
				}
			} else
				if (app.isDebugMode())
					app.getDefaultLogger()
						.warn("Task " + description + " error after being cancelled: " + t.getMessage()
						+ ", cancellation reason is " + result.getCancelEvent().getMessage(), t);
			return;
		}
		if (taskJoin == null) {
			status = STATUS_DONE;
			if (ondone != null) ondone.run(new Pair<>(res, null));
			result.unblockSuccess(res);
			return;
		}
		status = STATUS_EXECUTED;
		taskJoin.listenInline(new Runnable() {
			@Override
			public void run() {
				status = STATUS_DONE;
				if (taskJoin.isCancelled())
					result.unblockCancel(taskJoin.getCancelEvent());
				else if (taskJoin.hasError()) {
					if (ondone != null) ondone.run(new Pair<>(null, taskJoin.getError()));
					result.unblockError(taskJoin.getError());
				} else {
					if (ondone != null) ondone.run(new Pair<>(res, null));
					result.unblockSuccess(res);
				}
				taskJoin = null;
			}
		});
		if (app.isDebugMode()) {
			taskJoin.listenTime(30000, new Runnable() {
				@Override
				public void run() {
					app.getDefaultLogger().warn(
						"Task " + description + " is done, but still waiting for other works to be done after 30s.");
				}
			});
		}
	}
	
	/** Ask to start the task. This does not start it immediately, but adds it to the queue of tasks to execute. */
	public Task<T,TError> start() {
		synchronized (this) {
			if (result.isCancelled()) return this;
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
	
	void sendToTaskManager() {
		while (manager.getTransferTarget() != null)
			manager = manager.getTransferTarget();
		status = Task.STATUS_STARTED_READY;
		manager.addReady(this);
	}
	
	// public functions
	/*
	public static Task<?,?> getCaller() {
		Thread t = Thread.currentThread();
		if (t instanceof TaskWorker)
			return ((TaskWorker)t).currentTask;
		return null;
	}*/
	/** Cancel this task. */
	public void cancel(CancelException reason) {
		if (reason == null) reason = new CancelException("No reason given");
		if (!result.isCancelled())
			result.cancel(reason);
		if (!TaskScheduler.cancel(this))
			manager.remove(this);
		status = Task.STATUS_DONE;
	}
	
	/** Cancel this task only if not yet started. */
	public boolean cancelIfExecutionNotStarted(CancelException reason) {
		synchronized (this) {
			if (status < STATUS_RUNNING) {
				cancel(reason);
				return true;
			}
		}
		return false;
	}
	
	public boolean isDone() {
		return status == STATUS_DONE && result.isUnblocked();
	}
	
	public boolean isSuccessful() {
		return status == STATUS_DONE && result.isSuccessful();
	}
	
	public boolean isCancelled() {
		return result.isCancelled();
	}
	
	public boolean isStarted() {
		return status > STATUS_NOT_STARTED;
	}
	
	public boolean isRunning() {
		return status >= STATUS_RUNNING && status < STATUS_DONE;
	}
	
	public T getResult() { return result.getResult(); }
	
	public TError getError() { return result.getError(); }
	
	public CancelException getCancelEvent() { return result.getCancelEvent(); }
	
	public boolean hasError() { return result.hasError(); }
	
	@SuppressFBWarnings("UG_SYNC_SET_UNSYNC_GET")
	public byte getPriority() { return priority; }
	
	/** Change the priority of this task. */
	public synchronized void setPriority(byte priority) {
		if (this.priority == priority) return;
		if (status == STATUS_STARTED_READY) {
			if (manager.remove(this)) {
				this.priority = priority;
				while (manager.getTransferTarget() != null)
					manager = manager.getTransferTarget();
				manager.addReady(this);
				return;
			}
		}
		this.priority = priority;
	}
	
	public SyncDone getSynch() { return result; }
	
	/** Execute this task at the given time. */
	public void executeAt(long time) {
		nextExecution = time;
	}
	
	/** Execute this task in the given delay in milliseconds. */
	public void executeIn(long delay) {
		executeAt(System.currentTimeMillis() + delay);
	}
	
	/** Execute this task repetively. */
	public void executeEvery(long delay, long initialDelay) {
		executeEvery = delay;
		executeIn(initialDelay);
	}
	
	/** Do not execute this task again. */
	public void stopRepeat() {
		executeEvery = 0;
	}
	
	/** Execute again this task in the given delay. */
	public void executeAgainIn(long delay) {
		executeAgainAt(System.currentTimeMillis() + delay);
	}

	/** Execute again this task at the given time. */
	public void executeAgainAt(long time) {
		nextExecution = time;
	}
	
	/** Change the next execution time of scheduled or repetitive task. */
	public synchronized void changeNextExecutionTime(long time) {
		if (status == STATUS_STARTED_WAITING)
			TaskScheduler.changeNextExecutionTime(this, time);
		else
			nextExecution = time;
	}
	
	/** Change the next execution time to now. */
	public synchronized void executeNextOccurenceNow() {
		long now = System.currentTimeMillis();
		if (nextExecution > now)
			changeNextExecutionTime(System.currentTimeMillis());
	}
	
	/** Change the next execution time to now with the given priority. */
	public synchronized void executeNextOccurenceNow(byte priority) {
		setPriority(priority);
		executeNextOccurenceNow();
	}
	
	/** Start this task once the given synchronization point is unblocked. */
	public void startOn(ISynchronizationPoint<? extends Exception> sp, boolean evenOnErrorOrCancel) {
		if (app.isDebugMode() && Threading.traceTasksNotDone)
			ThreadingDebugHelper.waitingFor(this, sp);
		sp.listenInline(new Runnable() {
			@Override
			public void run() {
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
				} else
					start();
			}
		});
	}
	
	/** Start this task once all the given synchronization points are unblocked. */
	public void startOn(boolean evenOnErrorOrCancel, ISynchronizationPoint<?>... list) {
		if (app.isDebugMode() && Threading.traceTasksNotDone)
			ThreadingDebugHelper.waitingFor(this, list);
		JoinPoint<Exception> jp = new JoinPoint<>();
		for (ISynchronizationPoint<? extends Exception> sp : list)
			if (sp != null)
				jp.addToJoin(sp);
		jp.start();
		jp.listenInline(new Runnable() {
			@Override
			public void run() {
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
				} else
					start();
			}
		});
	}
	
	/** Set this task as done with the given result or error. */
	public void setDone(T result, TError error) {
		this.status = STATUS_DONE;
		if (error == null)
			this.result.unblockSuccess(result);
		else
			this.result.unblockError(error);
	}
	
	/** Start the given task once this task is done. */
	public void ondone(Task<?,?> todo, boolean evenIfErrorOrCancel) {
		if (app.isDebugMode() && Threading.traceTasksNotDone)
			ThreadingDebugHelper.waitingFor(todo, this);
		if (result.isCancelled())
			todo.cancel(result.getCancelEvent());
		else if (result.hasError()) {
			todo.cancel(new CancelException(result.getError()));
		} else if (result.isUnblocked())
			todo.start();
		else
			todo.startOn(result, evenIfErrorOrCancel);
	}
	
	@Override
	public String toString() {
		return super.toString() + "[" + description + "]";
	}
	
	/** Synchronization point holding the result or error of this task. */
	public class SyncDone extends AsyncWork<T, TError> {
		private SyncDone() {}
		
		public Task<T,TError> getTask() {
			return Task.this;
		}
		
		@Override
		public String toString() {
			return "Task synchronization point [" + description + "]";
		}
	}
	
}
