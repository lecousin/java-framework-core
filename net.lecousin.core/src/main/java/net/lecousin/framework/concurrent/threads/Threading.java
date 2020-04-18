package net.lecousin.framework.concurrent.threads;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.Blockable;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.DrivesThreadingManager.DrivesProvider;
import net.lecousin.framework.concurrent.threads.fixed.MultiThreadTaskManager;
import net.lecousin.framework.concurrent.threads.pool.ThreadPoolTaskManager;
import net.lecousin.framework.concurrent.threads.priority.SimpleTaskPriorityManager;
import net.lecousin.framework.concurrent.threads.priority.TaskPriorityManager;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.util.AsyncCloseable;
import net.lecousin.framework.util.DebugUtil;
import net.lecousin.framework.util.ThreadUtil;

/**
 * Utility class to initialize and stop multi-threading, and utiliy methods for multi-threading.
 */
@SuppressWarnings({
	"squid:ClassVariableVisibilityCheck",
	"squid:S1444" // not final
})
public final class Threading {
	
	private Threading() {
		/* no instance */
	}
	
	private static Logger logger;
	
	public static Logger getLogger() {
		return logger;
	}
	
	public static boolean traceBlockingTasks = System.getProperty("lc.traceBlockingTasks") != null;
	public static boolean traceTaskTime = System.getProperty("lc.traceTaskTime") != null;
	public static long debugListenersTakingMoreThanMilliseconds = 20;
	
	private static long logThreadingInterval = 30000;
	private static ThreadingLogger loggerThread;
	
	private static ArrayList<Thread> systemThreadsOnStart = new ArrayList<>();
	
	public static void setLogThreadingInterval(long interval) {
		logThreadingInterval = interval;
	}

	/**
	 * Initialize multi-threading.
	 * This method is called by the {@link net.lecousin.framework.application.LCCore.Environment} instance on initialization.
	 * @param threadFactory factory to use when creating threads
	 * @param nbCPUThreads number of threads to use for CPU tasks,
	 *     0 or negative value means the number of available processors returned by {@link Runtime#availableProcessors()}
	 * @param cpuTaskPriorityManagerSupplier the class to use to manage priority of tasks
	 * @param cpuMonitoring monitoring configuration for CPU threads
	 * @param drivesProvider provides physical drives and associated mount points.
	 *     If null, {@link File#listRoots()} is used and each returned root is considered as a physical drive.
	 *     This may be changed later on by calling {@link DrivesThreadingManager#setDrivesProvider(DrivesProvider)}.
	 * @param drivesTaskPriorityManagerSupplier priority manager for drives task
	 * @param driveMonitoring monitoring configuration for drives threads
	 * @param nbUnmanagedThreads number of threads to use for unmanaged tasks.
	 *     If 0 or negative, maximum 100 threads will be used.
	 * @param unmanagedTaskPriorityManagerSupplier priority manager to use for unmanaged tasks
	 * @param unmanagedMonitoring monitoring configuration for unmanaged threads
	 */
	@SuppressWarnings("java:S107") // number of parameters
	public static void init(
		ThreadFactory threadFactory,
		int nbCPUThreads,
		Supplier<TaskPriorityManager> cpuTaskPriorityManagerSupplier,
		TaskManagerMonitor.Configuration cpuMonitoring,
		DrivesProvider drivesProvider,
		Supplier<TaskPriorityManager> drivesTaskPriorityManagerSupplier,
		TaskManagerMonitor.Configuration driveMonitoring,
		int nbUnmanagedThreads,
		Supplier<TaskPriorityManager> unmanagedTaskPriorityManagerSupplier,
		TaskManagerMonitor.Configuration unmanagedMonitoring
	) {
		if (isInitialized()) throw new IllegalStateException("Threading has been already initialized.");
		
		systemThreadsOnStart.addAll(Thread.getAllStackTraces().keySet());
		systemThreadsOnStart.trimToSize();
		
		logger = LCCore.get().getThreadingLogger();
		TaskScheduler.init();
		TaskPriorityManager prioCpu;
		TaskPriorityManager prioUnmanaged;
		try {
			prioCpu = cpuTaskPriorityManagerSupplier.get();
			prioUnmanaged = unmanagedTaskPriorityManagerSupplier.get();
		} catch (Exception e) {
			Threading.getLogger().error("Unable to instantiate task priority manager", e);
			prioCpu = new SimpleTaskPriorityManager();
			prioUnmanaged = new SimpleTaskPriorityManager();
		}
		cpuManager = new MultiThreadTaskManager(
			"CPU",
			Threading.CPU,
			nbCPUThreads > 0 ? nbCPUThreads : Runtime.getRuntime().availableProcessors(),
			threadFactory,
			prioCpu,
			cpuMonitoring
		);
		cpuManager.start();
		resources.put(CPU, cpuManager);
		drivesManager = new DrivesThreadingManager(threadFactory, drivesTaskPriorityManagerSupplier, drivesProvider, driveMonitoring);
		unmanagedManager = new ThreadPoolTaskManager(
			"Unmanaged tasks manager", UNMANAGED, nbUnmanagedThreads, threadFactory, prioUnmanaged, unmanagedMonitoring);
		resources.put(UNMANAGED, unmanagedManager);
		LCCore.get().toClose(new StopMultiThreading());
		synchronized (resources) {
			for (TaskManager tm : resources.values())
				tm.started();
		}
		
		loggerThread = new ThreadingLogger();
		loggerThread.start();
	}
	
