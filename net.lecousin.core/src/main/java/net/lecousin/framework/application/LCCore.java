package net.lecousin.framework.application;

import java.io.Closeable;

import net.lecousin.framework.application.libraries.LibrariesManager;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.memory.MemoryManager;
import net.lecousin.framework.mutable.MutableBoolean;
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
		
		/** Return true if the current thread is allowed to change system configuration. */
		boolean currentThreadIsSystem();
		
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
	private static boolean started = false;
	
	/** Return the environment. */
	public static Environment get() {
		return instance;
	}
	
	/** Set the environment. */
	public static void set(Environment env) {
		if (instance != null) throw new IllegalStateException("LCCore environment already exists");
		instance = env;
	}
	
	/** Initialize properties. */
	public static void initEnvironment() {
		// init logging system if not specified
		if (System.getProperty("org.apache.commons.logging.Log") == null)
			System.setProperty("org.apache.commons.logging.Log", "net.lecousin.framework.log.bridges.ApacheCommonsLogging");
		
		// register protocols
		String protocols = System.getProperty("java.protocol.handler.pkgs");
		if (protocols == null) protocols = "";
		if (!protocols.contains("net.lecousin.framework.protocols")) {
			if (protocols.length() > 0) protocols += "|";
			protocols += "net.lecousin.framework.protocols";
			System.setProperty("java.protocol.handler.pkgs", protocols);
		}
	}
	
	/** Return true if the environment is already started. */
	public static boolean isStarted() {
		return started;
	}
	
	/** Initialization. */
	public static synchronized void start() {
		if (started) throw new IllegalStateException("LCCore environment already started");
		started = true;
		
		initEnvironment();
		
		instance.start();
		Task.cpu("Initializing framework tools", () -> {
			MemoryManager.init();
			return null;
		}).start();
	}
	
	static synchronized void start(Application app) {
		if (instance == null) {
			StandaloneLCCore env = new StandaloneLCCore();
			set(env);
		}
		instance.add(app);
		if (!isStarted())
			start();
	}
	
	public static Application getApplication() {
		return instance.getApplication();
	}
	
	public static boolean isStopping() {
		return stop != null;
	}
	
	/** Stop the environement. */
	@SuppressWarnings("squid:S106") // print to System.out/err because we are stopping and logging system is not available
	public static MutableBoolean stop(boolean forceJvmToStop) {
		if (stop != null) {
			new Exception("LCCore already stopped", stop).printStackTrace(System.err);
			return new MutableBoolean(true);
		}
		stop = new Exception("LCCore stop requested here");
		MutableBoolean stopped = new MutableBoolean(false);
		new Thread("Stopping LCCore") {
			@Override
			public void run() {
				synchronized (toDoInMainThread) {
					toDoInMainThread.notify();
				}
				if (instance != null) instance.stop();
				if (forceJvmToStop) {
					System.out.println("Stop JVM.");
					System.exit(0);
				}
				synchronized (stopped) {
					stopped.set(true);
					stopped.notifyAll();
				}
			}
		}.start();
		return stopped;
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
	private static Exception stop = null;
	private static TurnArray<Runnable> toDoInMainThread = new TurnArray<>(5);
	
	@SuppressWarnings("squid:S2142") // we should continue to shutdown properly
	private static void mainThreadLoop() {
		while (stop == null) {
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
		void execute(Runnable toExecute);
		
		/** Remove and return the next task to execute. */
		Runnable pop();
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
