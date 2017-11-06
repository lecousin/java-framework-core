package net.lecousin.framework.concurrent;

import java.io.Closeable;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ThreadFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.util.DebugUtil;

/** Monitoring of multi-threading system to avoid dead tasks. */
@SuppressFBWarnings({"MS_CANNOT_BE_FINAL", "MS_SHOULD_BE_FINAL" })
public final class TaskMonitoring {

	public static boolean checkLocksOfBlockingTasks = false;
	
	public static int MONITORING_INTERVAL = 60 * 1000;
	public static int SECONDS_BEFORE_TO_PUT_TASK_ASIDE = 5 * 60;
	public static int SECONDS_BEFORE_KILL_TASK = 10 * 60;
	
	private static TaskMonitor monitor;
	
	static void start(ThreadFactory threadFactory) {
		monitor = new TaskMonitor();
		threadFactory.newThread(monitor);
		LCCore.get().toClose(monitor);
	}
	
	/** Called when a TaskWorker is blocked, if checkLocksOfBlockingTasks is true. */
	static void checkNoLockForWorker() {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		if (bean == null) return;
		Thread t = Thread.currentThread();
		ThreadInfo info = bean.getThreadInfo(t.getId());
		if (info == null) return;
		MonitorInfo[] monitors = info.getLockedMonitors();
		LockInfo[] locks = info.getLockedSynchronizers();
		if (monitors.length == 0 && locks.length == 0) return;
		StringBuilder s = new StringBuilder(4096);
		s.append("TaskWorker is blocked while locking objects:\r\n");
		DebugUtil.createStackTrace(s, new Exception("Here"), false);
		if (monitors.length > 0) {
			s.append("\r\nLocked monitors:");
			for (int i = 0; i < monitors.length; ++i) {
				StackTraceElement trace = monitors[i].getLockedStackFrame();
				s.append("\r\n - ").append(trace.getClassName()).append('#')
					.append(trace.getMethodName()).append(':').append(trace.getLineNumber());
			}
		}
		if (locks.length > 0) {
			s.append("\r\nLocked synchronizers:");
			for (int i = 0; i < locks.length; ++i) {
				s.append("\r\n - ").append(locks[i].getClassName());
			}
		}
		Threading.logger.error(s.toString());
	}

	private static class TaskMonitor implements Runnable, Closeable {
		
		private Object lock = new Object();
		private boolean closed = false;
		
		@Override
		public void run() {
			while (!closed) {
				synchronized (lock) {
					try { lock.wait(MONITORING_INTERVAL); }
					catch (InterruptedException e) { break; }
					if (closed)
						break;
				}
				for (TaskManager manager : Threading.getAllTaskManagers())
					check(manager);
			}
		}
		
		@Override
		public void close() {
			closed = true;
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}
	
	private static void check(TaskManager manager) {
		for (TaskWorker worker : manager.getAllActiveWorkers())
			check(worker);
	}
	
	private static void check(TaskWorker worker) {
		if (worker == null) return;
		long now = System.nanoTime();
		Task<?,?> task = worker.currentTask;
		if (task == null) return;
		long start = worker.currentTaskStart;
		long seconds = (now - start) / (1000000L * 1000);
		if (seconds < SECONDS_BEFORE_TO_PUT_TASK_ASIDE) return;
		if (seconds < SECONDS_BEFORE_KILL_TASK) {
			Threading.logger.warn("Task " + task + " is running since " + seconds + " seconds ! put it aside and start a new thread");
			worker.manager.putWorkerAside(worker);
			return;
		}
		Threading.logger.error("Task " + task + " is running since " + seconds + " seconds ! kill it.");
		worker.manager.killWorker(worker);
	}
	
}
