package net.lecousin.framework.event;

import java.util.ArrayList;
import java.util.function.Consumer;

/** Abstract class implementing Listenable, containing 2 lists of listeners.
 * @param <T> type of event
 */
public abstract class AbstractListenable<T> extends AbstractSimpleListenable implements Listenable<T> {

	protected ArrayList<Consumer<T>> listenersConsumer = null;

	@Override
	public synchronized void addListener(Consumer<T> listener) {
		if (listenersConsumer == null) listenersConsumer = new ArrayList<>(5);
		listenersConsumer.add(listener);
	}
	
	@Override
	public synchronized void removeListener(Consumer<T> listener) {
		if (listenersConsumer == null) return;
		listenersConsumer.remove(listener);
		if (listenersConsumer.isEmpty()) listenersConsumer = null;
	}
	
	@Override
	public boolean hasListeners() { return listenersConsumer != null || listenersRunnable != null; }
	
}
