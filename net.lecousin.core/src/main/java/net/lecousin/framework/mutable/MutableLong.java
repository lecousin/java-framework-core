package net.lecousin.framework.mutable;

/** Mutable long. */
public class MutableLong {

	/** Constructor. */
	public MutableLong(long value) {
		this.value = value;
	}
	
	private long value;
	
	public long get() { return value; }
	
	public void set(long value) { this.value = value; }
	
	/** Increment the value and return the value after incrementation. */
	public long inc() { return ++value; }
	
	/** Decrement the value and return the value after decrementation. */
	public long dec() { return --value; }

	/** Add the given value and return the value after the addition. */
	public long add(long add) { return value += add; }
	
	/** Subtract the given value and return the value after the subtraction. */
	public long sub(long sub) { return value -= sub; }
	
}
