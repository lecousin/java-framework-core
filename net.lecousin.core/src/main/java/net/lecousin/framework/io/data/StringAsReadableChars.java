package net.lecousin.framework.io.data;

import java.nio.CharBuffer;

import net.lecousin.framework.text.IString;

/** Chars Readable implementation wrapping a String. */
public class StringAsReadableChars implements Chars.Readable {

	/** Constructor. */
	public StringAsReadableChars(String str) {
		this(str, 0, str.length());
	}

	/** Constructor. */
	public StringAsReadableChars(String str, int offset, int length) {
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
	public StringAsReadableChars subBuffer(int startPosition, int length) {
		return new StringAsReadableChars(str, offset + startPosition, length);
	}

	@Override
	public CharBuffer toCharBuffer() {
		CharBuffer b = CharBuffer.allocate(length);
		str.getChars(offset, offset + length, b.array(), 0);
		b.position(pos);
		return b;
	}

}
