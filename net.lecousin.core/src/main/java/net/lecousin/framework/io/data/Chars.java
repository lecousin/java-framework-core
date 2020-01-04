package net.lecousin.framework.io.data;

import java.nio.CharBuffer;

/** Bytes array. */
public interface Chars {

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
	
	/** Convert this Chars into a CharBuffer. */
	CharBuffer toCharBuffer();

	/** Readable bytes. */
	interface Readable extends Chars {
		
		/** Return the next char. */
		char get();
		
		/** Read <code>length</code> chars into <code>buffer</code> starting at <code>offset</code>. */
		void get(char[] buffer, int offset, int length);
		
		/** Return the char at offset from current position, without changing current position. */
		char getForward(int offset);
		
	}
	
	/** Writable bytes. */
	interface Writable extends Chars {
		
		/** Set the next char. */
		void put(char b);
		
		/** Write <code>length</code> chars from <code>buffer</code> starting at <code>offset</code>. */
		void put(char[] buffer, int offset, int length);
		
	}
	
}
