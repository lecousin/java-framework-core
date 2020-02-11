package net.lecousin.framework.io.data;

import java.nio.CharBuffer;

import net.lecousin.framework.text.IString;

/** Chars Readable implementation wrapping a String. */
public class CharsFromString extends AbstractDataBufferFromString implements Chars.Readable {

	/** Constructor. */
	public CharsFromString(String str) {
		super(str);
	}

	/** Constructor. */
	public CharsFromString(String str, int offset, int length) {
		super(str, offset, length);
	}
	
	@Override
	public char get() {
		return str.charAt(offset + pos++);
	}

	@Override
	public void get(char[] buffer, int offset, int length) {
		str.getChars(this.offset + pos, this.offset + pos + length, buffer, offset);
		pos += length;
	}

	@Override
	public void get(IString string, int length) {
		string.append(str, offset + pos, offset + pos + length);
		pos += length;
	}

	@Override
	public char getForward(int offset) {
		return str.charAt(this.offset + pos + offset);
	}

	@Override
	public CharBuffer toCharBuffer() {
		CharBuffer b = CharBuffer.allocate(length);
		str.getChars(offset, offset + length, b.array(), 0);
		b.position(pos);
		return b;
	}
	
	@Override
	public void free() {
		// nothing to free
	}

	@Override
	public CharsFromString subBuffer(int startPosition, int length) {
		return new CharsFromString(str, offset + startPosition, length);
	}
}
