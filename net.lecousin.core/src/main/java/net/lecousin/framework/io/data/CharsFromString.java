package net.lecousin.framework.io.data;

import java.nio.CharBuffer;

import net.lecousin.framework.text.IString;

/** Chars Readable implementation wrapping a String. */
public class CharsFromString extends AbstractDataBufferFromString implements Chars.Readable {

	/** Constructor. */
	public CharsFromString(CharSequence str) {
		super(str);
	}

	/** Constructor. */
	public CharsFromString(CharSequence str, int offset, int length) {
		super(str, offset, length);
	}
	
	@Override
	public char get() {
		return str.charAt(offset + pos++);
	}

	@Override
	public void get(char[] buffer, int offset, int length) {
		if (str instanceof String) {
			((String)str).getChars(this.offset + pos, this.offset + pos + length, buffer, offset);
		} else if (str instanceof IString) {
			IString s = (IString)str;
			s.substring(offset, length).fill(buffer, offset);
		} else {
			for (int i = 0; i < length; ++i)
				buffer[offset + i] = str.charAt(i);
		}
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
		int p = pos;
		pos = 0;
		get(b.array(), 0, length);
		b.position(p);
		pos = p;
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
