package net.lecousin.framework.event;

import java.util.ArrayList;
import java.util.function.Consumer;

import net.lecousin.framework.application.LCCore;

/**
 * An event allows to call listeners that previously registered themselves when the event is fired.
 * @param <T> type of data fired with the event
 */
public class Event<T> implements Listenable<T> {

	private ArrayList<Consumer<T>> listeners = null;
	private ArrayList<Runnable> listenersRunnable = null;

	@Override
	public synchronized void addListener(Consumer<T> listener) {
		if (listeners == null) listeners = new ArrayList<>(5);
		listeners.add(listener);
	}

	@Override
	public synchronized void addListener(Runnable listener) {
		if (listenersRunnable == null) listenersRunnable = new ArrayList<>(5);
		listenersRunnable.add(listener);
	}
	
	@Override
	public synchronized void removeListener(Consumer<T> listener) {
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
	
	@Override
	public boolean hasListeners() { return listeners != null || listenersRunnable != null; }
	
	/** Fire the event (call the listeners). */
	public void fire(T event) {
		ArrayList<Consumer<T>> list1;
		ArrayList<Runnable> list2;
		synchronized (this) {
			if (listeners == null) list1 = null;
			else list1 = new ArrayList<>(listeners);
			if (listenersRunnable == null) list2 = null;
			else list2 = new ArrayList<>(listenersRunnable);
		}
		if (list1 != null)
			for (int i = 0; i < list1.size(); ++i)
				try { list1.get(i).accept(event); }
				catch (Exception t) {
					LCCore.getApplication().getDefaultLogger().error("Event listener error: " + list1.get(i), t);
				}
		if (list2 != null)
			for (int i = 0; i < list2.size(); ++i)
				try { list2.get(i).run(); }
				catch (Exception t) {
					LCCore.getApplication().getDefaultLogger().error("Event listener error: " + list2.get(i), t);
				}
	}
	
	/** Bridge between 2 events: create a listener that will fire the given event when called. */
	public static <T> Consumer<T> createListenerToFire(Event<T> event) {
		return event::fire;
	}
	
}
