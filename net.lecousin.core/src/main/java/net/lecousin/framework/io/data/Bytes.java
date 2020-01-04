package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;

/** Bytes array. */
public interface Bytes extends ArrayBuffer {
	
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
