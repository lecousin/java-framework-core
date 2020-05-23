package net.lecousin.framework.concurrent.threads;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.threads.priority.TaskPriorityManager;
import net.lecousin.framework.util.DebugUtil;

/**
 * A TaskManager is responsible to execute tasks in threads for a given resource, based on a TaskPriorityManager.
 */
public abstract class TaskManager {
	
	/** Constructor. */
	public TaskManager(
		String name, Object resource, ThreadFactory threadFactory, TaskPriorityManager taskPriorityManager,
		TaskManagerMonitor.Configuration monitorConfig
	) {
		this.name = name;
		this.resource = resource;
		this.threadFactory = threadFactory;
		this.taskPriorityManager = taskPriorityManager;
		this.monitor = new TaskManagerMonitor(this, monitorConfig);
	}
	
	private String name;
	private Object resource;
	protected ThreadFactory threadFactory;
	protected TaskPriorityManager taskPriorityManager;
	protected TaskManagerMonitor monitor;

	private LinkedList<TaskExecutor> aside = new LinkedList<>();
	private TurnArray<TaskExecutor> blocked = new TurnArray<>(20);

	protected Object stopping = null;
	protected TaskManager transferredTo = null;
	protected boolean stopped = false;
	
	public final String getName() { return name; }
	
	public final void setName(String name) { this.name = name; }
	
	public final Object getResource() { return resource; }
	
	public TaskManagerMonitor getMonitor() {
		return monitor;
	}
	
	/** Start the threads of this task manager. */
	final void start() {
		startThreads();
	}
	
	/** Start the threads of this task manager. */
	protected abstract void startThreads();
	
	/** Called when all task managers are started, to initialize any remaining things such as monitoring. */
	final void started() {
		threadingStarted();
	}

	/** Called when all task managers are started, to initialize any remaining things such as monitoring. */
	protected abstract void threadingStarted();
	
	public final TaskManager getTransferTarget() {
		return transferredTo;
	}
	
	@SuppressWarnings({"squid:S106","squid:S2142"}) // print to console + InterruptedException
	final void shutdownWhenNoMoreTasks() {
		stopping = new Object();
		taskPriorityManager.stopping();
		Thread t = new Thread("Stopping Task Manager: " + name) {
			@Override
			public void run() {
				System.out.println("   * Stopping Task Manager: " + name);
				do {
					synchronized (taskPriorityManager) {
						if (!taskPriorityManager.hasRemainingTasks(false)) {
							// no more task => shutdown
							System.out.println("   * Task Manager has no more task to do: " + name);
							TaskManager.this.finishAndStopActiveAndInactiveExecutors();
							taskPriorityManager.notifyAll();
							break;
						}
					}
					System.out.println("   * Waiting for task manager " + name + " to finish its tasks");
					synchronized (stopping) {
						try { stopping.wait(1000); }
						catch (InterruptedException e) { break; }
					}
				} while (true);
			}
		};
		t.start();
	}
	
	final void forceStop() {
		synchronized (taskPriorityManager) {
			TaskManager.this.forceStopActiveAndInactiveExecutors();
			taskPriorityManager.forceStop();
		}
	}
	
	final void transferAndClose(TaskManager transferTo) {
		Threading.getLogger().info("Transferring TaskManager " + this.name + " to " + transferTo.name);
		// new tasks will go to the new one
		transferredTo = transferTo;
		taskPriorityManager.stopping();
		// transfer what is ready
		List<Task<?,?>> list;
		synchronized (taskPriorityManager) {
			list = taskPriorityManager.removeAllPendingTasks();
		}
		for (Task<?,?> t : list) {
			Threading.getLogger().debug("  - Task ready " + t.getDescription() + " transferred to " + transferTo.name);
			transferTo.addReady(t);	
		}
		// finish pending work
		synchronized (taskPriorityManager) {
			finishAndStopActiveAndInactiveExecutors();
			taskPriorityManager.notifyAll();
		}

		// TODO transfer blocked
		synchronized (blocked) {
			for (TaskExecutor executor : blocked)
				Threading.getLogger().error("  - Remaining blocked thread: " + executor.thread.getName());
		}
		
		Threading.getLogger().info("End of transfer for TaskManager " + this.name + " to " + transferTo.name);
		stopped = true;
	}
	
	final void cancelAndStop() {
		stopped = true;
		// stop the workers and spares
		synchronized (taskPriorityManager) {
			forceStopActiveAndInactiveExecutors();
			taskPriorityManager.notifyAll();
		}
		// cancel what is ready
		List<Task<?,?>> list;
		synchronized (taskPriorityManager) {
			list = taskPriorityManager.removeAllPendingTasks();
		}
		for (Task<?,?> t : list)
			t.cancel(new CancelException("Stop Task Manager", null));
	}
	
	final int getRemainingTasks(boolean includingBackground) {
		return taskPriorityManager.getRemainingTasks(includingBackground);
	}
	
	protected abstract void finishAndStopActiveAndInactiveExecutors();
	
	protected abstract void forceStopActiveAndInactiveExecutors();
	
	/** Return true if all active threads are stopped. */
	public abstract boolean allActiveExecutorsStopped();
	
	final boolean isStopping() {
		return stopping != null;
	}
	
