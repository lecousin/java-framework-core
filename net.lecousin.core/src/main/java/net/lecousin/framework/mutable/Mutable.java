package net.lecousin.framework.mutable;

/**
 * Mutable object (with getter and setter).
 * @param <T> type of object
 */
public class Mutable<T> {

	/** Constructor with initial value. */
	public Mutable(T value) {
		this.value = value;
	}
	
	protected T value;
	
	public T get() { return value; }
	
	public void set(T value) { this.value = value; }
	
}
