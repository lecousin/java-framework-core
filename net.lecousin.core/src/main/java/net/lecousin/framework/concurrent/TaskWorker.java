package net.lecousin.framework.concurrent;

import java.util.ArrayList;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.DebugUtil;

@SuppressWarnings("squid:S106") // print to console
class TaskWorker implements Runnable, BlockedThreadHandler {

	TaskWorker(String name, FixedThreadTaskManager manager) {
		this.manager = manager;
		this.thread = manager.newThread(this);
		this.thread.setName(name);
		Threading.registerBlockedThreadHandler(this, this.thread);
	}

	boolean stop = false;
	boolean finish = false;
	FixedThreadTaskManager manager;
	//boolean working = false;
	Task<?,?> currentTask = null;
	long currentTaskStart = -1;
	long tasksDone = 0;
	long workingTime = 0;
	long waitingTime = 0;
	long blockedTime = 0;
	long lastUsed = -1;
	Thread thread;
	boolean aside = false;
	boolean blocked = false;
	
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
	public void run() {
		// TODO ClassLoader initCL = thread.getContextClassLoader();
		while (!stop) {
			// check if we are supposed to pause
			AsyncWork<TaskWorker,NoException> waitPause = manager.getPauseToDo();
			if (waitPause != null) {
				//working = false;
				synchronized (this) {
					waitPause.unblockSuccess(this);
					try { this.wait(); }
					catch (InterruptedException e) { break; }
					continue;
				}
			}
			// take something to do
			//working = false;
			currentTaskStart = System.nanoTime();
			currentTask = manager.peekNextOrWait();
			if (currentTask == null) {
				waitingTime += System.nanoTime() - currentTaskStart;
				if (finish)
					stop = true;
				continue;
			}
			//working = true;
			synchronized (currentTask) {
				currentTask.status = Task.STATUS_RUNNING;
				currentTask.nextExecution = 0;
			}
			// TODO thread.setContextClassLoader(currentTask.getApplication().getClassLoader());
			long start = System.nanoTime();
			currentTask.execute();
			if (Threading.traceTaskTime)
				Threading.logger.debug("Task done in " + (System.nanoTime() - start) + "ns: " + currentTask.description);
			lastUsed = System.currentTimeMillis();
			// TODO thread.setContextClassLoader(initCL);
			Task<?,?> t = currentTask;
			currentTask = null;
			tasksDone++;
			t.rescheduleIfNeeded();
			workingTime += System.nanoTime() - start;
			if (aside) {
				manager.asideWorkerDone(this);
				break;
			}
		}
		Threading.unregisterBlockedThreadHandler(this.thread);
		StringBuilder s = new StringBuilder();
		printStats(s);
		System.out.print(s.toString());
	}
	
	@Override
	@SuppressWarnings("squid:S2274") // wait without loop
	public void blocked(ISynchronizationPoint<?> blockPoint, long blockTimeout) {
		long start = System.nanoTime();
		manager.imBlocked(this);
		long start2 = System.nanoTime();
		synchronized (blockPoint) {
			if (blockTimeout <= 0) {
				while (!blockPoint.isUnblocked())
					try { blockPoint.wait(0); }
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
			} else if (!blockPoint.isUnblocked()) {
				try { blockPoint.wait(blockTimeout); }
				catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			}
		}
		blockedTime += System.nanoTime() - start2;
		manager.imUnblocked(this, start2);
		long end = System.nanoTime();
		workingTime -= end - start;
		waitingTime += end - start;
		if (end - start > 100000000 && Threading.logger.debug()) {
			StackTraceElement[] stack = thread.getStackTrace();
			ArrayList<String> blocking = new ArrayList<>(stack.length - 2);
			for (int i = 2; i < stack.length; ++i) {
				StackTraceElement e = stack[i];
				String c = e.getClassName();
				/*
				if ("java.lang.Thread".equals(c)) continue;
				if (c.equals(getClass().getName())) continue;
				try {
					if (IBlockingPoint.class.isAssignableFrom(Class.forName(c))) continue;
				} catch (Throwable t) { continue; }*/
				blocking.add(e.getFileName() + ":" + c + "." + e.getMethodName() + ":" + e.getLineNumber());
				//if (blocking.size() == 5)
				//	break;
			}
			StringBuilder s = new StringBuilder();
			s.append("Task ").append(currentTask.description).append(" has been blocked for ")
				.append(((end - start) / 1000000)).append("ms. consider to split it into several tasks: ");
			for (String b : blocking) s.append("\r\n - ").append(b);
			Threading.logger.debug(s.toString());
		}
	}
	
	public void debug(StringBuilder s, String type) {
		s.append(" - ").append(type).append(' ').append(thread.getName()).append(": ");
		Task<?,?> c = currentTask;
		if (c == null)
			s.append("waiting");
		else
			s.append("executing ").append(c.description).append(" (").append(c.getClass().getName()).append(")");
		s.append("\r\n");
		if (c != null) {
			DebugUtil.createStackTrace(s,thread.getStackTrace());
			s.append("\r\n");
		}
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
