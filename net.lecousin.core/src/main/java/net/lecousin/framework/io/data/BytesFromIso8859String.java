package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;

/** Bytes Readable implementation wrapping a String that must contain only ISO-8859 characters. */
public class BytesFromIso8859String extends AbstractDataBufferFromString implements Bytes.Readable {

	/** Constructor. */
	public BytesFromIso8859String(String str) {
		super(str);
	}
	
	/** Constructor. */
	public BytesFromIso8859String(String str, int offset, int length) {
		super(str, offset, length);
	}
	
	@Override
	public byte get() {
		return (byte)str.charAt(offset + pos++);
	}

	@Override
	public void get(byte[] buffer, int offset, int length) {
		for (int i = 0; i < length; ++i)
			buffer[offset + i] = (byte)str.charAt(this.offset + pos++);
	}

	@Override
	public byte getForward(int offset) {
		return (byte)str.charAt(this.offset + pos + offset);
	}

	@Override
	public BytesFromIso8859String subBuffer(int startPosition, int length) {
		return new BytesFromIso8859String(str, offset + startPosition, length);
	}
	
	@Override
	public ByteBuffer toByteBuffer() {
		ByteBuffer b = ByteBuffer.allocate(length);
		byte[] buffer = b.array();
		for (int i = 0; i < length; ++i)
			buffer[i] = (byte)str.charAt(this.offset + i);
		b.position(pos);
		return b;
	}
	
	@Override
	public void free() {
		// nothing to free
	}
}
