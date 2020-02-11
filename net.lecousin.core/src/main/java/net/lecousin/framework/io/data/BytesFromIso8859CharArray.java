package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;

/** Readable bytes from readable CharArray of ISO-8859 characters. */
public class BytesFromIso8859CharArray implements Bytes.Readable {

	protected CharArray array;
	protected boolean freeCharArrayOnFree;
	
	/** Constructor. */
	public BytesFromIso8859CharArray(CharArray array, boolean freeCharArrayOnFree) {
		this.array = array;
		this.freeCharArrayOnFree = freeCharArrayOnFree;
	}
	
	@Override
	public void setPosition(int position) {
		array.setPosition(position);
	}
	
	@Override
	public int length() {
		return array.length();
	}

	@Override
	public int remaining() {
		return array.remaining();
	}
	
	@Override
	public boolean hasRemaining() {
		return array.hasRemaining();
	}
	
	@Override
	public int position() {
		return array.position();
	}
	
	/** Return the original CharArray from which this object has been created. */
	public CharArray getOriginalBuffer() {
		return array;
	}

	@Override
	public byte getForward(int offset) {
		return (byte)array.getForward(offset);
	}
	
	@Override
	public void get(byte[] buffer, int offset, int length) {
		for (int i = 0; i < length; ++i)
			buffer[offset + i] = get();
	}
	
	@Override
	public byte get() {
		return (byte)array.get();
	}
	
	@Override
	public ByteBuffer toByteBuffer() {
		byte[] bytes = new byte[remaining()];
		get(bytes, 0, bytes.length);
		return ByteBuffer.wrap(bytes);
	}
	
	@Override
	public BytesFromIso8859CharArray subBuffer(int startPosition, int length) {
		return new BytesFromIso8859CharArray(array.subBuffer(startPosition, length), false);
	}
	
	@Override
	public void free() {
		if (freeCharArrayOnFree)
			array.free();
	}

	/** Writable bytes from writable CharArray of ISO-8859 characters. */
	public static class Writable extends BytesFromIso8859CharArray implements Bytes.Writable {
		
		/** Constructor. */
		public Writable(CharArray.Writable array, boolean freeCharArrayOnFree) {
			super(array, freeCharArrayOnFree);
		}
		
		/** Return the original CharArray.Writable from which this object has been created. */
		@Override
		public CharArray.Writable getOriginalBuffer() {
			return (CharArray.Writable)array;
		}
		
		@Override
		public BytesFromIso8859CharArray.Writable subBuffer(int startPosition, int length) {
			return new BytesFromIso8859CharArray.Writable(((CharArray.Writable)array).subBuffer(startPosition, length), false);
		}

		@Override
		public void put(byte b) {
			((CharArray.Writable)array).put((char)(b & 0xFF));
		}
		
		@Override
		public void put(byte[] buffer, int offset, int length) {
			for (int i = 0; i < length; ++i)
				((CharArray.Writable)array).put((char)(buffer[offset + i] & 0xFF));
		}

		@Override
		public ByteBuffer toByteBuffer() {
			throw new UnsupportedOperationException();
		}
	}
	
}
