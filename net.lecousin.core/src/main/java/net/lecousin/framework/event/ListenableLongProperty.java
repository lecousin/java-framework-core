package net.lecousin.framework.event;

import java.util.function.Consumer;

/**
 * Property that calls listeners when modified.
 * It extends Mutable to hold the object of this property, and implements Listenable to add/remove lsiteners.
 * If the set method is called with the same object, listeners are not called.
 * Listeners are called with the PREVIOUS value, to get the current value the get method should be used.
 */
public class ListenableLongProperty implements Listenable<Long> {

	/** Constructor with initial object to hold. */
	public ListenableLongProperty(long value) {
		this.value = value;
	}
	
	protected long value;
	protected Event<Long> event = new Event<>();
	
	/** Return the current value. */
	public long get() {
		return value;
	}

	/** Set a new value. */
	public void set(long value) {
		if (this.value == value) return;
		long previous = this.value;
		this.value = value;
		event.fire(Long.valueOf(previous));
	}
	
	/** Addition. */
	public void add(long add) {
		set(value + add);
	}

	/** Subtraction. */
	public void sub(long sub) {
		set(value - sub);
	}
	
	/** Increment. */
	public void inc() {
		set(value + 1);
	}
	
	/** Decrement. */
	public void dec() {
		set(value - 1);
	}
	
	@Override
	@SuppressWarnings("squid:S4276") // cannot use LongConsumer because we inherit this method
	public void addListener(Consumer<Long> listener) {
		event.addListener(listener);
	}
	
	@Override
	public void addListener(Runnable listener) {
		event.addListener(listener);
	}
	
	@Override
	@SuppressWarnings("squid:S4276") // cannot use LongConsumer because we inherit this method
	public void removeListener(Consumer<Long> listener) {
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
