package net.lecousin.framework.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.io.data.RawByteBuffer;
import net.lecousin.framework.io.data.RawCharBuffer;

/**
 * Unprotected and mutable string using a byte array to store ASCII characters.
 */
public class UnprotectedStringIso8859 implements IString {

	/** Create an empty UnprotectedString with an initial capacity. */ 
	public UnprotectedStringIso8859(int initialCapacity) {
		chars = new byte[initialCapacity];
		start = 0;
		end = -1;
		usableEnd = initialCapacity - 1;
	}
	
	/** Create a string with a single character. */
	public UnprotectedStringIso8859(byte singleChar) {
		chars = new byte[] { singleChar };
		start = 0;
		end = 0;
		usableEnd = 0;
	}

	/** Create an UnprotectedString based on an existing character array. */
	public UnprotectedStringIso8859(byte[] chars, int offset, int len, int usableLength) {
		this.chars = chars;
		this.start = offset;
		this.end = offset + len - 1;
		this.usableEnd = offset + usableLength - 1;
	}

	/** Create an UnprotectedString based on an existing character array. */
	public UnprotectedStringIso8859(byte[] chars) {
		this(chars, 0, chars.length, chars.length);
	}
	
	/** Creates an UnprotectedString from the given String (a copy of characters is done). */
	public UnprotectedStringIso8859(String s) {
		chars = s.getBytes(StandardCharsets.US_ASCII);
		start = 0;
		end = chars.length - 1;
		usableEnd = end;
	}
	
	/** Creates an UnprotectedString from the given String (a copy of characters is done). */
	public UnprotectedStringIso8859(String s, int startPos, int endPos) {
		chars = s.getBytes(StandardCharsets.US_ASCII);
		start = startPos;
		end = start + endPos - startPos - 1;
		usableEnd = end;
	}

	/** Creates an UnprotectedString from the given String (a copy of characters is done). */
	public UnprotectedStringIso8859(IString s) {
		chars = new byte[s.length()];
		s.fillIso8859Bytes(chars);
		start = 0;
		end = chars.length - 1;
		usableEnd = end;
	}

	/** Creates an UnprotectedString from the given CharSequence (a copy of characters is done). */
	public UnprotectedStringIso8859(CharSequence s) {
		this(s.toString());
	}

	/** Creates an UnprotectedString from the given CharSequence (a copy of characters is done). */
	public UnprotectedStringIso8859(CharSequence s, int startPos, int endPos) {
		this(s.toString(), startPos, endPos);
	}
	
	private byte[] chars;
	private int start;
	private int end;
	private int usableEnd;
	
	@Override
	public int length() {
		return end - start + 1;
	}
	
	@Override
	public boolean isEmpty() {
		return end == -1 || end < start;
	}
	
	/** Make this string empty, only by setting the end offset to the start offset. */
	public void reset() {
		end = start - 1;
	}
	
	@Override
	public char charAt(int index) {
		return (char)(chars[start + index] & 0xFF);
	}
	
	/** Set the character at the given index. */
	@Override
	public void setCharAt(int index, char c) {
		if (index < 0 || index > end - start) throw new IllegalArgumentException("Character index " + index + " does not exist");
		chars[start + index] = (byte)c;
	}
	
	/** Return the first character, or -1 if the string is empty. */
	public int firstChar() {
		if (end >= start) return chars[start] & 0xFF;
		return -1;
	}
	
	/** Return the last character, or -1 if the string is empty. */
	public int lastChar() {
		if (end >= start) return chars[end] & 0xFF;
		return -1;
	}
	
	/** Return then number of unused characters at the end of the array. */
	public int canAppendWithoutEnlarging() {
		return usableEnd - end;
	}
	
	private void enlarge(int add) {
		byte[] a = new byte[chars.length + add];
		System.arraycopy(chars, start, a, 0, end - start + 1);
		end -= start;
		start = 0;
		usableEnd = a.length - 1;
		chars = a;
	}
	
