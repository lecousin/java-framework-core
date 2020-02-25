package net.lecousin.framework.application;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.LCCoreVersion;
import net.lecousin.framework.application.libraries.LibrariesManager;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.DrivesThreadingManager.DrivesProvider;
import net.lecousin.framework.concurrent.threads.TaskManagerMonitor;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.concurrent.threads.priority.SimpleTaskPriorityManager;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.memory.MemoryManager;
import net.lecousin.framework.util.AsyncCloseable;
import net.lecousin.framework.util.DebugUtil;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.ThreadUtil;

/**
 * LC Core Environment for a standalone application. This is the default implementation.
 */
public class StandaloneLCCore implements LCCore.Environment {

	/** Constructor. */
	@SuppressWarnings("squid:S106") // print to System.out because logging system not yet available
	public StandaloneLCCore() {
		System.out.println("net.lecousin.framework " + LCCoreVersion.VERSION);
		startTime = System.nanoTime();
		closing = false;
		
		Map<Thread,StackTraceElement[]> threads = Thread.getAllStackTraces();
		for (Map.Entry<Thread,StackTraceElement[]> thread : threads.entrySet())
			threadsBeforeInit.add(thread.getKey());
		threads = null;
	}
	
	private Application app = null;
	private long startTime;
	private boolean closing;
	private List<Thread> threadsBeforeInit = new LinkedList<>();
	private ArrayList<Closeable> toCloseSync = new ArrayList<>();
	private ArrayList<AsyncCloseable<?>> toCloseAsync = new ArrayList<>();
	private int nbCPUThreads = -1;
	private int nbUnmanagedThreads = -1;
	private DrivesProvider drivesProvider = null;
	private TaskManagerMonitor.Configuration cpuMonitorConfig = new TaskManagerMonitor.Configuration(
		60 * 1000, // 1 minute
		5 * 60 * 1000, // 5 minutes
		15 * 60 * 1000, // 15 minutes
		false
	);
	private TaskManagerMonitor.Configuration driveMonitorConfig = new TaskManagerMonitor.Configuration(
		30 * 1000, // 30 seconds
		1 * 60 * 1000, // 1 minute
		15 * 60 * 1000, // 15 minutes
		false
	);
	private TaskManagerMonitor.Configuration unmanagedMonitorConfig = new TaskManagerMonitor.Configuration(
		60 * 1000, // 1 minute
		5 * 60 * 1000, // 5 minutes
		15 * 60 * 1000, // 15 minutes
		false
	);

	@Override
	public void add(Application app) {
		if (this.app == null)
			this.app = app;
		else
			throw new IllegalStateException("Cannot add several application on a standalone LCCore environment");
	}
	
	private static final String ERROR_ALREADY_INITIALIZED = "Threading has been already initialized.";
	
	/** Set the number of CPU threads to use by multi-threading system. */
	public void setCPUThreads(int nbThreads) {
		if (Threading.isInitialized()) throw new IllegalStateException(ERROR_ALREADY_INITIALIZED);
		nbCPUThreads = nbThreads;
	}
	
	/** Set the number of threads to use by multi-threading system for unmanaged tasks. */
	public void setUnmanagedThreads(int nbThreads) {
		if (Threading.isInitialized()) throw new IllegalStateException(ERROR_ALREADY_INITIALIZED);
		nbUnmanagedThreads = nbThreads;
	}
	
	/** Set the drives provider to use by multi-threading system. */
	public void setDrivesProvider(DrivesProvider provider) {
		if (Threading.isInitialized()) throw new IllegalStateException(ERROR_ALREADY_INITIALIZED);
		drivesProvider = provider;
	}

	/** Set the monitoring configuration for CPU tasks. */
	public void setCpuMonitorConfig(TaskManagerMonitor.Configuration config) {
		if (Threading.isInitialized()) throw new IllegalStateException(ERROR_ALREADY_INITIALIZED);
		cpuMonitorConfig = config;
	}

	/** Set the monitoring configuration for drive tasks. */
	public void setDriveMonitorConfig(TaskManagerMonitor.Configuration config) {
		if (Threading.isInitialized()) throw new IllegalStateException(ERROR_ALREADY_INITIALIZED);
		driveMonitorConfig = config;
	}

	/** Set the monitoring configuration for unmanaged tasks. */
	public void setUnmanagedMonitorConfig(TaskManagerMonitor.Configuration config) {
		if (Threading.isInitialized()) throw new IllegalStateException(ERROR_ALREADY_INITIALIZED);
		unmanagedMonitorConfig = config;
	}
	
	private static long logThreadingInterval = 30000;
	
	public static void setLogThreadingInterval(long interval) {
		logThreadingInterval = interval;
	}
	
