package net.lecousin.framework.concurrent.threads.pool;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.TaskExecutor;

class TaskWorker extends TaskExecutor {

	TaskWorker(Task<?,?> task, ThreadPoolTaskManager manager, String name) {
		super(manager, name);
		currentTask = task;
		thread.start();
	}
	
	boolean forceStop = false;
	
	@Override
	protected void threadLoop() {
		ThreadPoolTaskManager manager = (ThreadPoolTaskManager)this.manager;
		do {
			executeTask();
			synchronized (manager.getPriorityManager()) {
				manager.tasksDone++;
				manager.tasksTime += System.nanoTime() - currentTaskStart;
				if (forceStop)
					currentTask = null;
				else
					currentTask = manager.getPriorityManager().peekNext();
				if (currentTask == null) {
					manager.active.remove(this);
					break;
				}
			}
		} while (true);
	}
	
	@Override
	protected void unblocked(long startBlock, long startWait, long endWait, long endBlock) {
		// nothing
	}

}