	/** Create a new character array to fit the current content of this string. */
	public void trimToSize() {
		byte[] a = new byte[end - start + 1];
		System.arraycopy(chars, start, a, 0, a.length);
		chars = a;
		start = 0;
		end = a.length - 1;
		usableEnd = 0;
	}
	
	@Override
	public UnprotectedStringIso8859 append(char c) {
		if (end == usableEnd)
			enlarge(chars.length < 128 ? 64 : chars.length >> 1);
		chars[++end] = (byte)c;
		return this;
	}
	
	/** Append a character. */
	public UnprotectedStringIso8859 append(byte c) {
		if (end == usableEnd)
			enlarge(chars.length < 128 ? 64 : chars.length >> 1);
		chars[++end] = c;
		return this;
	}
	
	@Override
	public UnprotectedStringIso8859 append(char[] chars, int offset, int len) {
		if (usableEnd - end < len) {
			int l = chars.length < 128 ? 64 : chars.length >> 1;
			if (l < len + 16) l = len + 16;
			enlarge(l);
		}
		for (int i = 0; i < len; ++i)
			this.chars[end + 1 + i] = (byte)chars[offset + i];
		end += len;
		return this;
	}

	/** Append characters. */
	public UnprotectedStringIso8859 append(byte[] chars, int offset, int len) {
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
	public UnprotectedStringIso8859 append(CharSequence s) {
		if (s == null) s = "null";
		int l = s.length();
		if (l == 0) return this;
		if (l >= usableEnd - end)
			enlarge(l + 16);
		for (int i = 0; i < l; ++i)
			chars[++end] = (byte)s.charAt(i);
		return this;
	}
	
	@Override
	public UnprotectedStringIso8859 append(CharSequence s, int startPos, int endPos) {
		if (s == null) return append("null");
		int l = endPos - startPos;
		if (l == 0) return this;
		if (l >= usableEnd - end)
			enlarge(l + 16);
		for (int i = 0; i < l; ++i)
			chars[++end] = (byte)s.charAt(i + startPos);
		return this;
	}
	
	@Override
	public int indexOf(char c, int pos) {
		for (int i = start + pos; i <= end; ++i)
			if ((chars[i] & 0xFF) == c)
				return i - start;
		return -1;
	}
	
	@Override
	public int indexOf(CharSequence s, int pos) {
		int l = s.length();
		if (start + pos + l - 1 > end) return -1;
		char first = s.charAt(0);
		for (int i = start + pos; i <= end - l + 1; ++i)
			if ((chars[i] & 0xFF) == first) {
				int j = 1;
				while (j < l) {
					if (s.charAt(j) != (chars[i + j] & 0xFF)) break;
					j++;
				}
				if (j == l) return i - start;
			}
		return -1;
	}
	
	@Override
	public String subSequence(int start, int end) {
		if (end <= start) throw new IllegalArgumentException("Cannot create substring from " + start + " to " + end);
		return new String(chars, this.start + start, end - start, StandardCharsets.ISO_8859_1);
	}
	
	@Override
	public UnprotectedStringIso8859 substring(int start, int end) {
		if (this.start + end > this.end) end = this.end - this.start + 1;
		if (end <= start) return new UnprotectedStringIso8859(0);
		return new UnprotectedStringIso8859(chars, this.start + start, end - start, end - start);
	}
	
	@Override
	public UnprotectedStringIso8859 substring(int start) {
		if (this.start + start > end)
			return new UnprotectedStringIso8859(0);
		return new UnprotectedStringIso8859(chars, this.start + start, end - (this.start + start) + 1, end - (this.start + start) + 1);
	}
	
	@Override
	public UnprotectedStringIso8859 trimBeginning() {
		while (start <= end && Character.isWhitespace(chars[start]))
			start++;
		return this;
	}
	
	@Override
	public UnprotectedStringIso8859 trimEnd() {
		while (end >= start && Character.isWhitespace(chars[end]))
			end--;
		return this;
	}
	
	@Override
	public UnprotectedStringIso8859 replace(char oldChar, char newChar) {
		for (int i = start; i <= end; ++i)
			if ((chars[i] & 0xFF) == oldChar)
				chars[i] = (byte)newChar;
		return this;
	}
	
	@Override
	public UnprotectedStringIso8859 removeEndChars(int nb) {
		end -= nb;
		if (end < start - 1) end = start - 1;
		return this;
	}
	
	@Override
	public UnprotectedStringIso8859 removeStartChars(int nb) {
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
		int len = this.end - this.start + 1;
		for (int i = 0; i < len; ++i)
			chars[start + i] = (char)(this.chars[this.start + i] & 0xFF);
		return len;
	}
	
	@Override
	public int fillIso8859Bytes(byte[] bytes, int start) {
		int len = this.end - this.start + 1;
		System.arraycopy(this.chars, this.start, bytes, start, len);
		return len;
	}
	
	@Override
	public List<UnprotectedStringIso8859> split(char sep) {
		LinkedList<UnprotectedStringIso8859> list = new LinkedList<>();
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
	public UnprotectedStringIso8859 toLowerCase() {
		for (int i = start; i <= end; ++i)
			chars[i] = (byte)Character.toLowerCase(chars[i] & 0xFF);
		return this;
	}
	
	@Override
	public UnprotectedStringIso8859 toUpperCase() {
		for (int i = start; i <= end; ++i)
			chars[i] = (byte)Character.toUpperCase(chars[i] & 0xFF);
		return this;
	}
	
	@Override
	public boolean startsWith(CharSequence start) {
		int l = start.length();
		if (end - this.start + 1 < l) return false;
		for (int i = 0; i < l; ++i)
			if ((chars[this.start + i] & 0xFF) != start.charAt(i))
				return false;
		return true;
	}
	
	@Override
	public boolean endsWith(CharSequence end) {
		int l = end.length();
		if (this.end - start + 1 < l) return false;
		for (int i = 0; i < l; ++i)
			if ((chars[this.end - i] & 0xFF) != end.charAt(l - 1 - i))
				return false;
		return true;
	}
	
	@Override
	public String toString() {
		if (end < start) return "";
		return new String(chars, start, end - start + 1, StandardCharsets.ISO_8859_1);
	}
	
	/** Create a char array from this string. */
	public char[] toChars() {
		char[] copy = new char[length()];
		fill(copy, 0);
		return copy;
	}
	
	@Override
	public RawCharBuffer[] asCharBuffers() {
		return new RawCharBuffer[] { new RawCharBuffer(toChars()) };
	}
	
	@Override
	public UnprotectedStringIso8859 copy() {
		byte[] copy = new byte[end - start + 1];
		System.arraycopy(chars, start, copy, 0, end - start + 1);
		return new UnprotectedStringIso8859(copy);
	}
	
	/**
	 * Create an AsyncConsumer of bytes that creates UnprotectedStringAscii for each buffer, and gives it to the consumer.
	 * @param <TError> type of error
	 * @param consumer consumer of UnprotectedStringAscii
	 * @return the AsyncConsumer
	 */
	public static <TError extends Exception> AsyncConsumer.Simple<ByteBuffer, TError> bytesConsumer(
		Function<UnprotectedStringIso8859, IAsync<TError>> consumer
	) {
		return new AsyncConsumer.Simple<ByteBuffer, TError>() {
			@Override
			public IAsync<TError> consume(ByteBuffer data, Consumer<ByteBuffer> onDataRelease) {
				RawByteBuffer raw = new RawByteBuffer(data);
				IAsync<TError> write = consumer.apply(
					new UnprotectedStringIso8859(raw.array, raw.arrayOffset, raw.length, raw.length));
				if (onDataRelease != null) {
					if (data.hasArray())
						write.onDone(() -> onDataRelease.accept(data));
					else
						onDataRelease.accept(data);
				}
				return write;
			}
		};
	}
}
