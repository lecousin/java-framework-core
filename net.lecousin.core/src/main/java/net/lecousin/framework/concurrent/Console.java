package net.lecousin.framework.concurrent;

import java.io.Closeable;
import java.io.PrintStream;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.threads.ApplicationThread;

/**
 * This class handles printing to the console in a separate thread,
 * so the calling thread is not blocked.
 * An instance can be obtain using LCCore.getApplication().getConsole()
 */
public class Console implements Closeable {
	
	/** Print the given line. */
	public void out(String line) {
		add(line, out);
	}
	
	/** Print the given error. */
	public void out(Throwable t) {
		add(t, out);
	}
	
	/** Print the given line. */
	public void err(String line) {
		add(line, err);
	}

	/** Print the given error. */
	public void err(Throwable t) {
		add(t, err);
	}
	
	@SuppressWarnings({"java:S3358", "java:S2142"})
	private void add(Object o, PrintStream s) {
		synchronized (this) {
			toPrint.addLast(o);
			stream.addLast(s);
			this.notify();
		}
		int nb = toPrint.size();
		if (nb > 5000) {
			// pause the thread to let time to logging
			try {
				Thread.sleep(nb > 10000 ? 200 : nb > 7500 ? 100 : 25);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	/** Instantiate for the given application. */
	@SuppressWarnings({
		"squid:S106", // writing to the console is the purpose of this class
	})
	public Console(Application app) {
		this.app = app;
		out = System.out;
		err = System.err;
		thread = app.createThread(new Printer());
		thread.setName("Console for " + app.getFullName());
		thread.start();
	}
	
	private Application app;
	private TurnArray<Object> toPrint = new TurnArray<>(100);
	private TurnArray<PrintStream> stream = new TurnArray<>(100);
	private Thread thread;
	private boolean stop = false;
	private Async<Exception> stopped = new Async<>();
	private PrintStream out;
	private PrintStream err;
	
	private class Printer implements ApplicationThread {
		@Override
		public Application getApplication() {
			return app;
		}
		
		@Override
		@SuppressWarnings({
			"squid:S106", // writing to the console is the purpose of this class
			"squid:S2142" // we stop the thread on interruption
		})
		public void run() {
			while (true) {
				Object obj;
				PrintStream s;
				synchronized (Console.this) {
					obj = toPrint.pollFirst();
					if (obj == null) {
						if (stop) break;
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
		}
		
		private void printTo(Object toPrint, PrintStream stream) {
			if (toPrint instanceof CharSequence)
				stream.println(toPrint);
			else if (toPrint instanceof Throwable) {
				((Throwable)toPrint).printStackTrace(stream);
			}
		}
		
		@Override
		public void debugStatus(StringBuilder s) {
			s.append(" - Console: ").append(toPrint.size()).append(" pending logs\r\n");
		}
	}
	
	@Override
	public void close() {
		stop = true;
		synchronized (this) { this.notify(); }
		stopped.block(0);
	}
	
}
