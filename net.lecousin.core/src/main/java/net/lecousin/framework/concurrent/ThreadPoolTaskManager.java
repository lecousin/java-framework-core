package net.lecousin.framework.concurrent;

import java.util.LinkedList;
import java.util.concurrent.ThreadFactory;

import net.lecousin.framework.application.LCCore;

/**
 * A Task Manager that use a pool of threads to execute tasks.
 * This is used for unmanaged tasks, that may use several physical resources and block at any time,
 * typically when using functionalities from a library that is not designed to be used with LCCore.
 * Each time a new task comes, if the number of active threads is less than the maximum number of threads,
 * a new thread is launched to execute the task, else it is queued.
 * When a thread finishes a task, it checkes if tasks are present in the queue. If yes, it peeks one and
 * execute it. Else the thread ends.
 */
public class ThreadPoolTaskManager extends TaskManager {

	/** Constructor. */
	public ThreadPoolTaskManager(
		String name, Object resource, int maxThreads, ThreadFactory threadFactory, Class<? extends TaskPriorityManager> taskPriorityManager
	) {
		super(name, resource, threadFactory, taskPriorityManager);
		if (maxThreads > 0)
			this.maxThreads = maxThreads;
		else
			this.maxThreads = 100;
	}
	
	private int maxThreads;
	private LinkedList<Worker> activeThreads = new LinkedList<>();
	private long tasksDone = 0;
	private long tasksTime = 0;
	
	@Override
	void start() {
		// nothing to do
	}
	
	@Override
	void started() {
		// nothing to do
	}
	
	@Override
	@SuppressWarnings("squid:S106") // print to console
	protected void finishAndStopThreads() {
		StringBuilder s = new StringBuilder();
		printStats(s);
		System.out.println(s.toString());
	}
	
	@Override
	protected void forceStopThreads() {
		synchronized (taskPriorityManager) {
			for (Worker w : activeThreads)
				w.forceStop = true;
			taskPriorityManager.notifyAll();
		}
	}
	
	@Override
	protected void finishTransfer() {
		// nothing to do
	}
	
	@Override
	boolean isStopped() {
		boolean stopped;
		synchronized (taskPriorityManager) {
			stopped = activeThreads.isEmpty();
		}
		return stopped;
	}
	
	@Override
	protected void add(Task<?, ?> t) {
		synchronized (taskPriorityManager) {
			if (activeThreads.size() < maxThreads)
				activeThreads.add(new Worker(t));
			else
				taskPriorityManager.add(t);
		}
	}
	
	private class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
		public UncaughtExceptionHandler(Worker worker) {
			this.worker = worker;
		}
		
		private Worker worker;
		
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			if (worker.task != null && !worker.task.isDone()) {
				CancelException reason = new CancelException("Unexpected error in thread " + t.getName(), e);
				worker.task.cancelling = reason;
				worker.task.result.cancelled(reason);
			}
			synchronized (taskPriorityManager) {
				activeThreads.remove(worker);
				Task<?,?> task = taskPriorityManager.peekNext();
				if (task != null)
					activeThreads.add(new Worker(task));
			}
			LCCore.getApplication().getDefaultLogger().error("Error in Worker " + t.getName(), e);
		}
	}
	
	private class Worker implements Runnable {
		public Worker(Task<?,?> task) {
			this.task = task;
			Thread t = threadFactory.newThread(this);
			t.setUncaughtExceptionHandler(new UncaughtExceptionHandler(this));
			t.start();
		}
		
		private Task<?,?> task;
		private boolean forceStop = false;
		
		@Override
		public void run() {
			do {
				long start = System.nanoTime();
				task.execute();
				task.rescheduleIfNeeded();
				synchronized (taskPriorityManager) {
					tasksDone++;
					tasksTime += System.nanoTime() - start;
					if (forceStop)
						task = null;
					else
						task = taskPriorityManager.peekNext();
					if (task == null) {
						activeThreads.remove(this);
						break;
					}
				}
			} while (true);
		}
	}
	
	@Override
	public void debug(StringBuilder s) {
		try {
			s.append("Thread pool ").append(getName()).append(": ")
			 .append(activeThreads.size()).append('/').append(maxThreads).append(" active threads\r\n");
			for (Worker w : activeThreads)
				s.append(" - ").append(w.task != null ? w.task.getDescription() : "waiting").append("\r\n");
		} catch (Exception t) {
			s.append("\r\n...");
		}
	}

	@Override
	public void printStats(StringBuilder s) {
		s.append("Thread pool ").append(getName()).append(": ");
		s.append(tasksDone).append(" tasks done in ").append(((double)tasksTime) / 1000000000).append("s.");
	}
	
}
