package net.lecousin.framework.event;

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
	public void addListener(Listener<Long> listener) {
		event.addListener(listener);
	}
	
	@Override
	public void addListener(Runnable listener) {
		event.addListener(listener);
	}
	
	@Override
	public void removeListener(Listener<Long> listener) {
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
