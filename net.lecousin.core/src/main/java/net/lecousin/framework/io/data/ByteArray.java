package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;

import net.lecousin.framework.memory.ByteArrayCache;

/** Byte array. */
public class ByteArray extends DataArray<byte[]> implements Bytes.Readable {

	/** Create a ByteArray from a ByteBuffer. The returned ByteArray is Writable if the given ByteArray is not read-only. */
	public static ByteArray fromByteBuffer(ByteBuffer b) {
		if (b.hasArray()) {
			ByteArray ba;
			if (b.isReadOnly())
				ba = new ByteArray(b.array(), b.arrayOffset(), b.arrayOffset() + b.position() + b.remaining());
			else
				ba = new ByteArray.Writable(b.array(), b.arrayOffset(), b.arrayOffset() + b.position() + b.remaining(), true);
			ba.currentOffset = b.arrayOffset() + b.position();
			return ba;
		}
		ByteArray.Writable ba = new ByteArray.Writable(ByteArrayCache.getInstance().get(b.remaining(), true), true);
		ba.length = b.remaining();
		b.get(ba.array, 0, ba.length);
		b.position(b.position() - ba.length);
		return ba;
	}
	
	/** Create a ByteArray from a ByteBuffer. The returned ByteArray is Writable if the given ByteArray is not read-only. */
	public static ByteArray readOnlyFromByteBuffer(ByteBuffer b) {
		if (b.hasArray()) {
			ByteArray ba = new ByteArray(b.array(), b.arrayOffset(), b.arrayOffset() + b.position() + b.remaining());
			ba.currentOffset = b.arrayOffset() + b.position();
			return ba;
		}
		ByteArray ba = new ByteArray(ByteArrayCache.getInstance().get(b.remaining(), true));
		ba.length = b.remaining();
		b.get(ba.array, 0, ba.length);
		b.position(b.position() - ba.length);
		return ba;
	}
	
	/** Constructor. */
	public ByteArray(byte[] buffer, int offset, int length) {
		super(buffer, offset, length);
	}
	
	/** Constructor. */
	public ByteArray(byte[] buffer) {
		super(buffer, 0, buffer.length);
	}
	
	/** Constructor. */
	public ByteArray(ByteArray copy) {
		super(copy);
	}
	
	/** Create a copy of this buffer. */
	public ByteArray duplicate() {
		return new ByteArray(this);
	}
	
	@Override
	public byte get() {
		return array[currentOffset++];
	}
	
	@Override
	public void get(byte[] buffer, int offset, int length) {
		System.arraycopy(array, currentOffset, buffer, offset, length);
		currentOffset += length;
	}
	
	@Override
	public byte getForward(int offset) {
		return array[currentOffset + offset];
	}
	
	/** Used when a ByteBuffer as been converted into a RawByteBuffer, and we want to update the ByteBuffer's position
	 * to the position of this buffer.
	 */
	public void setPosition(ByteBuffer originalBuffer) {
		originalBuffer.position(originalBuffer.limit() - remaining());
	}
	
	@Override
	public ByteArray subBuffer(int startPosition, int length) {
		return new ByteArray(array, arrayOffset + startPosition, length);
	}
	
	/** Create a ByteBuffer from this buffer. */
	@Override
	public ByteBuffer toByteBuffer() {
		return ByteBuffer.wrap(array, currentOffset, length - (currentOffset - arrayOffset)).asReadOnlyBuffer();
	}
	
	@Override
	public void free() {
		// nothing of read-only
	}
	
	/** Writable ByteArray. */
	public static class Writable extends ByteArray implements DataBuffer.Writable, Bytes.Writable {
		
		/** Create a ByteArray writable from a ByteBuffer. */
		public static ByteArray.Writable fromByteBuffer(ByteBuffer b) {
			if (b.hasArray()) {
				ByteArray.Writable ba;
				if (b.isReadOnly()) {
					ba = new ByteArray.Writable(ByteArrayCache.getInstance().get(b.remaining(), true), 0, b.remaining(),  true);
					b.get(ba.getArray(), 0, b.remaining());
				} else {
					ba = new ByteArray.Writable(b.array(), b.arrayOffset(), b.arrayOffset() + b.position() + b.remaining(), true);
				}
				ba.currentOffset = b.arrayOffset() + b.position();
				return ba;
			}
			ByteArray.Writable ba = new ByteArray.Writable(ByteArrayCache.getInstance().get(b.remaining(), true), true);
			ba.length = b.remaining();
			b.get(ba.array, 0, ba.length);
			b.position(b.position() - ba.length);
			return ba;
		}
		
		private boolean free;
		
		/** Constructor. */
		public Writable(byte[] buffer, int offset, int length, boolean free) {
			super(buffer, offset, length);
			this.free = free;
		}
		
		/** Constructor. */
		public Writable(byte[] buffer, boolean free) {
			super(buffer, 0, buffer.length);
			this.free = free;
		}
		
		/** Constructor. */
		public Writable(ByteArray copy) {
			super(copy);
			this.free = false;
		}

		/** Create a copy of this buffer. */
		@Override
		public ByteArray.Writable duplicate() {
			return new ByteArray.Writable(this);
		}
		
		@Override
		public void put(byte b) {
			array[currentOffset++] = b;
		}
		
		@Override
		public void put(byte[] buffer, int offset, int length) {
			System.arraycopy(buffer, offset, array, currentOffset, length);
			currentOffset += length;
		}
		
		@Override
		public ByteArray.Writable subBuffer(int startPosition, int length) {
			return new ByteArray.Writable(array, arrayOffset + startPosition, length, false);
		}
		
		/** Create a ByteBuffer from this buffer. */
		@Override
		public ByteBuffer toByteBuffer() {
			return ByteBuffer.wrap(array, currentOffset, length - (currentOffset - arrayOffset));
		}

		@Override
		public void free() {
			if (!free)
				return;
			ByteArrayCache.getInstance().free(array);
			array = null;
		}
	}
}
