package net.lecousin.framework.text;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

import net.lecousin.framework.io.data.RawByteBuffer;
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

	/** Append an ISO-8859 character as a byte. */
	public ByteArrayStringIso8859Buffer append(byte c) {
		if (strings == null) {
			strings = allocateArray(8);
			strings[0] = createString(newArrayStringCapacity);
			strings[0].append(c);
			lastUsed = 0;
			return this;
		}
		if (strings[lastUsed].appendNoEnlarge(c))
			return this;
		if (lastUsed < strings.length - 1) {
			strings[++lastUsed] = createString(newArrayStringCapacity);
			strings[lastUsed].append(c);
			return this;
		}
		ByteArrayStringIso8859[] a = allocateArray(++lastUsed + 8);
		System.arraycopy(strings, 0, a, 0, lastUsed);
		a[lastUsed] = createString(newArrayStringCapacity);
		a[lastUsed].append(c);
		strings = a;
		return this;
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
	
	/** Convert into an array of RawByteBuffer. */
	public RawByteBuffer[] asRawByteBuffers() {
		if (strings == null) return new RawByteBuffer[0];
		RawByteBuffer[] chars = new RawByteBuffer[lastUsed + 1];
		for (int i = 0; i <= lastUsed; ++i)
			chars[i] = strings[i].asRawByteBuffer();
		return chars;
	}
	
	/** Convert into an array of ByteBuffer. */
	public ByteBuffer[] asByteBuffers() {
		if (strings == null) return new ByteBuffer[0];
		ByteBuffer[] chars = new ByteBuffer[lastUsed + 1];
		for (int i = 0; i <= lastUsed; ++i)
			chars[i] = strings[i].asByteBuffer();
		return chars;
	}
}
