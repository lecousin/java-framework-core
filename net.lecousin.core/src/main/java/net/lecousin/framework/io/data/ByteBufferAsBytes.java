package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;

/** Implementation of Bytes wrapping a ByteBuffer. */
public class ByteBufferAsBytes implements Bytes.Readable, Bytes.Writable {

	/** Constructor. */
	public ByteBufferAsBytes(ByteBuffer buffer) {
		this.buffer = buffer;
	}
	
	private ByteBuffer buffer;

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
	public void put(byte b) {
		buffer.put(b);
	}

	@Override
	public void put(byte[] buffer, int offset, int length) {
		this.buffer.put(buffer, offset, length);
	}

	@Override
	public ByteBufferAsBytes subBuffer(int startPosition, int length) {
		ByteBuffer dup = buffer.duplicate();
		dup.position(startPosition);
		dup.limit(startPosition + length);
		return new ByteBufferAsBytes(dup.slice());
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
	
}
