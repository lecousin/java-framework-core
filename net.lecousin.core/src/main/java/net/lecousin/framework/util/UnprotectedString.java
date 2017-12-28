package net.lecousin.framework.util;

import java.nio.CharBuffer;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unprotected and mutable string.
 * String in Java is immutable, and every modification or sub-string creates a new instance with a new character array.
 * This means string manipulation has low performance because it implies many memory allocations, and copies.
 * This is mainly because Java strings may be shared as constants. 
 * The class StringBuffer adds some modification capabilities and is enough is many situations, but
 * this class provides additional capabilities to reuse character arrays.<br/>
 * This class simply wraps a character array, and add string functionalities on it.<br/>
 * It may be created with an existing character array, specifying the start offset to use,
 * the current number of characters used, and the total number of characters that may be used.<br/>
 * For example, creating a sub-string of an UnprotectedString will just create a new UnprotectedString
 * instance but sharing the same character array, with different offset and number of characters.
 * That means modifying a character in an UnprotectedString will also modify the character in sub-strings.<br/>
 */
public class UnprotectedString implements IString {

	/** Create an empty UnprotectedString with an initial capacity. */ 
	public UnprotectedString(int initialCapacity) {
		chars = new char[initialCapacity];
		start = 0;
		end = -1;
		usableEnd = initialCapacity - 1;
	}
	
	/** Create a string with a single character. */
	public UnprotectedString(char singleChar) {
		chars = new char[] { singleChar };
		start = 0;
		end = 0;
		usableEnd = 0;
	}

	/** Create an UnprotectedString based on an existing character array. */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public UnprotectedString(char[] chars, int offset, int len, int usableLength) {
		this.chars = chars;
		this.start = offset;
		this.end = offset + len - 1;
		this.usableEnd = offset + usableLength - 1;
	}

	/** Create an UnprotectedString based on an existing character array. */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public UnprotectedString(char[] chars) {
		this(chars, 0, chars.length, chars.length);
	}
	
	/** Creates an UnprotectedString from the given String (a copy of characters is done). */
	public UnprotectedString(String s) {
		chars = s.toCharArray();
		start = 0;
		end = chars.length - 1;
		usableEnd = end;
	}

	/** Creates an UnprotectedString from the given String (a copy of characters is done). */
	public UnprotectedString(IString s) {
		chars = new char[s.length()];
		s.fill(chars);
		start = 0;
		end = chars.length - 1;
		usableEnd = end;
	}

	/** Creates an UnprotectedString from the given CharSequence (a copy of characters is done). */
	public UnprotectedString(CharSequence s) {
		this(s.toString());
	}
	
	private char[] chars;
	private int start;
	private int end;
	private int usableEnd;
	
	@Override
	public int length() {
		return end - start + 1;
	}
	
	/** Make this string empty, only by setting the end offset to the start offset. */
	public void reset() {
		end = start - 1;
	}
	
	@Override
	public char charAt(int index) {
		return chars[start + index];
	}
	
	/** Set the character at the given index. */
	@Override
	public void setCharAt(int index, char c) throws IllegalArgumentException {
		if (index < 0 || index > end - start) throw new IllegalArgumentException("Character index " + index + " does not exist");
		chars[start + index] = c;
	}
	
	/** Return the first character, or -1 if the string is empty. */
	public int firstChar() {
		if (end >= start) return chars[start];
		return -1;
	}
	
	/** Return the last character, or -1 if the string is empty. */
	public int lastChar() {
		if (end >= start) return chars[end];
		return -1;
	}
	
	/** Return true if there are unused characters at the end of the array. */
	public int canAppendWithoutEnlarging() {
		return usableEnd - end;
	}
	
	private void enlarge(int add) {
		char[] a = new char[chars.length + add];
		System.arraycopy(chars, start, a, 0, end - start + 1);
		end -= start;
		start = 0;
		usableEnd = a.length - 1;
		chars = a;
	}
	
	/** Create a new character array to fit the current content of this string. */
	public void trimToSize() {
		char[] a = new char[end - start + 1];
		System.arraycopy(chars, start, a, 0, a.length);
		chars = a;
		start = 0;
		end = a.length - 1;
		usableEnd = 0;
	}
	
	@Override
	public UnprotectedString append(char c) {
		if (end == usableEnd)
			enlarge(chars.length < 1024 ? 512 : chars.length >> 1);
		chars[++end] = c;
		return this;
	}
	
	@Override
	public UnprotectedString append(char[] chars, int offset, int len) {
		if (usableEnd - end < len) {
			int l = chars.length < 1024 ? 512 : chars.length >> 1;
			if (l < len) l = len;
			enlarge(l);
		}
		System.arraycopy(chars, offset, this.chars, end + 1, len);
		end += len;
		return this;
	}
	
