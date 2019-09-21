package net.lecousin.framework.concurrent;

import java.io.Closeable;
import java.io.PrintStream;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;

/**
 * This class handles printing to the console in a separate thread,
 * so the calling thread is not blocked.
 * An instance can be obtain using LCCore.getApplication().getConsole()
 */
public class Console implements Closeable {
	
	/** Print the given line. */
	public synchronized void out(String line) {
		stdout.addLast(line);
		this.notify();
	}
	
	/** Print the given error. */
	public synchronized void out(Throwable t) {
		stdout.addLast(t);
		this.notify();
	}
	
	/** Print the given line. */
	public synchronized void err(String line) {
		stderr.addLast(line);
		this.notify();
	}

	/** Print the given error. */
	public synchronized void err(Throwable t) {
		stderr.addLast(t);
		this.notify();
	}

	/** Instantiate for the given application. */
	@SuppressWarnings({
		"squid:S106", // writing to the console is the purpose of this class
		"squid:S2142" // we stop the thread on interruption
	})
	public Console(Application app) {
		out = System.out;
		err = System.err;
		thread = app.getThreadFactory().newThread(() -> {
			while (!stop) {
				Object outObj = null;
				Object errObj;
				synchronized (Console.this) {
					errObj = stderr.pollFirst();
					if (errObj == null) {
						outObj = stdout.pollFirst();
						if (outObj == null) {
							try { Console.this.wait(); }
							catch (InterruptedException e) { break; }
							continue;
						}
					}
				}
				if (errObj != null)
					printTo(errObj, err);
				else
					printTo(outObj, out);
			}
			while (!stderr.isEmpty())
				Console.this.err.println(stderr.removeFirst());
			while (!stdout.isEmpty())
				Console.this.out.println(stdout.removeFirst());
			System.out.println("Console stopped for " + app.getGroupId() + '/' + app.getArtifactId());
			stopped.unblock();
		});
		thread.setName("Console for " + app.getGroupId() + "." + app.getArtifactId() + " " + app.getVersion().toString());
		thread.start();
	}
	
	private TurnArray<Object> stdout = new TurnArray<>(100);
	private TurnArray<Object> stderr = new TurnArray<>(50);
	private Thread thread;
	private boolean stop = false;
	private SynchronizationPoint<Exception> stopped = new SynchronizationPoint<>();
	private PrintStream out;
	private PrintStream err;
	
	private static void printTo(Object toPrint, PrintStream stream) {
		if (toPrint instanceof CharSequence)
			stream.println(toPrint);
		else if (toPrint instanceof Throwable) {
			((Throwable)toPrint).printStackTrace(stream);
		}
	}
	
	@Override
	public void close() {
		stop = true;
		synchronized (this) { this.notify(); }
		stopped.block(0);
	}
	
}
