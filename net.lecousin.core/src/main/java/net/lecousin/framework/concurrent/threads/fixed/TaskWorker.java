package net.lecousin.framework.concurrent.threads.fixed;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.threads.TaskExecutor;
import net.lecousin.framework.exception.NoException;

@SuppressWarnings("squid:S106") // print to console
class TaskWorker extends TaskExecutor {

	TaskWorker(String name, FixedThreadTaskManager manager) {
		super(manager, name);
	}

	boolean stop = false;
	boolean finish = false;
	long tasksDone = 0;
	long workingTime = 0;
	long waitingTime = 0;
	long blockedTime = 0;
	long lastUsed = -1;

	Thread getThread() {
		return thread;
	}
	
	void forceStop(boolean normal) {
		if (!normal) {
			StringBuilder s = new StringBuilder(200);
			s.append("Task worker forced to stop: ");
			debug(s, "");
			System.err.print(s.toString());
		}
		stop = true;
		synchronized (this) {
			this.notifyAll();
		}
	}
	
	void finishAndStop() {
		finish = true;
		synchronized (this) {
			this.notifyAll();
		}
	}
	
	@Override
	@SuppressWarnings("squid:S2142") // InterruptedException
	protected void threadLoop() {
		FixedThreadTaskManager manager = (FixedThreadTaskManager)this.manager;
		long waitStart = System.nanoTime();
		while (!stop) {
			// check if we are supposed to pause
			AsyncSupplier<TaskWorker,NoException> waitPause = manager.getPauseToDo();
			if (waitPause != null) {
				synchronized (this) {
					waitPause.unblockSuccess(this);
					try { this.wait(); }
					catch (InterruptedException e) { break; }
					continue;
				}
			}
			long now = System.nanoTime();
			waitingTime += now - waitStart;
			waitStart = now;
			// take something to do
			currentTask = manager.peekNextOrWait();
			if (currentTask == null) {
				if (finish)
					stop = true;
				continue;
			}
			executeTask();
			lastUsed = System.currentTimeMillis();
			tasksDone++;
			now = System.nanoTime();
			waitStart = now;
			workingTime += now - currentTaskStart;
			if (isAside())
				break;
		}
		StringBuilder s = new StringBuilder();
		printStats(s);
		System.out.print(s.toString());
	}
	
	@Override
	protected void unblocked(long startBlock, long startWait, long endWait, long endBlock) {
		blockedTime += endWait - startWait;
		workingTime -= endBlock - startBlock;
		waitingTime += endBlock - startBlock;
	}
	
	public void printStats(StringBuilder s) {
		 s.append(thread.getName());
		while (s.length() < 30) s.append(' ');
		s.append(": ");
		s.append(tasksDone);
		while (s.length() < 45) s.append(' ');
		s.append(" tasks done in ");
		s.append(((double)workingTime) / 1000000000);
		while (s.length() < 80) s.append(' ');
		s.append(" waited ");
		s.append(((double)waitingTime) / 1000000000);
		s.append(" blocked ");
		s.append(((double)blockedTime) / 1000000000);
		s.append("\r\n");
	}
}
