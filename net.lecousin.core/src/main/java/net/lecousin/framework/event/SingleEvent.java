package net.lecousin.framework.event;

import java.util.ArrayList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A SingleEvent is an event that can occur only once.
 * If a listener is added when the event already occured, the listener is fired immediately.
 * @param <T> type of data given to listeners
 */
public class SingleEvent<T> implements Listenable<T> {

	private boolean occured = false;
	private T data = null;
	private ArrayList<Listener<T>> listeners = null;
	private ArrayList<Runnable> listenersRunnable = null;
	
	@Override
	public synchronized void addListener(Listener<T> listener) {
		if (occured) listener.fire(data);
		else {
			if (listeners == null) listeners = new ArrayList<>();
			listeners.add(listener);
		}
	}
	
	@Override
	public synchronized void addListener(Runnable listener) {
		if (occured) listener.run();
		else {
			if (listenersRunnable == null) listenersRunnable = new ArrayList<>();
			listenersRunnable.add(listener);
		}
	}
	
	@Override
	public synchronized void removeListener(Listener<T> listener) {
		if (listeners == null) return;
		listeners.remove(listener);
		if (listeners.isEmpty()) listeners = null;
	}
	
	@Override
	public synchronized void removeListener(Runnable listener) {
		if (listenersRunnable == null) return;
		listenersRunnable.remove(listener);
		if (listenersRunnable.isEmpty()) listenersRunnable = null;
	}
	
	@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
	@Override
	public boolean hasListeners() {
		return listeners != null || listenersRunnable != null;
	}
	
	/** Fire the event.
	 * @throws IllegalStateException if the event has been already fired.
	 */
	public synchronized void fire(T event) {
		if (occured) throw new IllegalStateException("SingleEvent already fired");
		occured = true;
		data = event;
		if (listeners != null) {
			for (Listener<T> listener : listeners)
				listener.fire(event);
			listeners = null;
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
