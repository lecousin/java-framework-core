package net.lecousin.framework.event;

import java.util.ArrayList;

/**
 * A SimpleEvent is like an Event but does not provide data to describe the event.
 * Consequently, listeners are just Runnable.
 */
public class SimpleEvent implements SimpleListenable {

	private ArrayList<Runnable> listeners = null;
	
	@Override
	public synchronized void addListener(Runnable listener) {
		if (listeners == null) listeners = new ArrayList<>(5);
		listeners.add(listener);
	}

	@Override
	public synchronized void removeListener(Runnable listener) {
		if (listeners == null) return;
		listeners.remove(listener);
		if (listeners.isEmpty()) listeners = null;
	}
	
	@Override
	public boolean hasListeners() { return listeners != null; }
	
	/** Fire this event, or in other words call the listeners. */
	public void fire() {
		ArrayList<Runnable> listeners;
		synchronized (this) {
			if (this.listeners == null) return;
			listeners = new ArrayList<>(this.listeners);
		}
		for (int i = 0; i < listeners.size(); ++i)
			listeners.get(i).run();
	}
	
}
