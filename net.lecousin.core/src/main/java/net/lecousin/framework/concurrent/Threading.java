package net.lecousin.framework.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.DrivesTaskManager.DrivesProvider;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.util.AsyncCloseable;

/**
 * Utility class to initialize and stop multi-threading, and utiliy methods for multi-threading.
 */
public final class Threading {
	
	private Threading() { /* no instance */ }
	
	static Logger logger;
	
	public static boolean traceBlockingTasks = System.getProperty("lc.traceBlockingTasks") != null;
	public static boolean traceTaskTime = System.getProperty("lc.traceTaskTime") != null;
	public static boolean debugSynchronization = System.getProperty("lc.debugSynchronization") != null;
	public static boolean traceTaskDone = System.getProperty("lc.traceTaskDone") != null;
	public static boolean traceTasksNotDone = System.getProperty("lc.traceTasksNotDone") != null;
	
	/**
	 * Initialize multi-threading.
	 * This method is called by the {@link net.lecousin.framework.application.LCCore.Environment} instance on initialization.
	 * @param threadFactory factory to use when creating threads
	 * @param taskPriorityManager the class to use to manage priority of tasks
	 * @param nbCPUThreads number of threads to use for CPU tasks,
	 *     0 or negative value means the number of available processors returned by {@link Runtime#availableProcessors()}
	 * @param drivesProvider provides physical drives and associated mount points.
	 *     If null, {@link File#listRoots()} is used and each returned root is considered as a physical drive.
	 *     This may be changed later on by calling {@link DrivesTaskManager#setDrivesProvider(DrivesProvider)}.
	 * @param nbUnmanagedThreads number of threads to use for unmanaged tasks.
	 *     If 0 or negative, maximum 100 threads will be used.
	 */
	public static void init(
		ThreadFactory threadFactory,
		Class<? extends TaskPriorityManager> taskPriorityManager,
		int nbCPUThreads,
		DrivesProvider drivesProvider,
		int nbUnmanagedThreads
	) throws IllegalStateException {
		if (isInitialized()) throw new IllegalStateException("Threading has been already initialized.");
		logger = LCCore.get().getThreadingLogger();
		TaskScheduler.init();
		cpu = new CPUTaskManager(threadFactory, taskPriorityManager, nbCPUThreads);
		cpu.start();
		resources.put(CPU, cpu);
		drives = new DrivesTaskManager(threadFactory, taskPriorityManager, drivesProvider);
		unmanaged = new ThreadPoolTaskManager("Unmanaged tasks manager", UNMANAGED, nbUnmanagedThreads, threadFactory, taskPriorityManager);
		resources.put(UNMANAGED, unmanaged);
		LCCore.get().toClose(new StopMultiThreading());
		if (traceTasksNotDone)
			ThreadingDebugHelper.traceTasksNotDone();
		TaskMonitoring.start(threadFactory);
		synchronized (resources) {
			for (TaskManager tm : resources.values())
				tm.started();
		}
	}
	
	public static boolean isInitialized() {
		return cpu != null;
	}
	
	private static class StopMultiThreading implements AsyncCloseable<Exception> {
		@Override
		public ISynchronizationPoint<Exception> closeAsync() {
			SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
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
								stop &= tm.isStopped();
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
									if (!tm.isStopped()) {
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
	
	private static CPUTaskManager cpu;
	private static DrivesTaskManager drives;
	private static ThreadPoolTaskManager unmanaged;
	
	public static TaskManager getCPUTaskManager() { return cpu; }
	
	public static DrivesTaskManager getDrivesTaskManager() { return drives; }

	public static ThreadPoolTaskManager getUnmanagedTaskManager() { return unmanaged; }
	
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
	
	private static Map<Thread, BlockedThreadHandler> blockedHandlers = new HashMap<>();
	
	/** Rregister the given thread. */
	public static void registerBlockedThreadHandler(BlockedThreadHandler handler, Thread thread) {
		synchronized (blockedHandlers) {
			blockedHandlers.put(thread, handler);
		}
	}

	/** Unregister the given thread. */
	public static void unregisterBlockedThreadHandler(Thread thread) {
		synchronized (blockedHandlers) {
			blockedHandlers.remove(thread);
		}
	}
	
	/** Return the object handling a case of a blocked thread for the given thread. */
	public static BlockedThreadHandler getBlockedThreadHandler(Thread thread) {
		return blockedHandlers.get(thread);
	}
	
	/** Wait for the given tasks to be done. */
	public static void waitFinished(Collection<? extends Task<?,?>> tasks) throws Exception {
		for (Task<?,?> t : tasks) {
			t.getOutput().blockThrow(0);
		}
	}
	
	/** Wait for the given tasks to finish, if one has an error this error is immediately thrown without waiting for other tasks. */
	public static <TError extends Exception> void waitUnblockedWithError(Collection<AsyncWork<?,TError>> tasks) throws TError, CancelException {
		for (AsyncWork<?,TError> t : tasks)
			t.blockResult(0);
	}
	
	/** Wait for one of the given task to be done. */
	public static void waitOneFinished(List<? extends Task<?,?>> tasks) {
		if (tasks.isEmpty()) return;
		if (tasks.size() == 1)
			try { tasks.get(0).getOutput().block(0); }
			catch (Throwable e) { /* ignore */ }
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		for (Task<?,?> t : tasks) {
			if (t.isDone()) return;
			t.getOutput().synchWithNoError(sp);
		}
		sp.block(0);
	}

	/** Return a string containing multi-threading status for debugging purposes. */
	public static String debug() {
		StringBuilder s = new StringBuilder();
		for (TaskManager tm : resources.values())
			tm.debug(s);
		return s.toString();
	}
	
	/** Print statistics. */
	public static String printStats() {
		StringBuilder s = new StringBuilder();
		for (TaskManager tm : resources.values())
			tm.printStats(s);
		return s.toString();
	}
	
}
