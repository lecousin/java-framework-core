package net.lecousin.framework.event;

/**
 * A SimpleListenable is an object on which we can add and remove listeners which are simple Runnable.
 */
public interface SimpleListenable {

	/** Add a listener. */
	void addListener(Runnable listener);
	
	/** Remove a listener. */
	void removeListener(Runnable listener);
	
	/** Return true if at least one listener is present. */
	boolean hasListeners();
	
	default void listen(Runnable listener) { addListener(listener); }
	
	default void unlisten(Runnable listener) { removeListener(listener); }
	
}
