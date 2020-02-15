package net.lecousin.framework.text;

import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.io.data.CharArray;

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
public class CharArrayString extends ArrayString {

	private char[] chars;
	
	/** Create an empty UnprotectedString with an initial capacity. */ 
	public CharArrayString(int initialCapacity) {
		chars = new char[initialCapacity];
		start = 0;
		end = -1;
		usableEnd = initialCapacity - 1;
	}
	
	/** Create a string with a single character. */
	public CharArrayString(char singleChar) {
		chars = new char[] { singleChar };
		start = 0;
		end = 0;
		usableEnd = 0;
	}

	/** Create an UnprotectedString based on an existing character array. */
	public CharArrayString(char[] chars, int offset, int len, int usableLength) {
		this.chars = chars;
		this.start = offset;
		this.end = offset + len - 1;
		this.usableEnd = offset + usableLength - 1;
	}

	/** Create an UnprotectedString based on an existing character array. */
	public CharArrayString(char[] chars) {
		this(chars, 0, chars.length, chars.length);
	}
	
	/** Creates an UnprotectedString from the given String (a copy of characters is done). */
	public CharArrayString(String s) {
		chars = s.toCharArray();
		start = 0;
		end = chars.length - 1;
		usableEnd = end;
	}
	
	/** Creates an UnprotectedString from the given String (a copy of characters is done). */
	public CharArrayString(String s, int startPos, int endPos) {
		chars = s.toCharArray();
		start = startPos;
		end = start + endPos - startPos - 1;
		usableEnd = end;
	}

	/** Creates an UnprotectedString from the given String (a copy of characters is done). */
	public CharArrayString(IString s) {
		chars = new char[s.length()];
		s.fill(chars);
		start = 0;
		end = chars.length - 1;
		usableEnd = end;
	}

	/** Creates an UnprotectedString from the given CharSequence (a copy of characters is done). */
	public CharArrayString(CharSequence s) {
		this(s.toString());
	}

	/** Creates an UnprotectedString from the given CharSequence (a copy of characters is done). */
	public CharArrayString(CharSequence s, int startPos, int endPos) {
		this(s.toString(), startPos, endPos);
	}
	
	@Override
	public char charAt(int index) {
		return chars[start + index];
	}
	
	/** Set the character at the given index. */
	@Override
	public void setCharAt(int index, char c) {
		if (index < 0 || index > end - start) throw new IllegalArgumentException("Character index " + index + " does not exist");
		chars[start + index] = c;
	}
	
	/** Return the first character, or -1 if the string is empty. */
	@Override
	public int firstChar() {
		if (end >= start) return chars[start];
		return -1;
	}
	
