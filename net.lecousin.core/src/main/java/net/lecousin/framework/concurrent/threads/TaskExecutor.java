package net.lecousin.framework.concurrent.threads;

import java.util.ArrayList;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.util.DebugUtil;
import net.lecousin.framework.util.ThreadUtil;

/** Thread executing tasks. */
public abstract class TaskExecutor {
	
	protected TaskManager manager;
	protected Thread thread;
	protected Task<?, ?> currentTask;
	protected long currentTaskStart = -1;
	boolean blocked = false;
	boolean aside = false;
	
	protected TaskExecutor(TaskManager manager, String name) {
		this.manager = manager;
		thread = manager.threadFactory.newThread(() -> {
			try {
				threadLoop();
			} finally {
				Threading.unregisterTaskExecutor(this.thread);
				manager.executorEnd(TaskExecutor.this);
			}
		});
		thread.setName(name);
		thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler());
		Threading.registerTaskExecutor(this, thread);
	}

	/** Signal that the current thread is blocked by the given synchronization point. */
	public final void blocked(IAsync<?> synchPoint, long timeout) {
		long start = System.nanoTime();
		manager.imBlocked(this);
		long startWait = System.nanoTime();
		synchronized (synchPoint) {
			if (timeout <= 0) {
				while (!synchPoint.isDone())
					if (!ThreadUtil.wait(synchPoint, 0)) break;
			} else if (!synchPoint.isDone()) {
				ThreadUtil.wait(synchPoint, timeout);
			}
		}
		long endWait = System.nanoTime();
		manager.imUnblocked(this, startWait);
		long end = System.nanoTime();
		unblocked(start, startWait, endWait, end);
		if (end - start > currentTask.getMaxBlockingTimeInNanoBeforeToLog() && Threading.getLogger().debug()) {
			StackTraceElement[] stack = thread.getStackTrace();
			ArrayList<String> blocking = new ArrayList<>(stack.length - 2);
			for (int i = 2; i < stack.length; ++i) {
				StackTraceElement e = stack[i];
				String c = e.getClassName();
				blocking.add(e.getFileName() + ":" + c + "." + e.getMethodName() + ":" + e.getLineNumber());
			}
			StringBuilder s = new StringBuilder();
			s.append("Task ").append(currentTask.getDescription()).append(" has been blocked for ")
				.append(((end - start) / 1000000)).append("ms. consider to split it into several tasks: ");
			for (String b : blocking) s.append("\n - ").append(b);
			Threading.getLogger().debug(s.toString());
		}
	}
	
	/** Return the task currently executed, or null if not executing a task. */
	public final Task<?, ?> getCurrentTask() {
		return currentTask;
	}
	
	public boolean isAside() {
		return aside;
	}
	
	protected abstract void threadLoop();
	
	protected abstract void unblocked(long startBlock, long startWait, long endWait, long endBlock);
	
	protected void executeTask() {
		currentTaskStart = System.nanoTime();
		synchronized (currentTask) {
			currentTask.status = Task.STATUS_RUNNING;
			currentTask.nextExecution = 0;
		}
		currentTask.execute();
		if (Threading.traceTaskTime)
			Threading.getLogger().debug("Task done in " + (System.nanoTime() - currentTaskStart) + "ns: " + currentTask.getDescription());
		Task<?,?> t = currentTask;
		currentTask = null;
		t.rescheduleIfNeeded();
	}
	
	private class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			if (currentTask != null && !currentTask.isDone())
				currentTask.cancelledBecauseExecutorDied(new CancelException("Unexpected error in thread " + t.getName(), e));
			manager.executorUncaughtException(TaskExecutor.this);
			LCCore.getApplication().getDefaultLogger().error("Error in TaskWorker " + t.getName(), e);
		}
	}

	/** Describe what this executor is doing for debugging purpose. */
	public void debug(StringBuilder s, String type) {
		s.append("\n - ").append(type).append(' ').append(thread.getName()).append(": ");
		Task<?,?> c = currentTask;
		if (c == null)
			s.append("waiting");
		else
			s.append("executing ").append(c.getDescription()).append(" (").append(c.getClass().getName()).append(")");
		if (c != null) {
			DebugUtil.createStackTrace(s,thread.getStackTrace());
		}
	}
	
}