	public static boolean isInitialized() {
		return cpuManager != null;
	}
	
	private static class ThreadingLogger extends Thread implements Closeable {
		ThreadingLogger() {
			super("Threading logger");
		}
		
		private boolean closed = false;
		private final Object lock = new Object();
		
		@Override
		public void run() {
			do {
				synchronized (lock) {
					if (!ThreadUtil.wait(lock, logThreadingInterval))
						return;
				}
				if (closed) return;
				logger.debug("\n" + Threading.debug());
			} while (true);
		}
		
		@Override
		public void close() {
			synchronized (lock) {
				closed = true;
				lock.notify();
			}
		}
	}

	
	@SuppressWarnings("squid:S106") // print to console
	private static class StopMultiThreading implements AsyncCloseable<Exception> {
		@Override
		@SuppressWarnings({"squid:S2142", "squid:S3776"})
		public IAsync<Exception> closeAsync() {
			loggerThread.close();
			Async<Exception> sp = new Async<>();
			Thread t = new Thread("Stopping tasks managers") {
				@Override
				public void run() {
					boolean hasTasks = false;
					long start = System.currentTimeMillis();
					do {
						hasTasks = false;
						synchronized (resources) {
							for (TaskManager tm : resources.values()) {
								int nb = tm.getRemainingTasks(false);
								if (nb > 0) {
									System.out.println("   * Still " + nb + " tasks to do for " + tm.getName());
									hasTasks = true;
								}
							}
						}
						if (!hasTasks) break;
						if (System.currentTimeMillis() - start > 5000) break;
						try { Thread.sleep(25); }
						catch (InterruptedException e) { break; }
					} while (true);
					if (!hasTasks) System.out.println("   * No more task to do in any task manager, continue stop process");
					else System.out.println("   * Still some tasks after 5 seconds, continue stop process anyway");
					// stop scheduling new tasks
					TaskScheduler.end();
					// stopping task managers
					synchronized (resources) {
						for (TaskManager tm : resources.values())
							tm.shutdownWhenNoMoreTasks();
					}
					boolean stop = true;
					start = System.currentTimeMillis();
					do {
						synchronized (resources) {
							for (TaskManager tm : resources.values())
								stop &= tm.allActiveExecutorsStopped();
						}
						if (stop) break;
						try { Thread.sleep(10); }
						catch (InterruptedException e) { break; }
						stop = true;
						if (System.currentTimeMillis() - start > 10000) {
							start = Long.MAX_VALUE;
							TaskScheduler.end();
							synchronized (resources) {
								for (TaskManager tm : resources.values())
									if (!tm.allActiveExecutorsStopped()) {
										System.err.println("Force to stop " + tm.getName());
										tm.forceStop();
									}
							}
						}
					} while (true);
					System.out.println("   * All Task Managers are stopped");
					sp.unblock();
				}
			};
			t.start();
			return sp;
		}
	}
	
