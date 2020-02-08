package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;

/** Bytes Readable implementation wrapping a String that must contain only ISO-8859 characters. */
public class StringAsIso8859ReadableBytes implements Bytes.Readable {

	/** Constructor. */
	public StringAsIso8859ReadableBytes(String str) {
		this(str, 0, str.length());
	}
	
	/** Constructor. */
	public StringAsIso8859ReadableBytes(String str, int offset, int length) {
		this.str = str;
		this.offset = offset;
		this.length = length;
	}
	
	private String str;
	private int offset;
	private int length;
	private int pos = 0;

	@Override
	public int length() {
		return length;
	}

	@Override
	public int position() {
		return pos;
	}

	@Override
	public void setPosition(int position) {
		pos = position;
	}

	@Override
	public int remaining() {
		return length - pos;
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
	public StringAsIso8859ReadableBytes subBuffer(int startPosition, int length) {
		return new StringAsIso8859ReadableBytes(str, offset + startPosition, length);
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
}
