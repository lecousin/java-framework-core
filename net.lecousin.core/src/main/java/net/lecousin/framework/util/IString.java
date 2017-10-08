package net.lecousin.framework.util;

import java.util.List;

/**
 * Interface adding functionalities to CharSequence.
 */
public interface IString extends CharSequence {

	/** Append a character. */
	public void append(char c);
	
	/** Append characters. */
	public void append(char[] chars, int offset, int len);
	
	/** Append characters. */
	public void append(CharSequence s);
	
	/** Return the index of the first occurrence of the given character, starting at the given position, or -1 if not found. */
	public int indexOf(char c, int start);
	
	/** Return the index of the first occurrence of the given CharSequence, starting at the given position, or -1 if not found. */
	public int indexOf(CharSequence s, int start);
	
	/** Return the index of the first occurrence of the given character, or -1 if not found. */
	public default int indexOf(char c) { return indexOf(c, 0); }

	/** Return the index of the first occurrence of the given CharSequence, or -1 if not found. */
	public default int indexOf(CharSequence s) { return indexOf(s, 0); }
	
	/** Return a sub-string starting at the given position until the end. */
	public IString substring(int start);

	/** Return a sub-string starting at the given start position (included) until the given end position (excluded). */
	public IString substring(int start, int end);
	
	/** Fill the given character array with the content of this string. */
	public int fill(char[] chars, int start);

	/** Fill the given character array with the content of this string. */
	public default int fill(char[] chars) { return fill(chars, 0); }
	
	/** Remove spaces characters at the beginning of this string. */
	public void trimBeginning();

	/** Remove spaces characters at the end of this string. */
	public void trimEnd();

	/** Remove spaces characters at the beginning and the end of this string, and return this instance. */
	public default IString trim() {
		trimBeginning();
		trimEnd();
		return this;
	}
	
	/** Convert this string to lower case. */
	public void toLowerCase();

	/** Convert this string to upper case. */
	public void toUpperCase();
	
	/** Return a list of strings, by splitting the current string using the given character as separator. */
	public List<? extends IString> split(char sep);
	
	/** Compare the given CharSequence with this string. */
	public default boolean equals(CharSequence s) {
		int l = length();
		if (s.length() != l) return false;
		for (int i = 0; i < l; ++i)
			if (s.charAt(i) != charAt(i))
				return false;
		return true;
	}
	
	/** Convert this string into a Java string. */
	public default String asString() {
		char[] chars = new char[length()];
		fill(chars);
		return new String(chars);
	}
	
}
