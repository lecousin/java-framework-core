package net.lecousin.framework.io.data;

import java.nio.CharBuffer;

import net.lecousin.framework.text.ByteArrayStringIso8859;
import net.lecousin.framework.text.IString;

/** Wrap a Bytes.Readable as a Chars.Readable considering all bytes are ISO-8859 chars. */
public class CharsFromIso8859Bytes implements Chars.Readable {

	/** Constructor. */
	public CharsFromIso8859Bytes(Bytes.Readable bytes, boolean free) {
		this.bytes = bytes;
		this.free = free;
	}
	
	protected Bytes.Readable bytes;
	protected boolean free;

	@Override
	public int length() {
		return bytes.length();
	}
	
	@Override
	public int remaining() {
		return bytes.remaining();
	}
	
	@Override
	public boolean hasRemaining() {
		return bytes.hasRemaining();
	}

	@Override
	public int position() {
		return bytes.position();
	}

	@Override
	public void setPosition(int position) {
		bytes.setPosition(position);
	}
	
	@Override
	public void moveForward(int offset) {
		bytes.moveForward(offset);
	}
	
	@Override
	public void goToEnd() {
		bytes.goToEnd();
	}

	@Override
	public char get() {
		return (char)(bytes.get() & 0xFF);
	}

	@Override
	public void get(char[] buffer, int offset, int length) {
		for (int i = 0; i < length; ++i)
			buffer[offset + i] = (char)(bytes.get() & 0xFF);
	}
	
	@Override
	public void get(IString string, int length) {
		byte[] b = new byte[length];
		bytes.get(b, 0, length);
		string.append(new ByteArrayStringIso8859(b, 0, length, length));
	}

	@Override
	public char getForward(int offset) {
		return (char)(bytes.getForward(offset) & 0xFF);
	}
	
	@Override
	public CharBuffer toCharBuffer() {
		int pos = bytes.position();
		int len = bytes.remaining();
		bytes.setPosition(0);
		char[] chars = new char[len];
		get(chars, 0, len);
		bytes.setPosition(pos);
		return CharBuffer.wrap(chars);
	}
	
	@Override
	public void free() {
		if (free)
			bytes.free();
	}
	
	@Override
	public CharsFromIso8859Bytes subBuffer(int startPosition, int length) {
		return new CharsFromIso8859Bytes(bytes.subBuffer(startPosition, length), false);
	}
}
