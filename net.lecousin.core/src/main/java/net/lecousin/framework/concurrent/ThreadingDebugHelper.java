package net.lecousin.framework.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;

/**
 * Utilities to help debugging multi-threading applications.
 */
public final class ThreadingDebugHelper {
	
	private ThreadingDebugHelper() { /* no instance */ }

	/** Register a JoinPoint, to monitor that it will be unblocked. */
	public static synchronized void register(JoinPoint<?> jp) {
		JP j = new JP();
		j.jp = jp;
		j.creationTrace = Thread.currentThread().getStackTrace();
		jps.add(j);
	}
	
	/** Indicates that a JoinPoint is waiting for a synchronization point. */
	public static synchronized void registerJoin(JoinPoint<?> jp, ISynchronizationPoint<?> sp) {
		for (JP j : jps)
			if (j.jp == jp) {
				j.toJoinTraces.add(Thread.currentThread().getStackTrace());
				j.toJoinSP.add(sp);
				sp.listenInline(new Runnable() {
					@Override
					public void run() {
						int i = j.toJoinSP.indexOf(sp);
						j.toJoinSP.remove(i);
						j.toJoinTraces.remove(i);
					}
				});
				break;
			}
	}
	
	/** Indicate a JoinPoint has been started. */
	public static synchronized void started(JoinPoint<?> jp) {
		Task.Cpu<Void,NoException> task = new Task.Cpu<Void,NoException>("Check JoinPoint", Task.PRIORITY_LOW) {
			@Override
			public Void run() {
				if (jp.isUnblocked()) return null;
				synchronized (ThreadingDebugHelper.class) {
					for (int i = 0; i < jps.size(); ++i)
						if (jps.get(i).jp == jp) {
							JP j = jps.get(i);
							StringBuilder s = new StringBuilder();
							s.append("JoinPoint still blocked after 30s.:\r\n");
							s.append(" + Creation =\r\n");
							for (StackTraceElement e : j.creationTrace)
								s.append("    - ").append(e.getClassName()).append('#').append(e.getMethodName())
									.append(":").append(e.getLineNumber()).append("\r\n");
							s.append(" + Remaining joins:\r\n");
							for (int k = 0; k < j.toJoinSP.size(); ++k) {
								ISynchronizationPoint<?> sp = j.toJoinSP.get(k);
								if (sp.isUnblocked()) continue;
								s.append("    + ");
								if (sp instanceof Task.SyncDone)
									s.append("Task [").append(((Task<?,?>.SyncDone)sp).getTask().getDescription())
										.append("], ");
								s.append("creation at\r\n");
								for (StackTraceElement e : j.toJoinTraces.get(k))
									s.append("        - ").append(e.getClassName()).append('#')
										.append(e.getMethodName()).append(":").append(e.getLineNumber())
										.append("\r\n");
							}
							Threading.logger.error(s.toString());
							jps.remove(i);
							break;
						}
					return null;
				}
			}
		};
		task.executeIn(30000);
		task.start();
	}
	
	/** Indicate the given JoinPoint has been unblocked. */
	public static synchronized void unblocked(SynchronizationPoint<?> sp) {
		if (sp instanceof JoinPoint) {
			for (int i = 0; i < jps.size(); ++i)
				if (jps.get(i).jp == sp) {
					jps.remove(i);
					break;
				}
		}
	}
	
	private static ArrayList<JP> jps = new ArrayList<>();
	
	private static class JP {
		private JoinPoint<?> jp;
		private StackTraceElement[] creationTrace;
		private ArrayList<StackTraceElement[]> toJoinTraces = new ArrayList<>();
		private ArrayList<ISynchronizationPoint<?>> toJoinSP = new ArrayList<>();
	}
	
	private static class MonitoredTask {
		MonitoredTask(Task<?,?> task) {
			this.task = task;
		}
		
		Task<?,?> task;
		long creation = System.currentTimeMillis();
		LinkedList<ISynchronizationPoint<?>> waitingFor = new LinkedList<>();
	}
	
	private static LinkedList<MonitoredTask> tasks = new LinkedList<>();
	
	static void newTask(Task<?,?> task) {
		synchronized (tasks) { tasks.add(new MonitoredTask(task)); }
		task.getSynch().listenInline(new Runnable() {
			@Override
			public void run() {
				synchronized (tasks) {
					for (Iterator<MonitoredTask> it = tasks.iterator(); it.hasNext(); )
						if (it.next().task == task) {
							it.remove();
							break;
						}
				}
			}
		});
	}
	
	static void waitingFor(Task<?,?> task, ISynchronizationPoint<?> sp) {
		synchronized (tasks) {
			for (MonitoredTask t : tasks) {
				if (t.task != task) continue;
				t.waitingFor.add(sp);
				break;
			}
		}
	}
	
	/** Indicate a task is waiting for a synchronization point to start. */
	static void waitingFor(Task<?,?> task, ISynchronizationPoint<?>[] sp) {
		synchronized (tasks) {
			for (MonitoredTask t : tasks) {
				if (t.task != task) continue;
				for (int i = 0; i < sp.length; ++i)
					t.waitingFor.add(sp[i]);
				break;
			}
		}
	}

	/** Indicate a task is waiting for another one to start. */
	static void waitingFor(Task<?,?> task, Task<?,?> waitingFor) {
		waitingFor(task, waitingFor.getSynch());
	}
	
	
	static void traceTasksNotDone() {
		Thread t = new Thread("Track tasks not done") {
			@Override
			public void run() {
				do {
					try { Thread.sleep(30000); }
					catch (InterruptedException e) { break; }
					synchronized (tasks) {
						long now = System.currentTimeMillis();
						for (Iterator<MonitoredTask> it = tasks.iterator(); it.hasNext(); ) {
							MonitoredTask t = it.next();
							if (now - t.creation < 30000) continue;
							if (t.task.executeEvery > 0) {
								it.remove();
								continue;
							}
							if (t.task.nextExecution > now) continue;
							StringBuilder s = new StringBuilder();
							s.append("Task not yet done: ").append(t.task.description).append("\r\n");
							s.append("  -> status = ").append(t.task.status).append("\r\n");
							for (ISynchronizationPoint<?> sp : t.waitingFor) {
								s.append(" -> Waiting for ");
								s.append(sp.toString());
								if (sp.isUnblocked())
									s.append(" [unblocked]");
								if (sp.hasError())
									s.append(" [has error]");
								if (sp.isCancelled())
									s.append(" [cancelled]");
								s.append("\r\n");
							}
							Threading.logger.debug(s.toString());
						}
					}
				} while (true);
			}
		};
		t.start();
	}
	
}