	public static final Object CPU = new Object();
	public static final Object UNMANAGED = new Object();
	
	private static TaskManager cpuManager;
	private static DrivesThreadingManager drivesManager;
	private static ThreadPoolTaskManager unmanagedManager;
	
	public static TaskManager getCPUTaskManager() { return cpuManager; }
	
	public static DrivesThreadingManager getDrivesManager() { return drivesManager; }

	public static ThreadPoolTaskManager getUnmanagedTaskManager() { return unmanagedManager; }
	
	private static Map<Object,TaskManager> resources = new HashMap<>();
	
	/** Register a resource. */
	public static void registerResource(Object resource, TaskManager tm) {
		if (resource == null) return;
		synchronized (resources) {
			resources.put(resource, tm);
		}
	}
	
	/** Unregister a resource. */
	public static TaskManager unregisterResource(Object resource) {
		if (resource == null) return null;
		synchronized (resources) {
			return resources.remove(resource);
		}
	}
	
	/** Get the task manager for the given resource. */
	public static TaskManager get(Object resource) {
		return resources.get(resource);
	}
	
	/** Return all current TaskManager. */
	public static List<TaskManager> getAllTaskManagers() {
		synchronized (resources) {
			return new ArrayList<>(resources.values());
		}
	}
	
	private static Map<Thread, TaskExecutor> executors = new HashMap<>();
	private static Map<Thread, Blockable> blockables = new HashMap<>();
	private static Map<Thread, ApplicationThread> appThreads = new HashMap<>();
	
	/** Register the executor for the given thread. */
	public static void registerBlockable(Blockable handler, Thread thread) {
		synchronized (blockables) {
			blockables.put(thread, handler);
		}
	}

	/** Unregister the executor for the given thread. */
	public static void unregisterBlockable(Thread thread) {
		synchronized (blockables) {
			blockables.remove(thread);
		}
	}
	
	/** Return the blockable for the given thread. */
	public static Blockable getBlockable(Thread thread) {
		Blockable b = executors.get(thread);
		if (b != null) return b;
		return blockables.get(thread);
	}
	
	/** Return the blockable for the current thread. */
	public static Blockable getBlockable() {
		return getBlockable(Thread.currentThread());
	}
	
	/** Register the executor for the given thread. */
	public static void registerTaskExecutor(TaskExecutor handler, Thread thread) {
		synchronized (executors) {
			executors.put(thread, handler);
		}
	}

	/** Unregister the executor for the given thread. */
	public static void unregisterTaskExecutor(Thread thread) {
		synchronized (executors) {
			executors.remove(thread);
		}
	}
	
	/** Return the executor for the given thread. */
	public static TaskExecutor getTaskExecutor(Thread thread) {
		return executors.get(thread);
	}
	
	/** Return the executor for the current thread. */
	public static TaskExecutor getTaskExecutor() {
		return executors.get(Thread.currentThread());
	}
	
	/** Return the current task for the current thread. */
	public static Task<?, ?> currentTask() {
		TaskExecutor executor = executors.get(Thread.currentThread());
		return executor != null ? executor.getCurrentTask() : null;
	}
	
	public static void registerApplicationThread(Thread thread, ApplicationThread app) {
		synchronized (appThreads) {
			if (LCCore.getApplication() != app.getApplication() ||
				appThreads.containsKey(thread) ||
				executors.containsKey(thread))
				throw new IllegalStateException();
			appThreads.put(thread, app);
		}
	}
	
