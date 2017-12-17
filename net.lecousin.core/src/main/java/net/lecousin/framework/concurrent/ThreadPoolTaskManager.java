package net.lecousin.framework.concurrent;

import java.util.LinkedList;
import java.util.concurrent.ThreadFactory;

public class ThreadPoolTaskManager extends TaskManager {

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
	}
	
	@Override
	void started() {
	}
	
	@Override
	protected void finishAndStopThreads() {
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
	}
	
	@Override
	boolean isStopped() {
		boolean stopped;
		synchronized (taskPriorityManager) {
			stopped = activeThreads.isEmpty();
		}
		if (stopped) {
			StringBuilder s = new StringBuilder();
			printStats(s);
			System.out.println(s.toString());
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
	
	private class Worker implements Runnable {
		public Worker(Task<?,?> task) {
			this.task = task;
			threadFactory.newThread(this).start();
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
			 .append(activeThreads.size()).append(" active threads on ").append(maxThreads).append("\r\n");
			for (Worker w : activeThreads)
				s.append(" - ").append(w.task != null ? w.task.getDescription() : "waiting").append("\r\n");
		} catch (Throwable t) {
			s.append("\r\n...");
		}
	}

	@Override
	public void printStats(StringBuilder s) {
		s.append("Thread pool ").append(getName()).append(": ");
		s.append(tasksDone).append(" tasks done in ").append(((double)tasksTime) / 1000000000).append("s.");
	}
	
}
