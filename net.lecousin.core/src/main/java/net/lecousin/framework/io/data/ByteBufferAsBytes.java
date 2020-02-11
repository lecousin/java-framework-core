package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;

import net.lecousin.framework.memory.ByteArrayCache;

/** Implementation of Bytes wrapping a ByteBuffer. */
public class ByteBufferAsBytes implements Bytes.Readable {
	
	protected ByteBuffer buffer;
	protected boolean free;

	/** Get a ByteBufferAsBytes, writable if the buffer is not read-only. */
	public static ByteBufferAsBytes create(ByteBuffer buffer, boolean freeArray) {
		if (buffer.isReadOnly())
			return new ByteBufferAsBytes(buffer, freeArray);
		return new Writable(buffer, freeArray);
	}
	
	/** Constructor. */
	private ByteBufferAsBytes(ByteBuffer buffer, boolean freeArray) {
		this.buffer = buffer;
		this.free = freeArray;
	}

	@Override
	public ByteBuffer toByteBuffer() {
		return buffer;
	}

	@Override
	public int length() {
		return buffer.limit();
	}

	@Override
	public int position() {
		return buffer.position();
	}

	@Override
	public void setPosition(int position) {
		buffer.position(position);
	}

	@Override
	public int remaining() {
		return buffer.remaining();
	}

	@Override
	public ByteBufferAsBytes subBuffer(int startPosition, int length) {
		ByteBuffer dup = buffer.duplicate();
		dup.position(startPosition);
		dup.limit(startPosition + length);
		return new ByteBufferAsBytes(dup.slice(), false);
	}

	@Override
	public byte get() {
		return buffer.get();
	}

	@Override
	public void get(byte[] buffer, int offset, int length) {
		this.buffer.get(buffer, offset, length);
	}

	@Override
	public byte getForward(int offset) {
		return buffer.get(buffer.position() + offset);
	}
	
	@Override
	public void free() {
		if (free && buffer.hasArray())
			ByteArrayCache.getInstance().free(buffer.array());
		buffer = null;
	}
	
	/** ByteBuffer writable to Bytes writable. */
	public static class Writable extends ByteBufferAsBytes implements Bytes.Writable {
		
		/** Constructor. */
		private Writable(ByteBuffer buffer, boolean freeArray) {
			super(buffer, freeArray);
		}

		@Override
		public void put(byte b) {
			buffer.put(b);
		}

		@Override
		public void put(byte[] buffer, int offset, int length) {
			this.buffer.put(buffer, offset, length);
		}
		
		@Override
		public ByteBufferAsBytes.Writable subBuffer(int startPosition, int length) {
			ByteBuffer dup = buffer.duplicate();
			dup.position(startPosition);
			dup.limit(startPosition + length);
			return new ByteBufferAsBytes.Writable(dup.slice(), false);
		}
	}
	
}
