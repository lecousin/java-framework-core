package net.lecousin.framework.text;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.List;

import net.lecousin.framework.io.data.CharArray;

/**
 * Interface adding functionalities to CharSequence.
 */
public interface IString extends CharSequence, Appendable {
	
	/** Return true if empty (length == 0). */
	boolean isEmpty();

	/** Set a character. */
	void setCharAt(int index, char c);
	
	/** Append a character. */
	@Override
	IString append(char c);
	
	/** Append characters. */
	IString append(char[] chars, int offset, int len);
	
	/** Append characters. */
	default IString append(char[] chars) {
		return append(chars, 0, chars.length);
	}
	
	/** Append characters. */
	@Override
	IString append(CharSequence s);
	
	/** Append characters. */
	@Override
	IString append(CharSequence s, int start, int end);
	
	/** Return the index of the first occurrence of the given character, starting at the given position, or -1 if not found. */
	int indexOf(char c, int start);
	
	/** Return the index of the first occurrence of the given CharSequence, starting at the given position, or -1 if not found. */
	int indexOf(CharSequence s, int start);
	
	/** Return the index of the first occurrence of the given character, or -1 if not found. */
	default int indexOf(char c) { return indexOf(c, 0); }

	/** Return the index of the first occurrence of the given CharSequence, or -1 if not found. */
	default int indexOf(CharSequence s) { return indexOf(s, 0); }
	
	/** Return a sub-string starting at the given position until the end. */
	IString substring(int start);

	/** Return a sub-string starting at the given start position (included) until the given end position (excluded). */
	IString substring(int start, int end);
	
	/** Fill the given character array with the content of this string. */
	int fill(char[] chars, int start);

	/** Fill the given character array with the content of this string. */
	default int fill(char[] chars) { return fill(chars, 0); }
	
	/** Same as with char but with bytes, all characters are directly converted into bytes without CharsetEncoder. */
	int fillIso8859Bytes(byte[] bytes, int start);

	/** Same as with char but with bytes, all characters are directly converted into bytes without CharsetEncoder. */
	default int fillIso8859Bytes(byte[] bytes) { return fillIso8859Bytes(bytes, 0); }

	/** Same as with char but with bytes, all characters are directly converted into bytes without CharsetEncoder. */
	default byte[] toIso8859Bytes() {
		byte[] bytes = new byte[length()];
		fillIso8859Bytes(bytes, 0);
		return bytes;
	}
	
	/** Remove spaces characters at the beginning of this string. */
	IString trimBeginning();

	/** Remove spaces characters at the end of this string. */
	IString trimEnd();

	/** Remove spaces characters at the beginning and the end of this string, and return this instance. */
	default IString trim() {
		trimBeginning();
		trimEnd();
		return this;
	}

	/** Replace all occurrences of oldChar into newChar. */
	IString replace(char oldChar, char newChar);
	
	/** Replace all occurrences of search into replace. */
	IString replace(CharSequence search, CharSequence replace);
	
	/** Replace all occurrences of search into replace. */
	IString replace(char oldChar, CharSequence replace);
	
	/** Replace all occurrences of search into replace. */
	IString replace(char oldChar, char[] replace);
	
	/** Replace all occurrences of search into replace. */
	IString replace(CharSequence search, char replace);
	
	/** Replace all occurrences of search into replace. */
	IString replace(CharSequence search, char[] replace);

	/** Remove characters from start to end (inclusive), and replace them by the given character. */
	IString replace(int start, int end, char replace);

	/** Remove characters from start to end (inclusive), and replace them by the given string. */
	IString replace(int start, int end, CharSequence replace);

	/** Remove characters from start to end (inclusive), and replace them by the given string. */
	IString replace(int start, int end, char[] replace);
	
	/** Remove the given number of characters at the end. */
	IString removeEndChars(int nb);
	
	/** Remove the given number of characters at the beginning. */
	IString removeStartChars(int nb);
	
	/** Convert this string to lower case. */
	IString toLowerCase();

	/** Convert this string to upper case. */
	IString toUpperCase();
	
	/** Return a list of strings, by splitting the current string using the given character as separator. */
	List<? extends IString> split(char sep);
	
	/** Create a copy of this string, so it can be modified safely. */
	IString copy();
	
	/** Compare the given CharSequence with this string. */
	@SuppressWarnings("squid:S1201") // we want the name equals
	default boolean equals(CharSequence s) {
		int l = length();
		if (s.length() != l) return false;
		for (int i = 0; i < l; ++i)
			if (s.charAt(i) != charAt(i))
				return false;
		return true;
	}
	
	/** Return true if this string starts with the given one. */
	boolean startsWith(CharSequence start);
	
	/** Return true if this string ends with the given one. */
	boolean endsWith(CharSequence end);
	
	/** Return true if this string is a start for the given one.
	 * This string's length must be less or equal to the start's length.
	 */
	default boolean isStartOf(CharSequence s) {
		int l = length();
		if (l > s.length()) return false;
		for (int i = 0; i < l; ++i)
			if (charAt(i) != s.charAt(i))
				return false;
		return true;
	}
	
	/** Convert this string into a Java string. */
	default String asString() {
		char[] chars = new char[length()];
		fill(chars);
		return new String(chars);
	}
	
	/** Convert into an array of character arrays. */
	default char[][] asCharacters() {
		int l = length();
		if (l == 0) return new char[0][];
		char[] chars = new char[l];
		fill(chars);
		return new char[][] { chars };
	}
	
	/** Convert into an array of character buffers. */
	CharArray[] asCharBuffers();
	
	/** Encode this string into a ByteBuffer using the specified charset. */
	@SuppressWarnings("squid:S2259") // false positive: cr cannot be null because cbs length cannot be 0
	default ByteBuffer encode(Charset cs) throws CharacterCodingException {
		CharArray[] cbs = asCharBuffers();
		if (cbs.length == 0)
			return ByteBuffer.allocate(0);
		int len = length();
        if (len == 0)
            return ByteBuffer.allocate(0);
		CharsetEncoder ce = cs.newEncoder();
        int en = (int)(len * ce.maxBytesPerChar());
        byte[] ba = new byte[en];
        ce.onMalformedInput(CodingErrorAction.REPLACE)
          .onUnmappableCharacter(CodingErrorAction.REPLACE)
          .reset();
        ByteBuffer bb = ByteBuffer.wrap(ba);
    	CoderResult cr = null;
    	for (int i = 0; i < cbs.length; ++i)
    		cr = ce.encode(cbs[i].toCharBuffer(), bb, i == cbs.length - 1);
        if (!cr.isUnderflow())
            cr.throwException();
        cr = ce.flush(bb);
        if (!cr.isUnderflow())
            cr.throwException();
        bb.flip();
        return bb;
	}
	
}
