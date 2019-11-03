package net.lecousin.framework.event;

import java.util.function.Consumer;

/**
 * Object that can be listened.
 * @param <T> type of data given to listeners.
 */
public interface Listenable<T> extends SimpleListenable {

	/** Add a listener. */
	void addListener(Consumer<T> listener);

	/** Remove a listener. */
	void removeListener(Consumer<T> listener);

	default void listen(Consumer<T> listener) { addListener(listener); }

	default void unlisten(Consumer<T> listener) { removeListener(listener); }
	
}
