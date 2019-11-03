package net.lecousin.framework.event;

import java.util.function.Consumer;

/**
 * A SingleEvent is an event that can occur only once.
 * If a listener is added when the event already occured, the listener is fired immediately.
 * @param <T> type of data given to listeners
 */
public class SingleEvent<T> extends AbstractListenable<T> {

	private boolean occured = false;
	private T data = null;
	
	@Override
	public synchronized void addListener(Consumer<T> listener) {
		if (occured) listener.accept(data);
		else super.addListener(listener);
	}
	
	@Override
	public synchronized void addListener(Runnable listener) {
		if (occured) listener.run();
		else super.addListener(listener);
	}
	
	/** Fire the event.
	 * @throws IllegalStateException if the event has been already fired.
	 */
	public synchronized void fire(T event) {
		if (occured) throw new IllegalStateException("SingleEvent already fired");
		occured = true;
		data = event;
		if (listenersConsumer != null) {
			for (Consumer<T> listener : listenersConsumer)
				listener.accept(event);
			listenersConsumer = null;
		}
		if (listenersRunnable != null) {
			for (Runnable listener : listenersRunnable)
				listener.run();
			listenersRunnable = null;
		}
	}
	
	/** Return true if the event has been already fired. */
	public boolean occured() { return occured; }
	
}
