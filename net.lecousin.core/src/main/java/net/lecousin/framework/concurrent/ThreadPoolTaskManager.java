package net.lecousin.framework.concurrent;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ThreadFactory;

import net.lecousin.framework.exception.NoException;

public class ThreadPoolTaskManager extends TaskManager {

	public ThreadPoolTaskManager(
		String name, Object resource, int maxThreads, ThreadFactory threadFactory, Class<? extends TaskPriorityManager> taskPriorityManager
	) {
		super(name, resource, threadFactory, taskPriorityManager);
		this.maxThreads = maxThreads;
	}
	
	private int maxThreads;
	private LinkedList<Worker> activeThreads = new LinkedList<>();
	private CloseUnusedThreads closeThreads = new CloseUnusedThreads();
	
	@Override
	void start() {
	}
	
	@Override
	void started() {
		closeThreads.start();
	}
	
	@Override
	protected void finishAndStopThreads() {
		closeThreads.run();
	}
	
	@Override
	protected void forceStopThreads() {
		synchronized (taskPriorityManager) {
			for (Worker w : activeThreads) {
				w.forceStop = true;
				w.finishing = true;
			}
			taskPriorityManager.notifyAll();
		}
	}
	
	@Override
	protected void finishTransfer() {
	}
	
	@Override
	boolean isStopped() {
		synchronized (taskPriorityManager) {
			return activeThreads.isEmpty();
		}
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
			threadFactory.newThread(this);
		}
		
		private Task<?,?> task;
		private boolean finishing = false;
		private boolean forceStop = false;
		
		@Override
		public void run() {
			do {
				if (task != null) {
					task.execute();
					task.rescheduleIfNeeded();
					task = null;
				}
				synchronized (taskPriorityManager) {
					synchronized (this) {
						if (finishing) break;
					}
					task = taskPriorityManager.peekNextOrWait();
					synchronized (this) {
						if (task == null && finishing)
							break;
					}
				}
			} while (!forceStop);
			synchronized (taskPriorityManager) {
				activeThreads.remove(this);
			}
		}
	}
	
	private class CloseUnusedThreads extends Task.Cpu<Void, NoException> {
		public CloseUnusedThreads() {
			super("Close unused threads in pool", Task.PRIORITY_LOW);
			executeEvery(60000, 2 * 60000);
		}
		
		@Override
		public Void run() {
			synchronized (taskPriorityManager) {
				if (taskPriorityManager.hasRemainingTasks(true))
					return null;
				boolean hasFinishing = false;
				for (Iterator<Worker> it = activeThreads.iterator(); it.hasNext(); ) {
					Worker w = it.next();
					if (w.task != null) continue;
					synchronized (w) {
						if (w.task == null) {
							w.finishing = true;
							hasFinishing = true;
							it.remove();
						}
					}
				}
				if (hasFinishing)
					taskPriorityManager.notifyAll();
			}
			return null;
		}
	}

	@Override
	public void debug(StringBuilder s) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void printStats(StringBuilder s) {
		// TODO Auto-generated method stub
		
	}
	
}
