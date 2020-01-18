package net.lecousin.framework.io.data;

import java.nio.CharBuffer;

import net.lecousin.framework.text.IString;

/** Bytes array. */
public interface Chars extends DataBuffer {

	/** Convert this Chars into a CharBuffer. */
	CharBuffer toCharBuffer();

	/** Readable bytes. */
	interface Readable extends Chars {
		
		/** Return the next char. */
		char get();
		
		/** Read <code>length</code> chars into <code>buffer</code> starting at <code>offset</code>. */
		void get(char[] buffer, int offset, int length);
		
		/** Read <code>length</code> chars into <code>string</code>. */
		void get(IString string, int length);
		
		/** Return the char at offset from current position, without changing current position. */
		char getForward(int offset);
		
		@Override
		Chars.Readable subBuffer(int startPosition, int length);
	}
	
	/** Writable bytes. */
	interface Writable extends Chars {
		
		/** Set the next char. */
		void put(char b);
		
		/** Write <code>length</code> chars from <code>buffer</code> starting at <code>offset</code>. */
		void put(char[] buffer, int offset, int length);
		
		@Override
		Chars.Writable subBuffer(int startPosition, int length);
	}
	
}
