package net.lecousin.framework.text;

import java.util.Collection;
import java.util.List;

import net.lecousin.framework.io.data.RawCharBuffer;

/**
 * Array of ByteArrayStringIso8859, allowing to add and remove characters without re-allocating a byte array.
 * 
 * @see CharArrayString
 */
public class ByteArrayStringIso8859Buffer extends ArrayStringBuffer<ByteArrayStringIso8859, ByteArrayStringIso8859Buffer> {

	/** Create a new empty string. */
	public ByteArrayStringIso8859Buffer() {
		super();
	}

	/** Create a string with the given one. */
	public ByteArrayStringIso8859Buffer(ByteArrayStringIso8859 string) {
		super(string);
	}
	
	/** Create a string with the given ones. */
	public ByteArrayStringIso8859Buffer(Collection<ByteArrayStringIso8859> strings) {
		super(strings);
	}
	
	/** Create a string with the given one. */
	public ByteArrayStringIso8859Buffer(String s) {
		strings = new ByteArrayStringIso8859[1];
		lastUsed = 0;
		strings[0] = new ByteArrayStringIso8859(s);
	}

	/** Create a string with the given one. */
	public ByteArrayStringIso8859Buffer(CharSequence s) {
		strings = new ByteArrayStringIso8859[1];
		lastUsed = 0;
		strings[0] = new ByteArrayStringIso8859(s);
	}
	
	/** Create a string with the given one. */
	public ByteArrayStringIso8859Buffer(IString s) {
		strings = new ByteArrayStringIso8859[1];
		lastUsed = 0;
		strings[0] = new ByteArrayStringIso8859(s);
	}
	
	@Override
	protected ByteArrayStringIso8859[] allocateArray(int arraySize) {
		return new ByteArrayStringIso8859[arraySize];
	}
	
	@Override
	protected ByteArrayStringIso8859 createString(int initialCapacity) {
		return new ByteArrayStringIso8859(initialCapacity);
	}
	
	@Override
	protected ByteArrayStringIso8859 createString(CharSequence s) {
		return new ByteArrayStringIso8859(s);
	}
	
	@Override
	protected ByteArrayStringIso8859 createString(CharSequence s, int startPos, int endPos) {
		return new ByteArrayStringIso8859(s, startPos, endPos);
	}
	
	@Override
	protected ByteArrayStringIso8859Buffer createBuffer() {
		return new ByteArrayStringIso8859Buffer();
	}
	
	@Override
	protected ByteArrayStringIso8859Buffer createBuffer(ByteArrayStringIso8859 s) {
		return new ByteArrayStringIso8859Buffer(s);
	}
	
	@Override
	protected ByteArrayStringIso8859Buffer createBuffer(List<ByteArrayStringIso8859> list) {
		return new ByteArrayStringIso8859Buffer(list);
	}
	
	@Override
	protected Class<ByteArrayStringIso8859> getArrayType() {
		return ByteArrayStringIso8859.class;
	}

	@Override
	public ByteArrayStringIso8859 copy() {
		byte[] copy = new byte[length()];
		fillIso8859Bytes(copy, 0);
		return new ByteArrayStringIso8859(copy);
	}

	@Override
	public RawCharBuffer[] asCharBuffers() {
		if (strings == null) return new RawCharBuffer[0];
		RawCharBuffer[] chars = new RawCharBuffer[lastUsed + 1];
		for (int i = 0; i <= lastUsed; ++i)
			chars[i] = new RawCharBuffer(strings[i].toChars());
		return chars;
	}
}
