package net.lecousin.framework.event;

/**
 * Object that can be listened.
 * @param <T> type of data given to listeners.
 */
public interface Listenable<T> {

	/** Add a listener. */
	public void addListener(Listener<T> listener);
	
	/** Remove a listener. */
	public void removeListener(Listener<T> listener);
	
	/** Return true if at least one listener is present. */
	public boolean hasListeners();
	
	public default void listen(Listener<T> listener) { addListener(listener); }
	
	public default void unlisten(Listener<T> listener) { removeListener(listener); }
	
}
