package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;
import java.util.List;

/** Composite Chars (multiple Chars as a single one).
 * @param <T> type of Chars it contains
 */
public abstract class CompositeBytes<T extends Bytes> extends AbstractComposite<T> implements Bytes {

	/** Constructor. */
	public CompositeBytes() {
		super();
	}
	
	/** Constructor. */
	@SafeVarargs
	public CompositeBytes(T... bytes) {
		super(bytes);
	}

	/** Constructor. */
	public CompositeBytes(List<T> bytes) {
		super(bytes);
	}
	
	/** Composite Bytes.Readable. */
	public static class Readable extends CompositeBytes<Bytes.Readable> implements Bytes.Readable {
		
		/** Constructor. */
		public Readable() {
			super();
		}
		
		/** Constructor. */
		public Readable(Bytes.Readable... bytes) {
			super(bytes);
		}
		
		/** Constructor. */
		public Readable(List<Bytes.Readable> bytes) {
			super(bytes);
		}
		
		@Override
		public byte get() {
			Bytes.Readable bytes = list.get(index);
			byte b = bytes.get();
			position++;
			if (!bytes.hasRemaining()) index++;
			return b;
		}
		
		@Override
		public void get(byte[] buffer, int offset, int length) {
			while (length > 0) {
				Bytes.Readable bytes = list.get(index);
				int r = bytes.remaining();
				if (length < r) {
					bytes.get(buffer, offset, length);
					position += length;
					return;
				}
				bytes.get(buffer, offset, r);
				position += r;
				offset += r;
				length -= r;
				index++;
			}
		}
		
		@Override
		public byte getForward(int offset) {
			int i = index;
			int o = offset;
			do {
				Bytes.Readable bytes = list.get(i);
				int r = bytes.remaining();
				if (o < r)
					return bytes.getForward(o);
				o -= r;
				i++;
			} while (true);
		}
		
		@Override
		public ByteBuffer toByteBuffer() {
			byte[] buf = new byte[length];
			int off = 0;
			for (Bytes.Readable bytes : list) {
				int p = bytes.position();
				bytes.setPosition(0);
				int l = bytes.remaining();
				bytes.get(buf, off, l);
				bytes.setPosition(p);
				off += l;
			}
			return ByteBuffer.wrap(buf);
		}
		
		@Override
		public Bytes.Readable subBuffer(int startPosition, int length) {
			int i = 0;
			int p = startPosition;
			do {
				Bytes.Readable bytes = list.get(i);
				int r = bytes.length();
				if (p < r) {
					CompositeBytes.Readable result = new CompositeBytes.Readable();
					int l = Math.min(length, r - p);
					result.add(bytes.subBuffer(p, l));
					length -= l;
					while (length > 0 && ++i < list.size()) {
						bytes = list.get(i);
						l = Math.min(length, bytes.length());
						result.add(bytes.subBuffer(0, l));
						length -= l;
					}
					return result;
				}
				p -= r;
				i++;
			} while (i < list.size());
			return new ByteArray(new byte[0]);
		}
	}

	
	/** Composite Chars.Readable. */
	public static class Writable extends CompositeBytes<Bytes.Writable> implements Bytes.Writable {
		
		/** Constructor. */
		public Writable() {
			super();
		}
		
		/** Constructor. */
		public Writable(Bytes.Writable... bytes) {
			super(bytes);
		}
		
		/** Constructor. */
		public Writable(List<Bytes.Writable> bytes) {
			super(bytes);
		}
		
		@Override
		public void put(byte b) {
			Bytes.Writable bytes = list.get(index);
			bytes.put(b);
			position++;
			if (!bytes.hasRemaining()) index++;
		}
		
		@Override
		public void put(byte[] buffer, int offset, int length) {
			while (length > 0) {
				Bytes.Writable bytes = list.get(index);
				int r = bytes.remaining();
				if (length < r) {
					bytes.put(buffer, offset, length);
					position += length;
					return;
				}
				bytes.put(buffer, offset, r);
				position += r;
				offset += r;
				length -= r;
				index++;
			}
		}

		@Override
		public ByteBuffer toByteBuffer() {
			byte[] buf = new byte[length];
			int off = 0;
			for (Bytes.Writable bytes : list) {
				int p = bytes.position();
				bytes.setPosition(0);
				int l = bytes.remaining();
				if (!(bytes instanceof Bytes.Readable))
					throw new UnsupportedOperationException("Cannot convert a write-only Bytes to ByteBuffer");
				((Bytes.Readable)bytes).get(buf, off, l);
				bytes.setPosition(p);
				off += l;
			}
			ByteBuffer bb = ByteBuffer.wrap(buf);
			bb.position(this.position);
			return bb;
		}
		
		@Override
		public Bytes.Writable subBuffer(int startPosition, int length) {
			int i = 0;
			int p = startPosition;
			do {
				Bytes.Writable bytes = list.get(i);
				int r = bytes.length();
				if (p < r) {
					CompositeBytes.Writable result = new CompositeBytes.Writable();
					int l = Math.min(length, r - p);
					result.add(bytes.subBuffer(p, l));
					length -= l;
					while (length > 0 && ++i < list.size()) {
						bytes = list.get(i);
						l = Math.min(length, bytes.length());
						result.add(bytes.subBuffer(0, l));
						length -= l;
					}
					return result;
				}
				p -= r;
				i++;
			} while (i < list.size());
			return new ByteArray.Writable(new byte[0], false);
		}
	}
	
}
