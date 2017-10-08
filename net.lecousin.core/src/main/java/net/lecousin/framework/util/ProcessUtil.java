package net.lecousin.framework.util;

import java.io.InputStream;

import net.lecousin.framework.event.Listener;

/**
 * Utility methods with Process.
 */
public class ProcessUtil {

	/**
	 * Create a thread that wait for the given process to end, and call the given listener.
	 */
	public static void onProcessExited(Process process, Listener<Integer> exitValueListener) {
		new Thread("Waiting for process to exit") {
			@Override
			public void run() {
				try { exitValueListener.fire(Integer.valueOf(process.waitFor())); }
				catch (InterruptedException e) { /* ignore and quit */ }
			}
		}.start();
	}
	
	/** Launch 2 threads to consume both output and error streams, and call the listeners for each line read. */
	public static void consumeProcessConsole(Process process, Listener<String> outputListener, Listener<String> errorListener) {
		new ConsoleConsumer(process.getInputStream(), outputListener).start();
		new ConsoleConsumer(process.getErrorStream(), errorListener).start();
	}
	
	/** Thread that read from the given stream and call the listener for each line. */
	public static class ConsoleConsumer extends Thread {
		
		/** Constructor. */
		public ConsoleConsumer(InputStream input, Listener<String> listener) {
			super("Console consumer");
			this.input = input;
			this.listener = listener;
		}
		
		private InputStream input;
		private Listener<String> listener;
		
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
				String s = new String(buffer, 0, nb);
				while (s.length() > 0) {
					int i = s.indexOf('\r');
					int j = s.indexOf('\n');
					if (i < 0 && j < 0) {
						line.append(s);
						break;
					} else if (i < 0) {
						line.append(s.substring(0,j));
						listener.fire(line.toString());
						line = new StringBuilder();
						s = s.substring(j + 1);
					} else if (j < 0) {
						line.append(s.substring(0,i));
						listener.fire(line.toString());
						line = new StringBuilder();
						s = s.substring(i + 1);
					} else if (i == j - 1) {
						line.append(s.substring(0,i));
						listener.fire(line.toString());
						line = new StringBuilder();
						s = s.substring(j + 1);
					} else {
						if (j < i) i = j;
						line.append(s.substring(0,i));
						listener.fire(line.toString());
						line = new StringBuilder();
						s = s.substring(i + 1);
					}
				}
			} while (true);
			if (line.length() > 0)
				listener.fire(line.toString());
			try { input.close(); } catch (Throwable t) { /* ignore */ }
		}
	}
	
}
