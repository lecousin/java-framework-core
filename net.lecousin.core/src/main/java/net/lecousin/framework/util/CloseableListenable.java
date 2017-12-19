package net.lecousin.framework.util;

import net.lecousin.framework.event.Event;
import net.lecousin.framework.event.Listener;

/**
 * Closeable resource, with the possibility to add and remove listeners to be called when the resource is closed.
 */
public interface CloseableListenable<TError extends Exception> {

	/** Close. */
	public void close() throws TError;
	
	/** Return true if closed. */
	public boolean isClosed();
	
	/** Add a listener to be called once this resource is closed. */
	public void addCloseListener(Listener<CloseableListenable<TError>> listener);

	/** Add a listener to be called once this resource is closed. */
	public void addCloseListener(Runnable listener);
	
	/** Remove a listener. */
	public void removeCloseListener(Listener<CloseableListenable<TError>> listener);
	
	/** Remove a listener. */
	public void removeCloseListener(Runnable listener);
	
	/** Default implementation to handle listeners fired on close. */
	public static class Impl<TError extends Exception> implements CloseableListenable<TError> {
		private Event<CloseableListenable<TError>> event = new Event<>();
		private boolean closed = false;
		
		@Override
		public boolean isClosed() { return closed; }
		
		@Override
		public void close() {
			closed = true;
			event.fire(this);
		}
		
		@Override
		public void addCloseListener(Listener<CloseableListenable<TError>> listener) {
			event.addListener(listener);
		}

		@Override
		public void addCloseListener(Runnable listener) {
			event.addListener(listener);
		}
		
		@Override
		public void removeCloseListener(Listener<CloseableListenable<TError>> listener) {
			event.removeListener(listener);
		}

		@Override
		public void removeCloseListener(Runnable listener) {
			event.removeListener(listener);
		}
	}
	
}
