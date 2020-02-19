package net.lecousin.framework.concurrent;

import java.io.Closeable;
import java.io.PrintStream;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.async.Async;

/**
 * This class handles printing to the console in a separate thread,
 * so the calling thread is not blocked.
 * An instance can be obtain using LCCore.getApplication().getConsole()
 */
public class Console implements Closeable {
	
	/** Print the given line. */
	public synchronized void out(String line) {
		toPrint.addLast(line);
		stream.addLast(out);
		this.notify();
	}
	
	/** Print the given error. */
	public synchronized void out(Throwable t) {
		toPrint.addLast(t);
		stream.addLast(out);
		this.notify();
	}
	
	/** Print the given line. */
	public synchronized void err(String line) {
		toPrint.addLast(line);
		stream.addLast(err);
		this.notify();
	}

	/** Print the given error. */
	public synchronized void err(Throwable t) {
		toPrint.addLast(t);
		stream.addLast(err);
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
				Object obj;
				PrintStream s;
				synchronized (Console.this) {
					obj = toPrint.pollFirst();
					if (obj == null) {
						try { Console.this.wait(); }
						catch (InterruptedException e) { break; }
						continue;
					}
					s = stream.pollFirst();
				}
				printTo(obj, s);
			}
			while (!toPrint.isEmpty())
				printTo(toPrint.removeFirst(), stream.removeFirst());
			System.out.println("Console stopped for " + app.getFullName());
			stopped.unblock();
		});
		thread.setName("Console for " + app.getFullName());
		thread.start();
	}
	
	private TurnArray<Object> toPrint = new TurnArray<>(100);
	private TurnArray<PrintStream> stream = new TurnArray<>(100);
	private Thread thread;
	private boolean stop = false;
	private Async<Exception> stopped = new Async<>();
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
