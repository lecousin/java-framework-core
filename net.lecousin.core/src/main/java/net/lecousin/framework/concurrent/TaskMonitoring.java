package net.lecousin.framework.concurrent;

import java.io.Closeable;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Iterator;
import java.util.LinkedList;
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
	
	static synchronized void start(ThreadFactory threadFactory) {
		if (monitor != null) return;
		monitor = new TaskMonitor();
		Thread t = threadFactory.newThread(monitor);
		t.setName("Task Monitoring");
		t.start();
		LCCore.get().toClose(monitor);
	}
	
	private static void append(StringBuilder s, MonitorInfo[] monitors) {
		if (monitors.length > 0) {
			s.append("\r\nLocked monitors:");
			for (int i = 0; i < monitors.length; ++i) {
				StackTraceElement trace = monitors[i].getLockedStackFrame();
				s.append("\r\n - ").append(trace.getClassName()).append('#')
					.append(trace.getMethodName()).append(':').append(trace.getLineNumber());
			}
		}
	}
	
	private static void appendLocks(StringBuilder s, Thread t) {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		if (bean == null) return;
		ThreadInfo info = bean.getThreadInfo(new long[] { t.getId() }, true, false)[0];
		if (info == null) return;
		MonitorInfo[] monitors = info.getLockedMonitors();
		append(s, monitors);
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
				LinkedList<TaskWorker> blocked = new LinkedList<>();
				for (TaskManager manager : Threading.getAllTaskManagers())
					if (manager instanceof FixedThreadTaskManager) {
						FixedThreadTaskManager m = (FixedThreadTaskManager)manager;
						check(m);
						blocked.addAll(m.getBlockedWorkers());
					} else {
						// TODO
					}
				checkBlockedWorkers(blocked);
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
	
	private static void check(FixedThreadTaskManager manager) {
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
			if (worker.aside) return;
			StringBuilder s = new StringBuilder(1024);
			s.append("Task ").append(task).append(" is running since ").append(seconds)
			 .append(" seconds ! put it aside and start a new thread, current stack:\r\n");
			DebugUtil.createStackTrace(s, worker.thread.getStackTrace());
			appendLocks(s, worker.thread);
			Threading.logger.warn(s.toString());
			worker.manager.putWorkerAside(worker);
			return;
		}
		StringBuilder s = new StringBuilder(1024);
		s.append("Task ").append(task).append(" is running since ").append(seconds)
		 .append(" seconds ! kill it! current stack:\r\n");
		DebugUtil.createStackTrace(s, worker.thread.getStackTrace());
		appendLocks(s, worker.thread);
		Threading.logger.error(s.toString());
		worker.manager.killWorker(worker);
	}
	
	private static void checkBlockedWorkers(LinkedList<TaskWorker> workers) {
		if (!checkLocksOfBlockingTasks) return;
		if (workers.isEmpty()) return;
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		if (bean == null) return;
		long[] ids = new long[workers.size()];
		int i = 0;
		for (TaskWorker w : workers) ids[i++] = w.thread.getId();
		ThreadInfo[] info = bean.getThreadInfo(ids, true, false);
		Iterator<TaskWorker> it = workers.iterator();
		for (ThreadInfo ti : info) {
			TaskWorker w = it.next();
			Task<?,?> task = w.currentTask;
			if (task == null) continue;
			if (!w.blocked) continue;
			MonitorInfo[] monitors = ti.getLockedMonitors();
			if (monitors.length == 0) continue;
			StringBuilder s = new StringBuilder(1024);
			s.append("TaskWorker is blocked while locking objects in task ").append(task.description).append(":\r\n");
			DebugUtil.createStackTrace(s, ti.getStackTrace());
			append(s, monitors);
			if (!w.blocked) continue;
			Threading.logger.error(s.toString());
		}
	}
	
}
