package net.lecousin.framework.event;

import java.util.ArrayList;

/**
 * An event allows to call listeners that previously registered themselves when the event is fired.
 * @param <T> type of data fired with the event
 */
public class Event<T> implements Listenable<T> {

	private ArrayList<Listener<T>> listeners = null;

	@Override
	public synchronized void addListener(Listener<T> listener) {
		if (listeners == null) listeners = new ArrayList<>(5);
		listeners.add(listener);
	}

	@Override
	public synchronized void removeListener(Listener<T> listener) {
		if (listeners == null) return;
		listeners.remove(listener);
		if (listeners.isEmpty()) listeners = null;
	}
	
	@Override
	public boolean hasListeners() { return listeners != null; }
	
	/** Fire the event (call the listeners). */
	public void fire(T event) {
		ArrayList<Listener<T>> listeners;
		synchronized (this) {
			if (this.listeners == null) return;
			listeners = new ArrayList<>(this.listeners);
		}
		for (int i = 0; i < listeners.size(); ++i)
			listeners.get(i).fire(event);
	}
	
	/** Bridge between 2 events: create a listener that will fire the given event when called. */
	public static <T> Listener<T> createListenerToFire(Event<T> event) {
		return new Listener<T>() {
			@Override
			public void fire(T obj) {
				event.fire(obj);
			}
		};
	}
	
}
