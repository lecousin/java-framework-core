package net.lecousin.framework.concurrent.threads;

import java.io.Closeable;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Iterator;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.util.DebugUtil;

/** Monitoring of multi-threading system to avoid dead tasks. */
public final class TaskManagerMonitor {
	
	/** Configuration. */
	@SuppressWarnings({
		"squid:ClassVariableVisibilityCheck",
		"squid:S1444" // field not final
	})
	public static class Configuration {
		public int taskExecutionMillisecondsBeforeToWarn;
		public int taskExecutionMillisecondsBeforeToPutAside;
		public int taskExecutionMillisecondsBeforeToKill;
		public boolean checkLocksOnBlockedTasks;
		
		/** Constructor. */
		public Configuration(
			int taskExecutionMillisecondsBeforeToWarn,
			int taskExecutionMillisecondsBeforeToPutAside,
			int taskExecutionMillisecondsBeforeToKill,
			boolean checkLocksOnBlockedTasks
		) {
			this.taskExecutionMillisecondsBeforeToWarn = taskExecutionMillisecondsBeforeToWarn;
			this.taskExecutionMillisecondsBeforeToPutAside = taskExecutionMillisecondsBeforeToPutAside;
			this.taskExecutionMillisecondsBeforeToKill = taskExecutionMillisecondsBeforeToKill;
			this.checkLocksOnBlockedTasks = checkLocksOnBlockedTasks;
		}
	}
	
	TaskManagerMonitor(TaskManager manager, Configuration config) {
		this.manager = manager;
		this.config = config;
		thread = new Monitor();
		Thread t = manager.threadFactory.newThread(thread);
		t.setName(manager.getName() + " - Task Monitoring");
		t.start();
		LCCore.get().toClose(thread);
	}
	
	private TaskManager manager;
	private Configuration config;
	private Monitor thread;
	
	public Configuration getConfiguration() {
		return config;
	}
	
	/** Check tasks now. */
	public void checkNow() {
		synchronized (thread.lock) {
			thread.lock.notify();
		}
	}
	
	private class Monitor implements Runnable, Closeable {
		
		private Object lock = new Object();
		private boolean closed = false;
		
		@Override
		@SuppressWarnings("squid:S2142") // InterruptedException
		public void run() {
			long wait = config.taskExecutionMillisecondsBeforeToWarn;
			while (!closed) {
				synchronized (lock) {
					if (wait > 0)
						try { lock.wait(wait); }
						catch (InterruptedException e) { break; }
					if (closed || (manager.isStopping() && manager.allActiveExecutorsStopped()))
						break;
				}
				wait = check();
				if (wait == -1)
					wait = config.taskExecutionMillisecondsBeforeToWarn;
			}
		}
		
		@Override
		public void close() {
			closed = true;
			synchronized (lock) {
				lock.notifyAll();
			}
		}

		private long check() {
			long nextTime = -1;
			for (TaskExecutor executor : manager.getAllActiveExecutors()) {
				long next = check(executor);
				if (next != -1 && (nextTime == -1 || nextTime > next))
					nextTime = next;
			}
			if (config.checkLocksOnBlockedTasks)
				checkLocks(manager.getBlockedExecutors());
			return nextTime;
		}
		
		private long check(TaskExecutor executor) {
			if (executor == null) return -1;
			long now = System.nanoTime();
			Task<?,?> task = executor.currentTask;
			if (task == null) return -1;
			long start = executor.currentTaskStart;
			long ms = (now - start) / 1000000L;
			if (ms < config.taskExecutionMillisecondsBeforeToWarn) return config.taskExecutionMillisecondsBeforeToWarn - ms;
			if (ms < config.taskExecutionMillisecondsBeforeToPutAside) {
				StringBuilder s = new StringBuilder(256);
				startMessage(s, executor, ms);
				Threading.getLogger().warn(s.toString());
				return config.taskExecutionMillisecondsBeforeToPutAside - ms;
			}
			if (ms < config.taskExecutionMillisecondsBeforeToKill) {
				if (executor.aside) return -1;
				StringBuilder s = new StringBuilder(2048);
				startMessage(s, executor, ms);
				s.append(" ! put the thread aside and start a new thread, current stack:");
				DebugUtil.createStackTrace(s, executor.thread.getStackTrace());
				appendLocks(s, executor.thread);
				Threading.getLogger().warn(s.toString());
				executor.manager.putExecutorAside(executor);
				return config.taskExecutionMillisecondsBeforeToKill - ms;
			}
			StringBuilder s = new StringBuilder(2048);
			startMessage(s, executor, ms);
			s.append(" ! kill the thread! current stack:");
			DebugUtil.createStackTrace(s, executor.thread.getStackTrace());
			appendLocks(s, executor.thread);
			Threading.getLogger().error(s.toString());
			executor.manager.killExecutor(executor);
			return 1;
		}
		
		private void startMessage(StringBuilder s, TaskExecutor executor, long ms) {
			s.append("Task ").append(executor.currentTask).append(" is running since ")
			.append(ms).append(" milliseconds on thread ").append(executor.thread);
		}
		
		private void checkLocks(List<TaskExecutor> executors) {
			if (executors.isEmpty()) return;
			ThreadMXBean bean = ManagementFactory.getThreadMXBean();
			if (bean == null) return;
			long[] ids = new long[executors.size()];
			int i = 0;
			for (TaskExecutor executor : executors) ids[i++] = executor.thread.getId();
			ThreadInfo[] info = bean.getThreadInfo(ids, true, false);
			Iterator<TaskExecutor> it = executors.iterator();
			for (ThreadInfo ti : info) {
				TaskExecutor executor = it.next();
				Task<?,?> task = executor.currentTask;
				if (task == null) continue;
				if (!executor.blocked) continue;
				MonitorInfo[] monitors = ti.getLockedMonitors();
				if (monitors.length == 0) continue;
				StringBuilder s = new StringBuilder(1024);
				s.append("TaskWorker is blocked while locking objects in task ").append(task.getDescription()).append(":\r\n");
				DebugUtil.createStackTrace(s, ti.getStackTrace());
				if (append(s, monitors) == 0) continue;
				if (!executor.blocked) continue;
				Threading.getLogger().error(s.toString());
			}
		}
		
		private void appendLocks(StringBuilder s, Thread t) {
			ThreadMXBean bean = ManagementFactory.getThreadMXBean();
			if (bean == null) return;
			ThreadInfo info = bean.getThreadInfo(new long[] { t.getId() }, true, false)[0];
			if (info == null) return;
			MonitorInfo[] monitors = info.getLockedMonitors();
			append(s, monitors);
		}
		
		private int append(StringBuilder s, MonitorInfo[] monitors) {
			int nb = 0;
			s.append("\r\nLocked monitors:");
			for (int i = 0; i < monitors.length; ++i) {
				String className = monitors[i].getClassName();
				if (className.startsWith("net.lecousin.framework.concurrent.threads.")) {
					if (className.startsWith("net.lecousin.framework.concurrent.threads.priority")) continue;
					if ("net.lecousin.framework.concurrent.threads.Task".equals(className)) continue;
				}
				StackTraceElement trace = monitors[i].getLockedStackFrame();
				s.append("\r\n - ").append(className).append(" at ").append(trace.getClassName()).append('.')
					.append(trace.getMethodName()).append('(')
					.append(trace.getFileName()).append(':').append(trace.getLineNumber()).append(')');
				nb++;
			}
			return nb;
		}
		
	}
	
}
