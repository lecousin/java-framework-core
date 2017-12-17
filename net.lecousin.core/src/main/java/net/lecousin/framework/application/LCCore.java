package net.lecousin.framework.application;

import java.io.Closeable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.application.libraries.LibrariesManager;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.memory.MemoryManager;
import net.lecousin.framework.util.AsyncCloseable;

/**
 * LC Core framework.
 */
public final class LCCore {
	
	private LCCore() { /* no instance */ }
	
	/** Interface to implement according to the environment.
	 * For example, StandaloneLCCore can be used for a standalone application JVM.
	 * Other implementations may be used for J2EE environments or multi-application environments.
	 */
	public static interface Environment {
		
		/** Initialization. */
		void start();
		
		/** Add an application. */
		void add(Application app);
		
		/** Get the application associated with the calling thread. */
		Application getApplication();
		
		/** Add a resource to close when closing the environment, the resource must be application independent. */
		void toClose(Closeable toClose);

		/** Add a resource to close when closing the environment, the resource must be application independent. */
		void toClose(AsyncCloseable<?> toClose);
		
		/** Signal a resource has been closed and do not need to be closed anymore. */
		void closed(Closeable closed);

		/** Signal a resource has been closed and do not need to be closed anymore. */
		void closed(AsyncCloseable<?> closed);
		
		/** Return the libraries manager. */
		LibrariesManager getSystemLibraries();
		
		/** Return a system logger, that may be used during initialization of the environment. */
		Logger getSystemLogger(String name);

		/** Return the system logger with name "Threading". */
		default Logger getThreadingLogger() { return getSystemLogger("Threading"); }
		
		/** Return the system logger with name "Memory". */
		default Logger getMemoryLogger() { return getSystemLogger("Memory"); }
		
		/** Stop the environment and close all applications. */
		void stop();
		
		/** Return true if the environment is stopping. */
		boolean isStopping();
	}
	
	private static Environment instance = null;
	
	/** Return the environment. */
	public static Environment get() {
		return instance;
	}
	
	/** Initialization. */
	public static synchronized void init(Environment env) {
		if (instance != null) throw new IllegalStateException("LCCore environment already exists");
		instance = env;

		// init logging system if not specified
		if (System.getProperty("org.apache.commons.logging.Log") == null) {
			try {
				LCCore.class.getClassLoader().loadClass("net.lecousin.framework.log.bridges.ApacheCommonsLogging");
				System.setProperty("org.apache.commons.logging.Log", "net.lecousin.framework.log.bridges.ApacheCommonsLogging");
			} catch (Throwable t) { /* ignore */ }
		}
		
		// register protocols
		String protocols = System.getProperty("java.protocol.handler.pkgs");
		if (protocols == null) protocols = "";
		if (protocols.length() > 0) protocols += "|";
		protocols += "net.lecousin.framework.protocols";
		System.setProperty("java.protocol.handler.pkgs", protocols);
		
		instance.start();
		new Task.Cpu<Void,Exception>("Initializing framework tools", Task.PRIORITY_NORMAL) {
			@Override
			public Void run() {
				MemoryManager.init();
				return null;
			}
		}.start();
	}
	
	static synchronized void init(Application app) {
		if (instance == null) {
			StandaloneLCCore env = new StandaloneLCCore();
			env.add(app);
			init(env);
		} else
			instance.add(app);
	}
	
	public static Application getApplication() {
		return instance.getApplication();
	}
	
	/** Stop the environement. */
	@SuppressFBWarnings("NN_NAKED_NOTIFY")
	public static void stop(boolean forceJvmToStop) {
		stop = true;
		synchronized (toDoInMainThread) {
			toDoInMainThread.notify();
		}
		if (instance != null) instance.stop();
		if (forceJvmToStop) {
			System.out.println("Stop JVM.");
			System.exit(0);
		}
	}
	
	/**
	 * Because some systems may need to use the main thread for specific action (like Cocoa),
	 * we keep the current thread aside, and continue on a new thread. 
	 * @param toContinue the Runnable to run on a new Thread to continue the application startup
	 */
	public static void keepMainThread(Runnable toContinue) {
		mainThread = Thread.currentThread();
		Thread thread = new Thread(toContinue, "LC Core - Main");
		thread.start();
		mainThreadLoop();
	}
	
	public static Thread getMainThread() {
		return mainThread;
	}
	
	private static Thread mainThread = null;
	private static boolean stop = false;
	private static TurnArray<Runnable> toDoInMainThread = new TurnArray<>(5);
	
	private static void mainThreadLoop() {
		while (!stop) {
			Runnable todo = null;
			synchronized (toDoInMainThread) {
				if (toDoInMainThread.isEmpty()) {
					try { toDoInMainThread.wait(); } catch (InterruptedException e) { /* continue */ }
					continue;
				}
				todo = toDoInMainThread.pop();
			}
			todo.run();
		}
	}
	
	private static MainThreadExecutor mainThreadExecutor = new MainThreadExecutor() {
		@Override
		public void execute(Runnable toExecute) {
			synchronized (toDoInMainThread) {
				toDoInMainThread.push(toExecute);
				toDoInMainThread.notify();
			}
		}
		
		@Override
		public Runnable pop() {
			synchronized (toDoInMainThread) {
				return toDoInMainThread.pollFirst();
			}
		}
	};
	
	/** Interface to implement to override the main thread executor. */
	public static interface MainThreadExecutor {
		/** Execute a task. */
		public void execute(Runnable toExecute);
		
		/** Remove and return the next task to execute. */
		public Runnable pop();
	}
	
	/** Ask to execute the given Runnable in the main thread as soon as the thread is available. */
	public static void executeInMainThread(Runnable toExecute) {
		synchronized (LCCore.class) {
			mainThreadExecutor.execute(toExecute);
		}
	}
	
	/** Replace the main thread executor. */
	public static void replaceMainThreadExecutor(MainThreadExecutor executor) {
		MainThreadExecutor previous;
		synchronized (LCCore.class) {
			previous = mainThreadExecutor;
			mainThreadExecutor = executor;
		}
		do {
			Runnable toExecute = previous.pop();
			if (toExecute == null) break;
			executor.execute(toExecute);
		} while (true);
	}
}
