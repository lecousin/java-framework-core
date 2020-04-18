package net.lecousin.framework.event;

import java.util.Objects;
import java.util.function.Consumer;

import net.lecousin.framework.mutable.Mutable;

/**
 * Property that calls listeners when modified.
 * It extends Mutable to hold the object of this property, and implements Listenable to add/remove lsiteners.
 * If the set method is called with the same object, listeners are not called.
 * Listeners are called with the PREVIOUS value, to get the current value the get method should be used.
 * @param <T> type of data for this property
 */
public class ListenableProperty<T> extends Mutable<T> implements Listenable<T> {

	/** Constructor with initial object to hold. */
	public ListenableProperty(T value) {
		super(value);
	}
	
	protected Event<T> event = new Event<>();
	
	@Override
	public void set(T value) {
		T previous = get();
		if (Objects.equals(previous, value)) return;
		super.set(value);
		event.fire(previous);
	}
	
	@Override
	public void addListener(Consumer<T> listener) {
		event.addListener(listener);
	}
	
	@Override
	public void addListener(Runnable listener) {
		event.addListener(listener);
	}
	
	@Override
	public void removeListener(Consumer<T> listener) {
		event.removeListener(listener);
	}
	
	@Override
	public void removeListener(Runnable listener) {
		event.removeListener(listener);
	}
	
	@Override
	public boolean hasListeners() {
		return event.hasListeners();
	}
	
}
