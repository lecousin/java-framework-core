package net.lecousin.framework.event;

import java.util.function.Consumer;

/**
 * Object that can be listened.
 * @param <T> type of data given to listeners.
 */
public interface Listenable<T> {

	/** Add a listener. */
	void addListener(Consumer<T> listener);

	/** Add a listener. */
	void addListener(Runnable listener);
	
	/** Remove a listener. */
	void removeListener(Consumer<T> listener);

	/** Remove a listener. */
	void removeListener(Runnable listener);
	
	/** Return true if at least one listener is present. */
	boolean hasListeners();
	
	default void listen(Consumer<T> listener) { addListener(listener); }

	default void listen(Runnable listener) { addListener(listener); }
	
	default void unlisten(Consumer<T> listener) { removeListener(listener); }

	default void unlisten(Runnable listener) { removeListener(listener); }
	
}
