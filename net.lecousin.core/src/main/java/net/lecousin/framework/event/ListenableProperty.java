package net.lecousin.framework.event;

import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.util.ObjectUtil;

/**
 * Property that calls listeners when modified.
 * It extends Mutable to hold the object of this property, and implements Listenable to add/remove lsiteners.
 * If the set method is called with the same object, listeners are not called.
 * @param <T> type of data for this property
 */
public class ListenableProperty<T> extends Mutable<T> implements Listenable<T> {

	/** Constructor with initial object to hold. */
	public ListenableProperty(T value) {
		super(value);
	}
	
	public Event<T> event = new Event<>();
	
	@Override
	public void set(T value) {
		T previous = get();
		if (ObjectUtil.equalsOrNull(previous, value)) return;
		super.set(value);
		event.fire(previous);
	}
	
	@Override
	public void addListener(Listener<T> listener) {
		event.addListener(listener);
	}
	
	@Override
	public void removeListener(Listener<T> listener) {
		event.removeListener(listener);
	}
	
	@Override
	public boolean hasListeners() {
		return event.hasListeners();
	}
	
}
