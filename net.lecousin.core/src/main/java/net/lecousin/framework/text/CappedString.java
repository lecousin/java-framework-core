package net.lecousin.framework.text;

import java.util.List;

import net.lecousin.framework.io.data.RawCharBuffer;

/** A String with a maximum size.
 * Any character added beyond the maximum size is skipped.
 */
public class CappedString implements IString {
	
	private int size = 0;
	private int maxSize;
	private CharArrayStringBuffer buffer = new CharArrayStringBuffer();

	/** Constructor. */
	public CappedString(int maxSize) {
		this.maxSize = maxSize;
	}
	
	public int getMaxSize() {
		return maxSize;
	}
	
	@Override
	public int length() {
		return size;
	}

	@Override
	public char charAt(int index) {
		return buffer.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return buffer.subSequence(start, end);
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public void setCharAt(int index, char c) {
		buffer.setCharAt(index, c);
	}

	@Override
	public CappedString append(char c) {
		if (size == maxSize)
			return this;
		buffer.append(c);
		size++;
		return this;
	}

	@Override
	public CappedString append(char[] chars, int offset, int len) {
		len = Math.min(maxSize - size, len);
		if (len == 0) return this;
		buffer.append(chars, offset, len);
		size += len;
		return this;
	}

	@Override
	public CappedString append(CharSequence s) {
		return append(s, 0, s.length());
	}

	@Override
	public CappedString append(CharSequence s, int start, int end) {
		int len = Math.min(maxSize - size, end - start);
		if (len == 0) return this;
		buffer.append(s, start, start + len);
		size += len;
		return this;
	}
	
	/** Add the given string at the beginning. */
	public CappedString addFirst(CharSequence s) {
		buffer.addFirst(s);
		size += s.length();
		if (size > maxSize) {
			buffer.removeEndChars(size - maxSize);
			size = maxSize;
		}
		return this;
	}

	@Override
	public int indexOf(char c, int start) {
		return buffer.indexOf(c, start);
	}

	@Override
	public int indexOf(CharSequence s, int start) {
		return buffer.indexOf(s, start);
	}

	@Override
	public CharArrayStringBuffer substring(int start) {
		return buffer.substring(start);
	}

	@Override
	public CharArrayStringBuffer substring(int start, int end) {
		return buffer.substring(start, end);
	}

	@Override
	public int fill(char[] chars, int start) {
		return buffer.fill(chars, start);
	}

	@Override
	public int fillIso8859Bytes(byte[] bytes, int start) {
		return buffer.fillIso8859Bytes(bytes, start);
	}

	@Override
	public CappedString trimBeginning() {
		buffer.trimBeginning();
		size = buffer.length();
		return this;
	}

	@Override
	public CappedString trimEnd() {
		buffer.trimEnd();
		size = buffer.length();
		return this;
	}

	@Override
	public CappedString replace(char oldChar, char newChar) {
		buffer.replace(oldChar, newChar);
		return this;
	}
	
	@Override
	public CappedString replace(CharSequence search, CharSequence replace) {
		buffer.replace(search, replace);
		size = buffer.length();
		if (size > maxSize) {
			buffer = buffer.substring(0, maxSize);
			size = maxSize;
		}
		return this;
	}
	
	@Override
	public IString replace(int start, int end, CharSequence replace) {
		buffer.replace(start, end, replace);
		size = buffer.length();
		if (size > maxSize) {
			buffer = buffer.substring(0, maxSize);
			size = maxSize;
		}
		return this;
	}

	@Override
	public CappedString removeEndChars(int nb) {
		buffer.removeEndChars(nb);
		size = buffer.length();
		return this;
	}

	@Override
	public CappedString removeStartChars(int nb) {
		buffer.removeStartChars(nb);
		size = buffer.length();
		return this;
	}

	@Override
	public CappedString toLowerCase() {
		buffer.toLowerCase();
		return this;
	}

	@Override
	public CappedString toUpperCase() {
		buffer.toUpperCase();
		return this;
	}

	@Override
	public List<CharArrayStringBuffer> split(char sep) {
		return buffer.split(sep);
	}

	@Override
	public CappedString copy() {
		CappedString copy = new CappedString(maxSize);
		copy.buffer = new CharArrayStringBuffer(buffer.copy());
		copy.size = size;
		return copy;
	}

	@Override
	public boolean startsWith(CharSequence start) {
		return buffer.startsWith(start);
	}

	@Override
	public boolean endsWith(CharSequence end) {
		return buffer.endsWith(end);
	}

	@Override
	public RawCharBuffer[] asCharBuffers() {
		return buffer.asCharBuffers();
	}

	@Override
	public String toString() {
		return buffer.toString();
	}
}
