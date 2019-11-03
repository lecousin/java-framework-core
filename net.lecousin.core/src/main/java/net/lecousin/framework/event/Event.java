package net.lecousin.framework.event;

import java.util.ArrayList;
import java.util.function.Consumer;

import net.lecousin.framework.application.LCCore;

/**
 * An event allows to call listeners that previously registered themselves when the event is fired.
 * @param <T> type of data fired with the event
 */
public class Event<T> extends AbstractListenable<T> {

	/** Fire the event (call the listeners). */
	public void fire(T event) {
		ArrayList<Consumer<T>> list1;
		ArrayList<Runnable> list2;
		synchronized (this) {
			if (listenersConsumer == null) list1 = null;
			else list1 = new ArrayList<>(listenersConsumer);
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
