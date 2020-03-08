package net.lecousin.framework.text;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

import net.lecousin.framework.io.data.ByteArray;
import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.CharArray;
import net.lecousin.framework.memory.ByteArrayCache;

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
	protected ByteArrayStringIso8859 createString(char singleChar) {
		return new ByteArrayStringIso8859((byte)singleChar);
	}
	
	@Override
	protected ByteArrayStringIso8859 createString(char[] chars) {
		ByteArrayStringIso8859 s = new ByteArrayStringIso8859(chars.length);
		s.append(chars);
		return s;
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
	public CharArray[] asCharBuffers() {
		if (strings == null) return new CharArray[0];
		CharArray[] chars = new CharArray[lastUsed + 1];
		for (int i = 0; i <= lastUsed; ++i)
			chars[i] = new CharArray(strings[i].toChars());
		return chars;
	}
	
	/** Convert into an array of ByteArray. */
	public ByteArray[] asByteArrays() {
		if (strings == null) return new ByteArray[0];
		ByteArray[] chars = new ByteArray[lastUsed + 1];
		for (int i = 0; i <= lastUsed; ++i)
			chars[i] = strings[i].asByteArray();
		return chars;
	}
	
	/** Convert into a ByteArray. */
	public ByteArray asByteArray() {
		if (strings == null) return new ByteArray(new byte[0]);
		if (lastUsed == 0)
			return strings[0].asByteArray();
		int len = length();
		byte[] bytes = ByteArrayCache.getInstance().get(len, true);
		fillIso8859Bytes(bytes);
		return new ByteArray.Writable(bytes, 0, len, true);
	}
	
	/** Convert into an array of ByteBuffer. */
	public ByteBuffer[] asByteBuffers() {
		if (strings == null) return new ByteBuffer[0];
		ByteBuffer[] chars = new ByteBuffer[lastUsed + 1];
		for (int i = 0; i <= lastUsed; ++i)
			chars[i] = strings[i].asByteBuffer();
		return chars;
	}
	
	/** Convert into a ByteBuffer. */
	public ByteBuffer asByteBuffer() {
		return asByteArray().toByteBuffer();
	}
	
	/** Return readable bytes from this buffer. */
	public Bytes.Readable asReadableBytes() {
		return new ReadableBytes(0, length());
	}
	
	private class ReadableBytes implements Bytes.Readable {
		
		private ReadableBytes(int pos, int len) {
			this.pos = pos;
			this.len = len;
		}
		
		private int pos;
		private int len;
		
		@Override
		public void setPosition(int position) {
			pos = position;
		}
		
		@Override
		public int remaining() {
			return len - pos;
		}
		
		@Override
		public int position() {
			return pos;
		}
		
		@Override
		public int length() {
			return len;
		}
		
		@Override
		public void free() {
			// nothing
		}
		
		@Override
		public ByteBuffer toByteBuffer() {
			ByteBuffer b = ByteArrayStringIso8859Buffer.this.asByteBuffer();
			b.position(pos);
			return b;
		}
		
		@Override
		public Bytes.Readable subBuffer(int startPosition, int length) {
			return new ReadableBytes(startPosition, length);
		}
		
		@Override
		public byte getForward(int offset) {
			return (byte)charAt(pos + offset);
		}
		
		@Override
		public void get(byte[] buffer, int offset, int length) {
			int p = 0;
			for (int i = 0; i <= lastUsed; ++i) {
				int l = strings[i].length();
				if (p + l > pos) {
					int o = pos - p;
					int l2 = Math.min(l - o, length);
					System.arraycopy(strings[i].chars, strings[i].start + o, buffer, offset, l2);
					length -= l2;
					if (length == 0)
						return;
					offset += l2;
					pos += l2;
				}
				p += l;
			}
		}
		
		@Override
		public byte get() {
			return (byte)charAt(pos++);
		}
		
	}
	
	/** Return a writable bytes to this buffer. */
	public Bytes.Writable asWritableBytes() {
		return new WritableBytes(length(), Integer.MAX_VALUE);
	}
	
	private class WritableBytes implements Bytes.Writable {
		
		public WritableBytes(int pos, int length) {
			this.pos = pos;
			this.len = length;
		}
		
		private int pos;
		private int len;

		@Override
		public ByteBuffer toByteBuffer() {
			return ByteArrayStringIso8859Buffer.this.asByteBuffer();
		}

		@Override
		public int length() {
			return len;
		}

		@Override
		public int position() {
			return pos;
		}

		@Override
		public void setPosition(int position) {
			if (position > ByteArrayStringIso8859Buffer.this.length())
				throw new IllegalArgumentException();
			pos = position;
		}

		@Override
		public int remaining() {
			return len - pos;
		}

		@Override
		public void free() {
			// nothing
		}

		@Override
		public void put(byte b) {
			append(b);
			pos++;
		}

		@Override
		public void put(byte[] buffer, int offset, int length) {
			append(new ByteArrayStringIso8859(buffer, offset, length, length));
			pos += length;
		}

		@Override
		public Bytes.Writable subBuffer(int startPosition, int length) {
			return new WritableBytes(startPosition, length);
		}
		
	}
}
