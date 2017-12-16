package net.lecousin.framework.concurrent;

import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Base class to implement a TaskManager, which is responsible to execute tasks in threads.
 */
public abstract class TaskManager {
	
	/** Constructor. */
	public TaskManager(
		String name, Object resource, ThreadFactory threadFactory, Class<? extends TaskPriorityManager> taskPriorityManager
	) {
		this.name = name;
		this.resource = resource;
		this.threadFactory = threadFactory;
		try { this.taskPriorityManager = taskPriorityManager.newInstance(); }
		catch (Throwable t) {
			throw new RuntimeException("Unable to instantiate TaskPriorityManager", t);
		}
		this.taskPriorityManager.setTaskManager(this);
	}
	
	private String name;
	private Object resource;
	protected ThreadFactory threadFactory;
	protected TaskPriorityManager taskPriorityManager;
	
	protected Object stopping = null;
	protected TaskManager transferredTo = null;
	protected boolean stopped = false;
	
	public String getName() { return name; }
	
	public void setName(String name) { this.name = name; }
	
	public Object getResource() { return resource; }
	
	/** Start the threads of this task manager. */
	abstract void start();
	
	/** Called when all task managers are started, to initialize any remaining things such as monitoring. */
	abstract void started();
	
	public TaskManager getTransferTarget() {
		return transferredTo;
	}
	
	final void shutdownWhenNoMoreTasks() {
		stopping = new Object();
		Thread t = new Thread("Stopping Task Manager: " + name) {
			@Override
			public void run() {
				Threading.logger.info("   * Stopping Task Manager: " + name);
				do {
					synchronized (taskPriorityManager) {
						if (!taskPriorityManager.hasRemainingTasks(false)) {
							// no more task => shutdown
							Threading.logger.info("   * Task Manager has no more task to do: " + name);
							TaskManager.this.finishAndStopThreads();
							taskPriorityManager.notifyAll();
							break;
						}
					}
					Threading.logger.info("   * Waiting for task manager " + name + " to finish its tasks");
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
			TaskManager.this.forceStopThreads();
			taskPriorityManager.forceStop();
		}
	}
	
	final void transferAndClose(TaskManager transferTo) {
		Threading.logger.info("Transferring TaskManager " + this.name + " to " + transferTo.name);
		// new tasks will go to the new one
		transferredTo = transferTo;
		// transfer what is ready
		List<Task<?,?>> list;
		synchronized (taskPriorityManager) {
			list = taskPriorityManager.removeAllPendingTasks();
		}
		for (Task<?,?> t : list) {
			Threading.logger.debug("  - Task ready " + t.description + " transferred to " + transferTo.name);
			transferTo.addReady(t);	
		}
		// finish pending work
		synchronized (taskPriorityManager) {
			finishAndStopThreads();
			taskPriorityManager.notifyAll();
		}
		finishTransfer();
		Threading.logger.info("End of transfer for TaskManager " + this.name + " to " + transferTo.name);
		stopped = true;
	}
	
	void cancelAndStop() {
		stopped = true;
		// stop the workers and spares
		synchronized (taskPriorityManager) {
			forceStopThreads();
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
	
	protected abstract void finishAndStopThreads();
	
	protected abstract void forceStopThreads();
	
	protected abstract void finishTransfer();
	
	abstract boolean isStopped();
	
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
				task.manager = task.manager.transferredTo;
			} while (task.manager.transferredTo != null);
			synchronized (task.manager.taskPriorityManager) {
				return task.manager.taskPriorityManager.remove(task);
			}
		}
		return false;
	}
	
	/** Describe what threads are doing for debugging purpose. */
	public abstract void debug(StringBuilder s);

	/** Print statistics to the given StringBuilder. */
	public abstract void printStats(StringBuilder s);
	
}
