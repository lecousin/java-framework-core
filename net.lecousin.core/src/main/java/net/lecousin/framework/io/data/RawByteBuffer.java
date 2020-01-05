package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;

/** Utility class that contains a byte array with public attributes. */
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class RawByteBuffer extends RawBuffer<byte[]> implements Bytes.Readable, Bytes.Writable {

	/** Constructor. */
	public RawByteBuffer(byte[] buffer, int offset, int length) {
		super(buffer, offset, length);
	}
	
	/** Constructor. */
	public RawByteBuffer(byte[] buffer) {
		super(buffer, 0, buffer.length);
	}
	
	/** Constructor. */
	public RawByteBuffer(RawByteBuffer copy) {
		super(copy);
	}
	
	/** Constructor. */
	public RawByteBuffer(ByteBuffer b) {
		if (b.hasArray()) {
			array = b.array();
			arrayOffset = b.arrayOffset();
			currentOffset = arrayOffset + b.position();
			length = currentOffset + b.remaining();
		} else {
			array = new byte[b.remaining()];
			arrayOffset = currentOffset = 0;
			length = array.length;
			b.get(array);
		}
	}
	
	/** Create a copy of this buffer. */
	public RawByteBuffer duplicate() {
		return new RawByteBuffer(this);
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
	
	@Override
	public void put(byte b) {
		array[currentOffset++] = b;
	}
	
	@Override
	public void put(byte[] buffer, int offset, int length) {
		System.arraycopy(buffer, offset, array, currentOffset, length);
		currentOffset += length;
	}
	
	/** Create a ByteBuffer from this buffer. */
	@Override
	public ByteBuffer toByteBuffer() {
		ByteBuffer b = ByteBuffer.wrap(array, arrayOffset, length);
		b.position(currentOffset - arrayOffset);
		return b;
	}
	
	@Override
	public RawByteBuffer subBuffer(int startPosition, int length) {
		return new RawByteBuffer(array, arrayOffset + startPosition, length);
	}
}
