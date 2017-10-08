package net.lecousin.framework.mutable;

/** Mutable boolean. */
public class MutableBoolean {

	/** Constructor. */
	public MutableBoolean(boolean value) {
		this.value = value;
	}
	
	private boolean value;
	
	public boolean get() { return value; }
	
	public void set(boolean value) { this.value = value; }
	
}