	@Override
	@SuppressWarnings("squid:S1312") // logger is used only in debug mode, we do not want it as static
	public void start() {
		// start multi-threading system
		Threading.init(
			app.getThreadFactory(),
			SimpleTaskPriorityManager.class,
			nbCPUThreads,
			cpuMonitorConfig,
			drivesProvider,
			driveMonitorConfig,
			nbUnmanagedThreads,
			unmanagedMonitorConfig
		);
		
		// debugging
		if (app.isDebugMode()) {
			final Logger logger = app.getLoggerFactory().getLogger("Threading Status");
			class ThreadingLogger extends Thread implements Closeable {
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
			
			if (logger.debug()) {
				ThreadingLogger t = new ThreadingLogger();
				t.start();
				app.toClose(0, t);
			}
		}
	}
	
	@Override
	public Application getApplication() {
		return app;
	}
	
	@Override
	public Logger getSystemLogger(String name) {
		return app.getLoggerFactory().getLogger(name);
	}
	
	@Override
	public LibrariesManager getSystemLibraries() {
		return app.getLibrariesManager();
	}
	
	@Override
	public boolean currentThreadIsSystem() {
		return true;
	}
	
	@Override
	public void toClose(Closeable c) {
		synchronized (toCloseSync) { toCloseSync.add(c); }
	}
	
	@Override
	public void toClose(AsyncCloseable<?> c) {
		synchronized (toCloseAsync) { toCloseAsync.add(c); }
	}
	
	@Override
	public void closed(Closeable c) {
		synchronized (toCloseSync) { toCloseSync.remove(c); }
	}
	
	@Override
	public void closed(AsyncCloseable<?> c) {
		synchronized (toCloseAsync) { toCloseAsync.remove(c); }
	}
	
	@Override
	public boolean isStopping() {
		return closing;
	}

	@Override
	@SuppressWarnings({
		"squid:S1872", // to avoid class loading issue
		"squid:S106", // loggers are not available while shutting down
		"squid:S2142", // we are shutting down properly
		"squid:S3014", // this is intentional
		"squid:S135", // this would make the code more difficult to read
		"squid:S3776", // complexity: we do not want to split into several methods
	})
	public void stop() {
		closing = true;
		if (!app.isStopping())
			app.stop();
		
		System.out.println(" * Closing resources");
		for (Closeable c : new ArrayList<>(toCloseSync)) {
			System.out.println("     - " + c);
			try { c.close(); } catch (Exception t) {
				System.err.println("Error closing resource " + c);
				t.printStackTrace(System.err);
			}
		}

		List<Pair<AsyncCloseable<?>,IAsync<?>>> closingResources = new LinkedList<>();
		for (AsyncCloseable<?> s : new ArrayList<>(toCloseAsync)) {
			System.out.println(" * Closing " + s);
			closingResources.add(new Pair<>(s, s.closeAsync()));
		}
		toCloseAsync.clear();
		long start = System.currentTimeMillis();
		do {
			for (Iterator<Pair<AsyncCloseable<?>,IAsync<?>>> it = closingResources.iterator(); it.hasNext(); ) {
				Pair<AsyncCloseable<?>,IAsync<?>> s = it.next();
				if (s.getValue2().isDone()) {
					System.out.println(" * Closed: " + s.getValue1());
					it.remove();
				}
			}
			if (closingResources.isEmpty()) break;
			if (!ThreadUtil.sleep(100)) break;
			if (System.currentTimeMillis() - start > 15000) {
				System.out.println("Ressources are still closing, but we don't wait more than 15 seconds.");
				break;
			}
		} while (true);
		
		Map<Thread,StackTraceElement[]> threads = Thread.getAllStackTraces();
		int count = 0;
		do {
			int nb = 0;
			if ((count % 10) == 0)
				System.out.println("Threads still running:");
			for (Map.Entry<Thread,StackTraceElement[]> thread : threads.entrySet()) {
				if (thread.getKey() == Thread.currentThread()) continue;
				ThreadGroup g = thread.getKey().getThreadGroup();
				if (g == null || "system".equals(g.getName())) continue;
				if ("DestroyJavaVM".equals(thread.getKey().getName())) continue;
				if (thread.getKey().getName().startsWith("AWT-")) continue;
				if (threadsBeforeInit.contains(thread.getKey())) {
					if ((count % 10) == 0)
						System.out.println("Thread ignored because started before: " + thread.getKey());
					continue;
				}
				nb++;
				if ((count % 10) == 0) {
					System.out.println("Thread: " + thread.getKey() + " [id " + thread.getKey().getId() + "]");
					System.out.println(DebugUtil.createStackTrace(new StringBuilder(), thread.getValue()).toString());
				}
			}
			if (nb == 0)
				break;
			if ((count % 10) == 0) {
				System.out.println("Waiting for threads to stop");
			}
			if (!ThreadUtil.sleep(100)) break;
			count++;
		} while (count < 50);

		long end = System.nanoTime();
		
		StringBuilder s = new StringBuilder(2048);
		MemoryManager.logMemory(s);
		System.out.println(s.toString());
		System.out.println("JVM has run during " + String.format("%.5f", Double.valueOf((end - startTime) * 1.d / 1000000000)) + "s.");
	}
}
