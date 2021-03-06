package net.lecousin.framework.util;

import java.util.function.Consumer;

import net.lecousin.framework.event.Event;

/**
 * Closeable resource, with the possibility to add and remove listeners to be called when the resource is closed.
 */
public interface CloseableListenable {

	/** Close. */
	@SuppressWarnings("squid:S00112") // implementation should be able to throw any exception
	void close() throws Exception;
	
	/** Return true if closed. */
	boolean isClosed();
	
	/** Add a listener to be called once this resource is closed. */
	void addCloseListener(Consumer<CloseableListenable> listener);

	/** Add a listener to be called once this resource is closed. */
	void addCloseListener(Runnable listener);
	
	/** Remove a listener. */
	void removeCloseListener(Consumer<CloseableListenable> listener);
	
	/** Remove a listener. */
	void removeCloseListener(Runnable listener);
	
	/** Default implementation to handle listeners fired on close. */
	public static class Impl implements CloseableListenable {
		private Event<CloseableListenable> event = new Event<>();
		private boolean closed = false;
		
		@Override
		public boolean isClosed() { return closed; }
		
		@Override
		public void close() {
			closed = true;
			event.fire(this);
		}
		
		@Override
		public void addCloseListener(Consumer<CloseableListenable> listener) {
			event.addListener(listener);
		}

		@Override
		public void addCloseListener(Runnable listener) {
			event.addListener(listener);
		}
		
		@Override
		public void removeCloseListener(Consumer<CloseableListenable> listener) {
			event.removeListener(listener);
		}

		@Override
		public void removeCloseListener(Runnable listener) {
			event.removeListener(listener);
		}
	}
	
}
