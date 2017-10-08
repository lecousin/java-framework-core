package net.lecousin.framework.util;

import net.lecousin.framework.event.Event;
import net.lecousin.framework.event.Listener;

/**
 * Closeable resource, with the possibility to add and remove listeners to be called when the resource is closed.
 */
public interface CloseableListenable {

	/** Close. */
	public void close();
	
	/** Return true if closed. */
	public boolean isClosed();
	
	/** Add a listener to be called once this resource is closed. */
	public void addCloseListener(Listener<CloseableListenable> listener);

	/** Remove a listener. */
	public void removeCloseListener(Listener<CloseableListenable> listener);
	
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
		public void addCloseListener(Listener<CloseableListenable> listener) {
			event.addListener(listener);
		}
		
		@Override
		public void removeCloseListener(Listener<CloseableListenable> listener) {
			event.removeListener(listener);
		}
	}
	
}
