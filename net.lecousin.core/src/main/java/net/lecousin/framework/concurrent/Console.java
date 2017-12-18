package net.lecousin.framework.concurrent;

import java.io.Closeable;
import java.io.PrintStream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
	public Console(Application app) {
		out = System.out;
		err = System.err;
		thread = app.getThreadFactory().newThread(new Runnable() {
			@Override
			public void run() {
				while (!stop) {
					Object out = null;
					Object err;
					synchronized (Console.this) {
						err = stderr.pollFirst();
						if (err == null) {
							out = stdout.pollFirst();
							if (out == null) {
								try { Console.this.wait(); }
								catch (InterruptedException e) { break; }
								continue;
							}
						}
					}
					if (err != null) {
						if (err instanceof CharSequence)
							Console.this.err.println(err);
						else if (err instanceof Throwable) {
							((Throwable)err).printStackTrace(Console.this.err);
						}
					} else {
						if (out instanceof CharSequence)
							Console.this.out.println(out);
						else if (out instanceof Throwable)
							((Throwable)out).printStackTrace(Console.this.out);
					}
				}
				while (!stderr.isEmpty())
					Console.this.err.println(stderr.removeFirst());
				while (!stdout.isEmpty())
					Console.this.out.println(stdout.removeFirst());
				System.out.println("Console stopped for " + app.getGroupId() + '/' + app.getArtifactId());
				stopped.unblock();
			}
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
	
	@SuppressFBWarnings("NN_NAKED_NOTIFY")
	@Override
	public void close() {
		stop = true;
		synchronized (this) { this.notify(); }
		stopped.block(0);
	}
	
}