	@Override
	public UnprotectedString append(CharSequence s) {
		if (s instanceof UnprotectedString) {
			UnprotectedString us = (UnprotectedString)s;
			append(us.chars, us.start, us.end - us.start + 1);
			return this;
		}
		if (s instanceof UnprotectedStringBuffer) {
			UnprotectedStringBuffer usb = (UnprotectedStringBuffer)s;
			int l = usb.length();
			if (l == 0) return this;
			if (l >= usableEnd - end)
				enlarge(l);
			int i = 0;
			do {
				UnprotectedString us = usb.getUnprotectedString(i);
				System.arraycopy(us.chars, us.start, chars, end + 1, us.length());
				end += us.length();
			} while (++i < usb.getNbUsableUnprotectedStrings());
			return this;
		}
		int l = s.length();
		for (int i = 0; i < l; ++i)
			append(s.charAt(i));
		return this;
	}
	
	@Override
	public int indexOf(char c, int pos) {
		for (int i = start + pos; i <= end; ++i)
			if (chars[i] == c)
				return i - start;
		return -1;
	}
	
	@Override
	public int indexOf(CharSequence s, int pos) {
		int l = s.length();
		if (start + pos + l - 1 > end) return -1;
		char first = s.charAt(0);
		for (int i = start + pos; i <= end - l + 1; ++i)
			if (chars[i] == first) {
				int j = 1;
				while (j < l) {
					if (s.charAt(j) != chars[i + j]) break;
					j++;
				}
				if (j == l) return i - start;
			}
		return -1;
	}
	
	@Override
	public String subSequence(int start, int end) {
		if (end <= start) throw new IllegalArgumentException("Cannot create substring from " + start + " to " + end);
		return new String(chars, this.start + start, end - start);
	}
	
	@Override
	public UnprotectedString substring(int start, int end) {
		if (this.start + end > this.end) end = this.end - this.start + 1;
		if (end <= start) return new UnprotectedString(0);
		return new UnprotectedString(chars, this.start + start, end - start, end - start);
	}
	
	@Override
	public UnprotectedString substring(int start) {
		if (this.start + start > end)
			return new UnprotectedString(0);
		return new UnprotectedString(chars, this.start + start, end - (this.start + start) + 1, end - (this.start + start) + 1);
	}
	
	@Override
	public UnprotectedString trimBeginning() {
		while (start <= end && Character.isWhitespace(chars[start]))
			start++;
		return this;
	}
	
	@Override
	public UnprotectedString trimEnd() {
		while (end >= start && Character.isWhitespace(chars[end]))
			end--;
		return this;
	}
	
	@Override
	public UnprotectedString replace(char oldChar, char newChar) {
		for (int i = start; i <= end; ++i)
			if (chars[i] == oldChar)
				chars[i] = newChar;
		return this;
	}
	
	@Override
	public UnprotectedString removeEndChars(int nb) {
		end -= nb;
		if (end < start - 1) end = start - 1;
		return this;
	}
	
	@Override
	public UnprotectedString removeStartChars(int nb) {
		start += nb;
		if (start > end) start = end + 1;
		return this;
	}
	
	/** Remove the given number of characters at the beginning of the string. */
	public void moveForward(int skip) {
		this.start += skip;
	}

	@Override
	public int fill(char[] chars, int start) {
		int pos = 0;
		for (int i = this.start; i <= this.end; ++i)
			chars[start + (pos++)] = this.chars[i];
		return pos;
	}
	
	@Override
	public List<UnprotectedString> split(char sep) {
		LinkedList<UnprotectedString> list = new LinkedList<>();
		int pos = start;
		while (pos <= end) {
			int found = pos;
			while (found <= end && chars[found] != sep) found++;
			list.add(substring(pos - start, found - start));
			pos = found + 1;
		}
		return list;
	}
	
	@Override
	public UnprotectedString toLowerCase() {
		for (int i = start; i <= end; ++i)
			chars[i] = Character.toLowerCase(chars[i]);
		return this;
	}
	
	@Override
	public UnprotectedString toUpperCase() {
		for (int i = start; i <= end; ++i)
			chars[i] = Character.toUpperCase(chars[i]);
		return this;
	}
	
	@Override
	public boolean startsWith(CharSequence start) {
		int l = start.length();
		if (end - this.start + 1 < l) return false;
		for (int i = 0; i < l; ++i)
			if (chars[this.start + i] != start.charAt(i))
				return false;
		return true;
	}
	
	@Override
	public boolean endsWith(CharSequence end) {
		int l = end.length();
		if (this.end - start + 1 < l) return false;
		for (int i = 0; i < l; ++i)
			if (chars[this.end - i] != end.charAt(l - 1 - i))
				return false;
		return true;
	}
	
	@Override
	public String toString() {
		if (end < start) return "";
		return new String(chars, start, end - start + 1);
	}
	
	/** Create a CharBuffer wrapping the current string. */
	public CharBuffer asCharBuffer() {
		return CharBuffer.wrap(chars, start, end - start + 1);
	}
	
	@Override
	public CharBuffer[] asCharBuffers() {
		return new CharBuffer[] { asCharBuffer() };
	}
	
	/** Return the underlying character array. */
	@SuppressFBWarnings("EI_EXPOSE_REP")
	public char[] charArray() {
		return chars;
	}
	
	/** Return the current start offset in the underlying character array. */
	public int charArrayStart() {
		return start;
	}
	
	/** Return the number of occurences of the given array in this string. */
	public int countChar(char c) {
		int count = 0;
		for (int i = start; i <= end; ++i)
			if (chars[i] == c)
				count++;
		return count;
	}
}
