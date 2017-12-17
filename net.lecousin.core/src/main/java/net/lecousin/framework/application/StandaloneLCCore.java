package net.lecousin.framework.application;

import java.io.Closeable;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import net.lecousin.framework.LCCoreVersion;
import net.lecousin.framework.application.libraries.LibrariesManager;
import net.lecousin.framework.concurrent.StandaloneTaskPriorityManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.util.AsyncCloseable;
import net.lecousin.framework.util.DebugUtil;
import net.lecousin.framework.util.Pair;

/**
 * LC Core Environment for a standalone application. This is the default implementation.
 */
public class StandaloneLCCore implements LCCore.Environment {

	/** Constructor with the single application. */
	public StandaloneLCCore(Application app) {
		System.out.println("net.lecousin.framework " + LCCoreVersion.VERSION);
		startTime = System.nanoTime();
		closing = false;
		this.app = app;
		
		Map<Thread,StackTraceElement[]> threads = Thread.getAllStackTraces();
		for (Map.Entry<Thread,StackTraceElement[]> thread : threads.entrySet())
			threadsBeforeInit.add(thread.getKey());
		threads = null;
	}
	
	private Application app;
	private long startTime;
	private boolean closing;
	private List<Thread> threadsBeforeInit = new LinkedList<Thread>();
	private ArrayList<Closeable> toCloseSync = new ArrayList<>();
	private ArrayList<AsyncCloseable<?>> toCloseAsync = new ArrayList<>();
	
	@Override
	public void add(Application app) {
		throw new IllegalStateException("Cannot add several application on a standalone LCCore environment");
	}
	
	@SuppressFBWarnings("MS_SHOULD_BE_FINAL")
	public static long logThreadingInterval = 30000;
	
	@Override
	public void start() {
		// start multi-threading system
		Threading.init(app.getThreadFactory(), StandaloneTaskPriorityManager.class);
		
		// debugging
		if (app.isDebugMode()) {
			final Logger logger = app.getLoggerFactory().getLogger("Threading Status");
			class ThreadingLogger extends Thread implements Closeable {
				ThreadingLogger() {
					super("Threading logger");
				}
				
				private boolean closed = false;
				
				@Override
				public void run() {
					do {
						synchronized (this) {
							try { this.wait(logThreadingInterval); }
							catch (InterruptedException e) { return; }
						}
						if (closed) return;
						logger.debug("\n" + Threading.debug());
					} while (!closed);
				}
				
				@Override
				public void close() {
					synchronized (this) {
						closed = true;
						this.notify();
					}
				}
			}
			
			if (logger.debug()) {
				@SuppressWarnings("resource")
				ThreadingLogger t = new ThreadingLogger();
				t.start();
				app.toClose(t);
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
	@SuppressWarnings("restriction")
	public void stop() {
		closing = true;
		if (!app.isStopping())
			app.stop();
		
		System.out.println(" * Closing resources");
		for (Closeable c : new ArrayList<>(toCloseSync)) {
			System.out.println("     - " + c);
			try { c.close(); } catch (Throwable t) {
				System.err.println("Error closing resource " + c);
				t.printStackTrace(System.err);
			}
		}

		List<Pair<AsyncCloseable<?>,ISynchronizationPoint<?>>> closing = new LinkedList<>();
		for (AsyncCloseable<?> s : new ArrayList<>(toCloseAsync)) {
			System.out.println(" * Closing " + s);
			closing.add(new Pair<>(s, s.closeAsync()));
		}
		toCloseAsync.clear();
		long start = System.currentTimeMillis();
		do {
			for (Iterator<Pair<AsyncCloseable<?>,ISynchronizationPoint<?>>> it = closing.iterator(); it.hasNext(); ) {
				Pair<AsyncCloseable<?>,ISynchronizationPoint<?>> s = it.next();
				if (s.getValue2().isUnblocked()) {
					System.out.println(" * Closed: " + s.getValue1());
					it.remove();
				}
			}
			if (closing.isEmpty()) break;
			try { Thread.sleep(100); }
			catch (InterruptedException e) { break; }
			if (System.currentTimeMillis() - start > 10000) {
				System.out.println("Ressources are still closing, but we don't wait more than 10 seconds.");
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
				if (thread.getKey().getName().startsWith("AWT-")) continue;
				if (thread.getKey().getName().equals("DestroyJavaVM")) continue;
				if (threadsBeforeInit.contains(thread.getKey())) {
					if ((count % 10) == 0)
						System.out.println("Thread ignored because started before: " + thread.getKey());
					continue;
				}
				nb++;
				if ((count % 10) == 0) {
					System.out.println("Thread: " + thread.getKey());
					System.out.println(DebugUtil.createStackTrace(new StringBuilder(), thread.getValue()).toString());
				}
			}
			if (nb == 0)
				break;
			if ((count % 10) == 0)
				System.out.println("Waiting for threads to stop");
			try { Thread.sleep(100); }
			catch (InterruptedException e) { break; }
			count++;
		} while (count < 50);

		long end = System.nanoTime();
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		try {
			ClassLoader.getSystemClassLoader().loadClass("com.sun.management.OperatingSystemMXBean");
			if (os instanceof com.sun.management.OperatingSystemMXBean) {
				long cpuUsage = ((com.sun.management.OperatingSystemMXBean)os).getProcessCpuTime();
				System.out.println("JVM used "
					+ String.format("%.5f", new Double(cpuUsage * 1.d / 1000000000)) + "s. of CPU, while running "
					+ String.format("%.5f", new Double((end - startTime) * 1.d / 1000000000)) + "s. with "
					+ os.getAvailableProcessors() + " processors ("
					+ String.format("%.2f", new Double(cpuUsage * 100.d / ((end - startTime) * os.getAvailableProcessors())))
					+ "%)");
			} else
				throw new Exception();
		} catch (Throwable t) {
			System.out.println("JVM has run during " + String.format("%.5f", new Double((end - startTime) * 1.d / 1000000000)) + "s.");
		}
	}
}
