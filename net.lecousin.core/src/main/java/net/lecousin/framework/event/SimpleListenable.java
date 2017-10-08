package net.lecousin.framework.event;

/**
 * A SimpleListenable is an object on which we can add and remove listeners which are simple Runnable.
 */
public interface SimpleListenable {

	/** Add a listener. */
	public void addListener(Runnable listener);
	
	/** Remove a listener. */
	public void removeListener(Runnable listener);
	
	/** Return true if at least one listener is present. */
	public boolean hasListeners();
	
	public default void listen(Runnable listener) { addListener(listener); }
	
	public default void unlisten(Runnable listener) { removeListener(listener); }
	
}
