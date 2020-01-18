package net.lecousin.framework.text;

/**
 * Unprotected and mutable string using an array.
 * 
 * @see CharArrayString
 * @see ByteArrayString
 */
public abstract class ArrayString implements IString {
	
	protected int start;
	protected int end;
	protected int usableEnd;

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
	
	/** Return the first character, or -1 if the string is empty. */
	public abstract int firstChar();
	
	/** Return the last character, or -1 if the string is empty. */
	public abstract int lastChar();
	
	/** Return then number of unused characters at the end of the array. */
	public int canAppendWithoutEnlarging() {
		return usableEnd - end;
	}
	
	/** Create a new array to fit the current content of this string. */
	public abstract void trimToSize();
	
	/** Append the given character without enlarging the array, return false if it cannot be done. */
	public abstract boolean appendNoEnlarge(char c);
	
	@Override
	public ArrayString trimBeginning() {
		while (start <= end && Character.isWhitespace(firstChar()))
			start++;
		return this;
	}
	
	@Override
	public ArrayString trimEnd() {
		while (end >= start && Character.isWhitespace(lastChar()))
			end--;
		return this;
	}
	
	@Override
	public ArrayString removeEndChars(int nb) {
		end -= nb;
		if (end < start - 1) end = start - 1;
		return this;
	}
	
	@Override
	public ArrayString removeStartChars(int nb) {
		start += nb;
		if (start > end) start = end + 1;
		return this;
	}
	
	/** Remove the given number of characters at the beginning of the string. */
	public void moveForward(int skip) {
		this.start += skip;
	}

	/** Return the current start offset in the underlying character array. */
	public int arrayStart() {
		return start;
	}
	
	/** Return the number of occurences of the given array in this string. */
	public abstract int countChar(char c);
}