	public static void unregisterApplicationThread(Thread thread) {
		synchronized (appThreads) {
			ApplicationThread a = appThreads.get(thread);
			if (a == null)
				return;
			if (LCCore.getApplication() != a.getApplication())
				throw new IllegalStateException();
			appThreads.remove(thread);
		}
	}
	
	public static void unregisterApplicationThreads(Application app) {
		synchronized (appThreads) {
			List<Thread> threads = new LinkedList<>();
			for (Map.Entry<Thread, ApplicationThread> e : appThreads.entrySet())
				if (e.getValue().getApplication() == app)
					threads.add(e.getKey());
			for (Thread t : threads)
				appThreads.remove(t);
		}
	}
	
	/** Set the monitoring configuration for CPU tasks. */
	public static void setCpuMonitorConfiguration(TaskManagerMonitor.Configuration config) {
		if (!LCCore.get().currentThreadIsSystem()) throw new IllegalThreadStateException();
		cpuManager.getMonitor().setConfiguration(config);
	}
	
	/** Set the monitoring configuration for drives tasks. */
	public static void setDrivesMonitorConfiguration(TaskManagerMonitor.Configuration config) {
		if (!LCCore.get().currentThreadIsSystem()) throw new IllegalThreadStateException();
		drivesManager.setMonitoringConfiguration(config);
	}
	
	/** Set the monitoring configuration for unmanaged tasks. */
	public static void setUnmanagedMonitorConfiguration(TaskManagerMonitor.Configuration config) {
		if (!LCCore.get().currentThreadIsSystem()) throw new IllegalThreadStateException();
		unmanagedManager.getMonitor().setConfiguration(config);
	}
	
	/** Return a string containing multi-threading status for debugging purposes. */
	public static String debug() {
		StringBuilder s = new StringBuilder();
		for (TaskManager tm : resources.values()) {
			tm.debug(s);
			s.append("\r\n");
		}
		Set<Application> apps = new HashSet<>();
		for (ApplicationThread at : appThreads.values())
			apps.add(at.getApplication());
		for (Application app : apps) {
			s.append(" --- Threads status for application ").append(app.getFullName()).append(" ---\n");
			for (ApplicationThread at : appThreads.values())
				if (at.getApplication() == app)
					at.debugStatus(s);
		}
		List<Thread> legalThreads = new LinkedList<>();
		legalThreads.addAll(systemThreadsOnStart);
		legalThreads.add(loggerThread); // this is us
		legalThreads.add(TaskScheduler.get());
		for (Map.Entry<Thread, TaskExecutor> e : executors.entrySet())
			legalThreads.add(e.getKey());
		for (TaskManager manager : resources.values())
			legalThreads.add(manager.getMonitor().getThread());
		legalThreads.addAll(appThreads.keySet());
		Map<Thread,StackTraceElement[]> threads = Thread.getAllStackTraces();
		boolean first = true;
		for (Map.Entry<Thread,StackTraceElement[]> e : threads.entrySet()) {
			Thread t = e.getKey();
			if (legalThreads.contains(t))
				continue;
			if (first) {
				s.append(" --- Threads started without being attached to an application ---\n");
				first = false;
			}
			s.append(" - ").append(t.getName()).append(" [").append(t.getThreadGroup().getName()).append(']');
			DebugUtil.createStackTrace(s, e.getValue());
			s.append('\n');
		}
		return s.toString();
	}
	
	/** Signal a call to a listener and log it if it took a too long time. */
	public static void debugListenerCall(Object listener, long nanoseconds) {
		if (nanoseconds > debugListenersTakingMoreThanMilliseconds * 1000000)
			logger.debug("Listener took " + (nanoseconds / 1000000.0d) + "ms: " + listener);
	}
	
}
