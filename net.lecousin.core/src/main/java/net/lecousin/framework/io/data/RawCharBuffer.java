package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import net.lecousin.framework.text.IString;

/** Utility class that contains a byte array with public attributes. */
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
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
		CharBuffer b = CharBuffer.wrap(array, arrayOffset, length);
		b.position(currentOffset - arrayOffset);
		return b;
	}
	
	@Override
	public RawCharBuffer subBuffer(int startPosition, int length) {
		return new RawCharBuffer(array, arrayOffset + startPosition, length);
	}
	
	private abstract class Iso8859Buffer implements DataBuffer {
		
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
		
	}

	private class Iso8859BufferReadable extends Iso8859Buffer implements Bytes.Readable {
		
		@Override
		public byte getForward(int offset) {
			return (byte)RawCharBuffer.this.getForward(offset);
		}
		
		@Override
		public void get(byte[] buffer, int offset, int length) {
			for (int i = 0; i < length; ++i)
				buffer[i] = get();
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
