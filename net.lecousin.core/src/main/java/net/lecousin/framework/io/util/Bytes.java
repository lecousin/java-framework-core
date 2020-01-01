package net.lecousin.framework.io.util;

import java.nio.ByteBuffer;

/** Bytes array. */
public interface Bytes {

	/** Return the number of remaining bytes. */
	int remaining();
	
	/** Return true if at least one byte is remaining. */
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
	
	/** Convert this Bytes into a ByteBuffer. */
	ByteBuffer toByteBuffer();
	
	/** Readable bytes. */
	interface Readable extends Bytes {
		
		/** Return the next byte. */
		byte get();
		
		/** Read <code>length</code> bytes into <code>buffer</code> starting at <code>offset</code>. */
		void get(byte[] buffer, int offset, int length);
		
		/** Return the byte at offset from current position, without changing current position. */
		byte getForward(int offset);
		
	}
	
	/** Writable bytes. */
	interface Writable extends Bytes {
		
		/** Set the next byte. */
		void put(byte b);
		
		/** Write <code>length</code> bytes from <code>buffer</code> starting at <code>offset</code>. */
		void put(byte[] buffer, int offset, int length);
		
	}
	
}
