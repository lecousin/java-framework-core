package net.lecousin.framework.io.data;

import java.nio.CharBuffer;
import java.util.List;

import net.lecousin.framework.text.IString;

/** Composite Chars (multiple Chars as a single one).
 * @param <T> type of Chars it contains
 */
public abstract class CompositeChars<T extends Chars> extends AbstractComposite<T> implements Chars {

	/** Constructor. */
	public CompositeChars() {
		super();
	}
	
	/** Constructor. */
	@SafeVarargs
	public CompositeChars(T... chars) {
		super(chars);
	}

	/** Constructor. */
	public CompositeChars(List<T> chars) {
		super(chars);
	}
	
	/** Composite Chars.Readable. */
	public static class Readable extends CompositeChars<Chars.Readable> implements Chars.Readable {
		
		/** Constructor. */
		public Readable() {
			super();
		}
		
		/** Constructor. */
		public Readable(Chars.Readable... chars) {
			super(chars);
		}
		
		/** Constructor. */
		public Readable(List<Chars.Readable> chars) {
			super(chars);
		}
		
		@Override
		public char get() {
			Chars.Readable chars = list.get(index);
			char c = chars.get();
			position++;
			if (!chars.hasRemaining()) index++;
			return c;
		}
		
		@Override
		public void get(char[] buffer, int offset, int length) {
			while (length > 0) {
				Chars.Readable chars = list.get(index);
				int r = chars.remaining();
				if (length < r) {
					chars.get(buffer, offset, length);
					position += length;
					return;
				}
				chars.get(buffer, offset, r);
				position += r;
				offset += r;
				length -= r;
				index++;
			}
		}
		
		@Override
		public void get(IString string, int length) {
			while (length > 0) {
				Chars.Readable chars = list.get(index);
				int r = chars.remaining();
				if (length < r) {
					chars.get(string, length);
					position += length;
					return;
				}
				chars.get(string, r);
				position += r;
				length -= r;
				index++;
			}
		}
		
		@Override
		public char getForward(int offset) {
			int i = index;
			int o = offset;
			do {
				Chars.Readable chars = list.get(i);
				int r = chars.remaining();
				if (o < r)
					return chars.getForward(o);
				o -= r;
				i++;
			} while (true);
		}
		
		@Override
		public CharBuffer toCharBuffer() {
			char[] buf = new char[length];
			int off = 0;
			for (Chars.Readable chars : list) {
				int p = chars.position();
				chars.setPosition(0);
				int l = chars.remaining();
				chars.get(buf, off, l);
				chars.setPosition(p);
				off += l;
			}
			return CharBuffer.wrap(buf);
		}
		
		@Override
		public Chars.Readable subBuffer(int startPosition, int length) {
			int i = 0;
			int p = startPosition;
			do {
				Chars.Readable chars = list.get(i);
				int r = chars.length();
				if (p < r) {
					CompositeChars.Readable result = new CompositeChars.Readable();
					int l = Math.min(length, r - p);
					result.add(chars.subBuffer(p, l));
					length -= l;
					while (length > 0 && ++i < list.size()) {
						chars = list.get(i);
						l = Math.min(length, chars.length());
						result.add(chars.subBuffer(0, l));
						length -= l;
					}
					return result;
				}
				p -= r;
				i++;
			} while (i < list.size());
			return new RawCharBuffer(new char[0]);
		}
	}

	
	/** Composite Chars.Readable. */
	public static class Writable extends CompositeChars<Chars.Writable> implements Chars.Writable {
		
		/** Constructor. */
		public Writable() {
			super();
		}
		
		/** Constructor. */
		public Writable(Chars.Writable... chars) {
			super(chars);
		}
		
		/** Constructor. */
		public Writable(List<Chars.Writable> chars) {
			super(chars);
		}
		
		@Override
		public void put(char b) {
			Chars.Writable chars = list.get(index);
			chars.put(b);
			position++;
			if (!chars.hasRemaining()) index++;
		}
		
		@Override
		public void put(char[] buffer, int offset, int length) {
			while (length > 0) {
				Chars.Writable chars = list.get(index);
				int r = chars.remaining();
				if (length < r) {
					chars.put(buffer, offset, length);
					position += length;
					return;
				}
				chars.put(buffer, offset, r);
				position += r;
				offset += r;
				length -= r;
				index++;
			}
		}

		@Override
		public CharBuffer toCharBuffer() {
			char[] buf = new char[length];
			int off = 0;
			for (Chars.Writable chars : list) {
				int p = chars.position();
				chars.setPosition(0);
				int l = chars.remaining();
				if (!(chars instanceof Chars.Readable))
					throw new UnsupportedOperationException("Cannot convert a write-only Chars to CharBuffer");
				((Chars.Readable)chars).get(buf, off, l);
				chars.setPosition(p);
				off += l;
			}
			CharBuffer cb = CharBuffer.wrap(buf);
			cb.position(this.position);
			return cb;
		}
		
		@Override
		public Chars.Writable subBuffer(int startPosition, int length) {
			int i = 0;
			int p = startPosition;
			do {
				Chars.Writable chars = list.get(i);
				int r = chars.length();
				if (p < r) {
					CompositeChars.Writable result = new CompositeChars.Writable();
					int l = Math.min(length, r - p);
					result.add(chars.subBuffer(p, l));
					length -= l;
					while (length > 0 && ++i < list.size()) {
						chars = list.get(i);
						l = Math.min(length, chars.length());
						result.add(chars.subBuffer(0, l));
						length -= l;
					}
					return result;
				}
				p -= r;
				i++;
			} while (i < list.size());
			return new RawCharBuffer(new char[0]);
		}
	}
	
}
