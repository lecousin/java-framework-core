package net.lecousin.framework.io.data;

/** Buffer of bits. */
public interface BitsBuffer {
	
	/** Return true if some bits are remaining on the byte. */
	boolean hasRemaining();
	
	/** Return the number of remaining bits. */
	int remaining();

	/** Readable buffer of bits. */
	interface Readable extends BitsBuffer {
		
		/** Get the next bit. */
		boolean get();

		/** Move forward to the next byte alignment.
		 * If already aligned, nothing is done.
		 */
		void alignToNextByte();
		
	}
	
	/** Writable buffer of bits. */
	interface Writable extends BitsBuffer {
		
		/** Write a bit. */
		void put(boolean bit);

		/** Move forward to the next byte alignment by filling with the given bit.
		 * If already aligned, nothing is done.
		 */
		void alignToNextByte(boolean fillBit);
		
	}
	
	/** Little-endian bits buffer. */
	interface LittleEndian extends BitsBuffer {}
	
	/** Big-endian bits buffer. */
	interface BigEndian extends BitsBuffer {}
	
}
