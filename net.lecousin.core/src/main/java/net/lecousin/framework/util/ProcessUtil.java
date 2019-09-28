package net.lecousin.framework.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.mutable.Mutable;

/**
 * Utility methods with Process.
 */
public final class ProcessUtil {

	private ProcessUtil() { /* no instance */ }
	
	/**
	 * Create a thread that wait for the given process to end, and call the given listener.
	 */
	@SuppressWarnings("squid:S2142") // InterruptedException
	public static void onProcessExited(Process process, Consumer<Integer> exitValueListener) {
		Application app = LCCore.getApplication();
		Mutable<Thread> mt = new Mutable<>(null);
		Thread t = app.getThreadFactory().newThread(() -> {
			try { exitValueListener.accept(Integer.valueOf(process.waitFor())); }
			catch (InterruptedException e) { /* ignore and quit */ }
			app.interrupted(mt.get());
		});
		mt.set(t);
		t.setName("Waiting for process to exit");
		t.start();
		app.toInterruptOnShutdown(t);
	}
	
	/** Launch 2 threads to consume both output and error streams, and call the listeners for each line read. */
	public static void consumeProcessConsole(Process process, Consumer<String> outputListener, Consumer<String> errorListener) {
		Application app = LCCore.getApplication();
		ThreadFactory factory = app.getThreadFactory();
		Thread t;
		ConsoleConsumer cc;
		cc = new ConsoleConsumer(process.getInputStream(), outputListener);
		t = factory.newThread(cc);
		t.setName("Process output console consumer");
		cc.app = app;
		cc.t = t;
		t.start();
		app.toInterruptOnShutdown(t);
		cc = new ConsoleConsumer(process.getErrorStream(), errorListener);
		t = factory.newThread(cc);
		t.setName("Process error console consumer");
		cc.app = app;
		cc.t = t;
		t.start();
		app.toInterruptOnShutdown(t);
	}
	
	/** Thread that read from the given stream and call the listener for each line. */
	public static class ConsoleConsumer implements Runnable {
		
		/** Constructor. */
		public ConsoleConsumer(InputStream input, Consumer<String> listener) {
			this.input = input;
			this.listener = listener;
		}
		
		private InputStream input;
		private Consumer<String> listener;
		private Application app;
		private Thread t;
		
		@Override
		public void run() {
			StringBuilder line = new StringBuilder();
			byte[] buffer = new byte[1024];
			do {
				int nb;
				try { nb = input.read(buffer); }
				catch (Exception e) { break; }
				if (nb < 0) break;
				if (nb == 0) continue;
				String s = new String(buffer, 0, nb, StandardCharsets.ISO_8859_1);
				while (s.length() > 0) {
					int i = s.indexOf('\r');
					int j = s.indexOf('\n');
					if (i < 0 && j < 0) {
						line.append(s);
						break;
					} else if (i < 0) {
						line.append(s.substring(0,j));
						listener.accept(line.toString());
						line = new StringBuilder();
						s = s.substring(j + 1);
					} else if (j < 0) {
						line.append(s.substring(0,i));
						listener.accept(line.toString());
						line = new StringBuilder();
						s = s.substring(i + 1);
					} else if (i == j - 1) {
						line.append(s.substring(0,i));
						listener.accept(line.toString());
						line = new StringBuilder();
						s = s.substring(j + 1);
					} else {
						if (j < i) i = j;
						line.append(s.substring(0,i));
						listener.accept(line.toString());
						line = new StringBuilder();
						s = s.substring(i + 1);
					}
				}
			} while (true);
			if (line.length() > 0)
				listener.accept(line.toString());
			try { input.close(); } catch (Exception e) { /* ignore */ }
			app.interrupted(t);
		}
	}
	
}