	/** Return the last character, or -1 if the string is empty. */
	@Override
	public int lastChar() {
		if (end >= start) return chars[end];
		return -1;
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
	@Override
	public void trimToSize() {
		char[] a = new char[end - start + 1];
		System.arraycopy(chars, start, a, 0, a.length);
		chars = a;
		start = 0;
		end = a.length - 1;
		usableEnd = 0;
	}
	
	/** Append the given character without enlarging the char array, return false if it cannot be done. */
	@Override
	public boolean appendNoEnlarge(char c) {
		if (end == usableEnd) return false;
		chars[++end] = c;
		return true;
	}
	
	@Override
	public CharArrayString append(char c) {
		if (end == usableEnd)
			enlarge(chars.length < 128 ? 64 : chars.length >> 1);
		chars[++end] = c;
		return this;
	}
	
	@Override
	public CharArrayString append(char[] chars, int offset, int len) {
		if (usableEnd - end < len) {
			int l = chars.length < 128 ? 64 : chars.length >> 1;
			if (l < len + 16) l = len + 16;
			enlarge(l);
		}
		System.arraycopy(chars, offset, this.chars, end + 1, len);
		end += len;
		return this;
	}
	
	@Override
	public CharArrayString append(CharSequence s) {
		if (s == null) s = "null";
		int l = s.length();
		if (l == 0) return this;
		if (l >= usableEnd - end)
			enlarge(l + 16);
		if (s instanceof IString) {
			((IString)s).fill(chars, end + 1);
			end += l;
			return this;
		}
		for (int i = 0; i < l; ++i)
			chars[++end] = s.charAt(i);
		return this;
	}
	
	@Override
	public CharArrayString append(CharSequence s, int startPos, int endPos) {
		if (s == null) return append("null");
		int l = endPos - startPos;
		if (l == 0) return this;
		if (l >= usableEnd - end)
			enlarge(l + 16);
		if (s instanceof IString) {
			((IString)s).substring(startPos, endPos).fill(chars, end + 1);
			end += l;
			return this;
		}
		for (int i = 0; i < l; ++i)
			chars[++end] = s.charAt(i + startPos);
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
		if (end <= start) return "";
		return new String(chars, this.start + start, end - start);
	}
	
	@Override
	public CharArrayString substring(int start, int end) {
		if (this.start + end > this.end) end = this.end - this.start + 1;
		if (end <= start) return new CharArrayString(0);
		return new CharArrayString(chars, this.start + start, end - start, end - start);
	}
	
	@Override
	public CharArrayString substring(int start) {
		if (this.start + start > end)
			return new CharArrayString(0);
		return new CharArrayString(chars, this.start + start, end - (this.start + start) + 1, end - (this.start + start) + 1);
	}
	
	@Override
	public CharArrayString replace(char oldChar, char newChar) {
		for (int i = start; i <= end; ++i)
			if (chars[i] == oldChar)
				chars[i] = newChar;
		return this;
	}
	
	@Override
	public CharArrayString replace(CharSequence search, CharSequence replace) {
		int sl = search.length();
		int rl = replace.length();
		if (sl == rl) {
			int pos = 0;
			int i;
			while ((i = indexOf(search, pos)) >= 0) {
				replace(i, i + sl - 1, replace);
				pos = i + sl;
			}
			return this;
		}
		if (sl > rl) {
			int pos = 0;
			int diff = 0;
			int i;
			while ((i = indexOf(search, pos)) >= 0) {
				if (diff > 0)
					System.arraycopy(chars, this.start + pos, chars, this.start + pos - diff, i - pos);
				overwrite(this.start + i - diff, replace);
				diff += sl - rl;
				pos = i + sl;
			}
			if (diff > 0) {
				System.arraycopy(chars, this.start + pos, chars, this.start + pos - diff, end + 1 - pos - this.start);
				end -= diff;
			}
			return this;
		}
		LinkedArrayList<Integer> found = new LinkedArrayList<>(10);
		int pos = 0;
		int i;
		while ((i = indexOf(search, pos)) >= 0) {
			found.add(Integer.valueOf(i));
			pos = i + sl;
		}
		if (found.isEmpty())
			return this;
		int diff = (rl - sl) * found.size();
		if (usableEnd - end < diff)
			enlarge(Math.min(diff, 16));
		pos = end + 1;
		i = found.size() - 1;
		while (i >= 0) {
			int index = found.get(i--).intValue();
			System.arraycopy(
				chars, this.start + index + sl,
				chars, this.start + index + rl + ((rl - sl) * (i + 1)),
				pos - index - sl - this.start);
			end += rl - sl;
			overwrite(this.start + index + ((rl - sl) * (i + 1)), replace);
			pos = this.start + index;
		}
		return this;
	}
	
	@Override
	public CharArrayString replace(int start, int end, CharSequence replace) {
		int l = replace.length();
		int l2 = end - start + 1;
		if (l == l2) {
			overwrite(this.start + start, replace);
			return this;
		}
		if (l < l2) {
			overwrite(this.start + start, replace);
			System.arraycopy(chars, this.start + start + l2, chars, this.start + start + l, this.end - end);
			this.end -= l2 - l;
			return this;
		}
		enlarge(Math.min(l - l2, 16));
		System.arraycopy(chars, this.start + start + l2, chars, this.start + start + l, this.end - end);
		overwrite(this.start + start, replace);
		this.end += l - l2;
		return this;
	}
	
	private void overwrite(int start, CharSequence s) {
		for (int i = s.length() - 1; i >= 0; --i)
			chars[start + i] = s.charAt(i);
	}
	
	@Override
	public int fill(char[] chars, int start) {
		int len = this.end - this.start + 1;
		System.arraycopy(this.chars, this.start, chars, start, len);
		return len;
	}
	
	@Override
	public int fillIso8859Bytes(byte[] bytes, int start) {
		int pos = 0;
		for (int i = this.start; i <= this.end; ++i)
			bytes[start + (pos++)] = (byte)this.chars[i];
		return pos;
	}
	
	@Override
	public List<CharArrayString> split(char sep) {
		LinkedList<CharArrayString> list = new LinkedList<>();
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
	public CharArrayString toLowerCase() {
		for (int i = start; i <= end; ++i)
			chars[i] = Character.toLowerCase(chars[i]);
		return this;
	}
	
	@Override
	public CharArrayString toUpperCase() {
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
	public CharArray asCharBuffer() {
		return new CharArray(chars, start, end - start + 1);
	}
	
	@Override
	public CharArray[] asCharBuffers() {
		return new CharArray[] { asCharBuffer() };
	}
	
	/** Return the underlying character array. */
	public char[] charArray() {
		return chars;
	}
	
	@Override
	public CharArrayString copy() {
		char[] copy = new char[end - start + 1];
		System.arraycopy(chars, start, copy, 0, end - start + 1);
		return new CharArrayString(copy);
	}
	
	/** Return the number of occurences of the given array in this string. */
	@Override
	public int countChar(char c) {
		int count = 0;
		for (int i = start; i <= end; ++i)
			if (chars[i] == c)
				count++;
		return count;
	}
}
