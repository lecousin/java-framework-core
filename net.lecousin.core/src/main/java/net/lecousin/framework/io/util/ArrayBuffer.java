package net.lecousin.framework.io.util;

/** Base interface for array buffers. */
public interface ArrayBuffer {
	
	/** Return the number of remaining values. */
	int remaining();
	
	/** Return true if at least one value is remaining. */
	default boolean hasRemaining() {
		return remaining() > 0;
	}
	
	/** Return the current position (currentOffset - arrayOffset). */
	int position();
	
	/** Set the position. */
	void setPosition(int position);
	
	/** Move the position forward. */
	default void moveForward(int offset) {
		setPosition(position() + offset);
	}
	
	/** Set the position to the end. */
	default void goToEnd() {
		setPosition(position() + remaining());
	}
}
