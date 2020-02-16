package net.lecousin.framework.io.data;

import java.nio.CharBuffer;

import net.lecousin.framework.memory.CharArrayCache;
import net.lecousin.framework.text.IString;

/** Character array. */
public class CharArray extends DataArray<char[]> implements Chars.Readable {

	/** Create a ByteArray from a ByteBuffer. The returned ByteArray is Writable if the given ByteArray is not read-only. */
	public static CharArray fromCharBuffer(CharBuffer b) {
		if (b.hasArray()) {
			CharArray ba;
			if (b.isReadOnly())
				ba = new CharArray(b.array(), b.arrayOffset(), b.arrayOffset() + b.position() + b.remaining());
			else
				ba = new CharArray.Writable(b.array(), b.arrayOffset(), b.arrayOffset() + b.position() + b.remaining(), true);
			ba.currentOffset = b.arrayOffset() + b.position();
			return ba;
		}
		CharArray.Writable ba = new CharArray.Writable(CharArrayCache.getInstance().get(b.remaining(), true), true);
		ba.length = b.remaining();
		b.get(ba.array, 0, ba.length);
		b.position(b.position() - ba.length);
		return ba;
	}

	/** Constructor. */
	public CharArray(char[] buffer, int offset, int length) {
		super(buffer, offset, length);
	}
	
	/** Constructor. */
	public CharArray(char[] buffer) {
		super(buffer, 0, buffer.length);
	}
	
	/** Constructor. */
	public CharArray(CharArray copy) {
		super(copy);
	}
	
	/** Create a copy of this buffer. */
	public CharArray duplicate() {
		return new CharArray(this);
	}

	@Override
	public char get() {
		return array[currentOffset++];
	}
	
	@Override
	public void get(char[] buffer, int offset, int length) {
		System.arraycopy(array, currentOffset, buffer, offset, length);
		currentOffset += length;
	}
	
	@Override
	public void get(IString string, int length) {
		string.append(array, currentOffset, length);
		currentOffset += length;
	}
	
	@Override
	public char getForward(int offset) {
		return array[currentOffset + offset];
	}

	/** Create a CharBuffer from this buffer. */
	@Override
	public CharBuffer toCharBuffer() {
		return CharBuffer.wrap(array, currentOffset, length - (currentOffset - arrayOffset)).asReadOnlyBuffer();
	}
	
	/** Used when a CharBuffer as been converted into a RawCharBuffer, and we want to update the CharBuffer's position
	 * to the position of this buffer.
	 */
	public void setPosition(CharBuffer originalBuffer) {
		originalBuffer.position(originalBuffer.limit() - remaining());
	}

	@Override
	public CharArray subBuffer(int startPosition, int length) {
		return new CharArray(array, arrayOffset + startPosition, length);
	}
	
	@Override
	public void free() {
		// nothing if read-only
	}
	
	/** Writable char array. */
	public static class Writable extends CharArray implements DataBuffer.Writable, Chars.Writable {
		
		private boolean free;
		
		/** Constructor. */
		public Writable(char[] buffer, int offset, int length, boolean free) {
			super(buffer, offset, length);
			this.free = free;
		}
		
		/** Constructor. */
		public Writable(char[] buffer, boolean free) {
			super(buffer, 0, buffer.length);
			this.free = free;
		}
		
		/** Constructor. */
		public Writable(CharArray copy) {
			super(copy);
			this.free = false;
		}
		
		/** Create a copy of this buffer. */
		@Override
		public CharArray.Writable duplicate() {
			return new CharArray.Writable(this);
		}

		@Override
		public void put(char b) {
			array[currentOffset++] = b;
		}
		
		@Override
		public void put(char[] buffer, int offset, int length) {
			System.arraycopy(buffer, offset, array, currentOffset, length);
			currentOffset += length;
		}

		/** Create a CharBuffer from this buffer. */
		@Override
		public CharBuffer toCharBuffer() {
			return CharBuffer.wrap(array, currentOffset, length - (currentOffset - arrayOffset));
		}
		
		@Override
		public CharArray.Writable subBuffer(int startPosition, int length) {
			return new CharArray.Writable(array, arrayOffset + startPosition, length, false);
		}

		@Override
		public void free() {
			if (!free)
				return;
			CharArrayCache.getInstance().free(array);
			array = null;
		}
	}

}
