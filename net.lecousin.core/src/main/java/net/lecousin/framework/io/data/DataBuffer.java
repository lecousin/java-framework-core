package net.lecousin.framework.io.data;

/** Base interface for data buffers. */
public interface DataBuffer {
	
	/** Return the length of this buffer, which is equivalent to remaining() when position is set to 0. */
	int length();
	
	/** Return the current position (currentOffset - arrayOffset). */
	int position();
	
	/** Set the position. */
	void setPosition(int position);
	
	/** Return the number of remaining values. */
	int remaining();
	
	/** Return true if at least one value is remaining. */
	default boolean hasRemaining() {
		return remaining() > 0;
	}
	
	/** Move the position forward. */
	default void moveForward(int offset) {
		setPosition(position() + offset);
	}
	
	/** Set the position to the end. */
	default void goToEnd() {
		setPosition(position() + remaining());
	}
	
	/** Return a new buffer wrapping this buffer starting from the given position. */
	DataBuffer subBuffer(int startPosition, int length);
}
