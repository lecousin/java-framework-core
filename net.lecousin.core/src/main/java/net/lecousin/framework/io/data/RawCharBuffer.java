package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import net.lecousin.framework.text.IString;

/** Utility class that contains a byte array with public attributes. */
public class RawCharBuffer extends RawBuffer<char[]> implements Chars.Readable, Chars.Writable {

	/** Constructor. */
	public RawCharBuffer(char[] buffer, int offset, int length) {
		super(buffer, offset, length);
	}
	
	/** Constructor. */
	public RawCharBuffer(char[] buffer) {
		super(buffer, 0, buffer.length);
	}
	
	/** Constructor. */
	public RawCharBuffer(RawCharBuffer copy) {
		super(copy);
	}
	
	/** Constructor. */
	public RawCharBuffer(CharBuffer b) {
		if (b.hasArray()) {
			array = b.array();
			arrayOffset = b.arrayOffset();
			currentOffset = arrayOffset + b.position();
			length = currentOffset + b.remaining();
		} else {
			array = new char[b.remaining()];
			arrayOffset = currentOffset = 0;
			length = array.length;
			b.get(array);
			b.position(b.position() - length);
		}
	}
	
	/** Create a copy of this buffer. */
	public RawCharBuffer duplicate() {
		return new RawCharBuffer(this);
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
	
	/** Used when a CharBuffer as been converted into a RawCharBuffer, and we want to update the CharBuffer's position
	 * to the position of this buffer.
	 */
	public void setPosition(CharBuffer originalBuffer) {
		originalBuffer.position(currentOffset - arrayOffset);
	}

	@Override
	public RawCharBuffer subBuffer(int startPosition, int length) {
		return new RawCharBuffer(array, arrayOffset + startPosition, length);
	}
	
	/** Wraps a RawCharBuffer to convert it into a Bytes considering characters are only ISO-8859 characters. */
	public abstract class Iso8859Buffer implements DataBuffer {
		
		@Override
		public void setPosition(int position) {
			RawCharBuffer.this.setPosition(position);
		}
		
		@Override
		public int length() {
			return RawCharBuffer.this.length();
		}

		@Override
		public int remaining() {
			return RawCharBuffer.this.remaining();
		}
		
		@Override
		public boolean hasRemaining() {
			return RawCharBuffer.this.hasRemaining();
		}
		
		@Override
		public int position() {
			return RawCharBuffer.this.position();
		}
		
		/** Return the original RawCharBuffer from which this Iso8859Buffer has been created. */
		public RawCharBuffer getOriginalBuffer() {
			return RawCharBuffer.this;
		}
	}

	private class Iso8859BufferReadable extends Iso8859Buffer implements Bytes.Readable {
		
		@Override
		public byte getForward(int offset) {
			return (byte)RawCharBuffer.this.getForward(offset);
		}
		
		@Override
		public void get(byte[] buffer, int offset, int length) {
			for (int i = 0; i < length; ++i)
				buffer[offset + i] = get();
		}
		
		@Override
		public byte get() {
			return (byte)RawCharBuffer.this.get();
		}
		
		@Override
		public ByteBuffer toByteBuffer() {
			byte[] bytes = new byte[remaining()];
			get(bytes, 0, bytes.length);
			return ByteBuffer.wrap(bytes);
		}
		
		@Override
		public Bytes.Readable subBuffer(int startPosition, int length) {
			return RawCharBuffer.this.subBuffer(startPosition, length).iso8859AsReadableBytes();
		}

	}
	
	private class Iso8859BufferWritable extends Iso8859Buffer implements Bytes.Writable {
		
		@Override
		public void put(byte b) {
			RawCharBuffer.this.put((char)(b & 0xFF));
		}
		
		@Override
		public void put(byte[] buffer, int offset, int length) {
			for (int i = 0; i < length; ++i)
				RawCharBuffer.this.put((char)(buffer[offset + i] & 0xFF));
		}
		
		@Override
		public ByteBuffer toByteBuffer() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Bytes.Writable subBuffer(int startPosition, int length) {
			return RawCharBuffer.this.subBuffer(startPosition, length).asWritableIso8859Bytes();
		}

	}
	
	/** Create a Readable Bytes from this character array, considering there is only ascii characters. */
	public Bytes.Readable iso8859AsReadableBytes() {
		return new Iso8859BufferReadable();
	}
	
	/** Create a Readable Bytes from this character array, considering there is only ascii characters. */
	public Bytes.Writable asWritableIso8859Bytes() {
		return new Iso8859BufferWritable();
	}

}