	final void addReady(Task<?,?> t) {
		if (stopped)
			t.cancel(new CancelException("Task Manager already stopped", null));
		add(t);
	}
	
	protected void add(Task<?,?> t) {
		taskPriorityManager.add(t);
	}
	
	boolean remove(Task<?,?> task) {
		synchronized (taskPriorityManager) {
			if (taskPriorityManager.remove(task))
				return true;
		}
		if (transferredTo != null) {
			do {
				task.transferTo(task.getTaskManager().transferredTo);
			} while (task.getTaskManager().transferredTo != null);
			synchronized (task.getTaskManager().taskPriorityManager) {
				return task.getTaskManager().taskPriorityManager.remove(task);
			}
		}
		return false;
	}
	
	void imBlocked(TaskExecutor executor) {
		executor.blocked = true;
		if (Threading.traceBlockingTasks) {
			Threading.getLogger().error("Task " + executor.getCurrentTask().getDescription() + " blocked", new Exception("blocked here"));
		}
		if (transferredTo != null) {
			// we are in the process of being transferred, we cannot launch a spare
			Threading.getLogger().info("Task blocked while transferring to a new TaskManager: "
				+ executor.getCurrentTask().getDescription());
			synchronized (blocked) {
				blocked.addLast(executor);
			}
			return;
		}
		
		// a thread is blocked, we need to launch a spare
		replaceBlockedExecutor(executor);
		
		synchronized (blocked) {
			blocked.addLast(executor);
		}
	}
	
	protected abstract void replaceBlockedExecutor(TaskExecutor executor);
	
	void imUnblocked(TaskExecutor executor, long since) {
		executor.blocked = false;
		
		if (Threading.traceBlockingTasks) {
			Threading.getLogger().error("Task " + executor.getCurrentTask().getDescription() + " unblocked after "
				+ ((System.nanoTime() - since) / 1000000) + "ms.");
		}

		unblockedExecutor(executor);
		
		synchronized (blocked) {
			blocked.removeInstance(executor);
		}
	}
	
	protected abstract void unblockedExecutor(TaskExecutor executor);

	
	void executorEnd(TaskExecutor executor) {
		synchronized (aside) {
			aside.remove(executor);
		}
	}
	
	void putExecutorAside(TaskExecutor executor) {
		if (executor.aside) return;
		executor.aside = true;
		synchronized (aside) {
			aside.add(executor);
		}
		executorAside(executor);
	}
	
	@SuppressWarnings({"deprecation", "squid:CallToDeprecatedMethod", "java:S1181"})
	void killExecutor(TaskExecutor executor) {
		synchronized (aside) {
			if (!aside.remove(executor)) return;
		}
		StackTraceElement[] stack = executor.thread.getStackTrace();
		StringBuilder s = new StringBuilder(1024);
		s.append("Task stopped at \r\n");
		DebugUtil.createStackTrace(s, stack);
		Threading.getLogger().error(s.toString());
		Task<?, ?> task = executor.getCurrentTask();
		if (task != null)
			task.cancel(new CancelException("Task was running since a too long time"));
		try {
			executor.thread.stop();
		} catch (Throwable e) {
			// ignore
		}		
	}
	
	protected abstract void executorUncaughtException(TaskExecutor executor);
	
	protected abstract void executorAside(TaskExecutor executor);
	
	/** Return the list of executors in a blocked state. */
	public final List<TaskExecutor> getBlockedExecutors() {
		synchronized (blocked) {
			return new ArrayList<>(blocked);
		}
	}
	
	/** Return the list of executors currently active, including blocked ones and aside ones. */
	public final List<TaskExecutor> getAllActiveExecutors() {
		List<TaskExecutor> actives = getActiveExecutors();
		ArrayList<TaskExecutor> list = new ArrayList<>(actives.size() + blocked.size() + aside.size());
		list.addAll(actives);
		synchronized (blocked) {
			list.addAll(blocked);
		}
		synchronized (aside) {
			list.addAll(aside);
		}
		return list;
	}

	/** Return the list of executors currently active, not blocked and not aside. */
	public abstract List<TaskExecutor> getActiveExecutors();

	/** Return the list of executors currently inactive, typically spare threads. */
	public abstract List<TaskExecutor> getInactiveExecutors();
	
	/** Describe what threads are doing for debugging purpose. */
	public final void debug(StringBuilder s) {
		getDebugDescription(s);
		for (TaskExecutor w : getActiveExecutors())
			try { w.debug(s, "Active"); }
			catch (Exception t) { /* ignore, because we don't want to do it in a synchronized block, so NPE can happen */ }
		List<TaskExecutor> inactives = getInactiveExecutors();
		if (!inactives.isEmpty())
			s.append("\n - ").append(inactives.size()).append(" thread(s) inactive");
		for (TaskExecutor w : blocked)
			try { w.debug(s, "Blocked"); }
			catch (Exception t) { /* ignore, because we don't want to do it in a synchronized block, so NPE can happen */ }
		for (TaskExecutor w : aside)
			try { w.debug(s, "Aside"); }
			catch (Exception t) { /* ignore, because we don't want to do it in a synchronized block, so NPE can happen */ }
	}
	
	protected abstract void getDebugDescription(StringBuilder s);

}
