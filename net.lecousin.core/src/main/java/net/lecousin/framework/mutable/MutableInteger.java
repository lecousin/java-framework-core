package net.lecousin.framework.mutable;

/** Mutable integer. */
public class MutableInteger {

	/** Constructor. */
	public MutableInteger(int value) {
		this.value = value;
	}
	
	private int value;
	
	public int get() { return value; }
	
	public void set(int value) { this.value = value; }
	
	/** Increment the value and return the value after incrementation. */
	public int inc() { return ++value; }
	
	/** Decrement the value and return the value after decrementation. */
	public int dec() { return --value; }

	/** Add the given value and return the value after the addition. */
	@SuppressWarnings("squid:AssignmentInSubExpressionCheck")
	public int add(int add) { return value += add; }
	
	/** Subtract the given value and return the value after the subtraction. */
	@SuppressWarnings("squid:AssignmentInSubExpressionCheck")
	public int sub(int add) { return value -= add; }
	
}
