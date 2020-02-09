package net.lecousin.framework.io.data;

import java.nio.ByteBuffer;

/** Utility class that contains a byte array with public attributes. */
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
			b.position(b.position() - length);
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
		return ByteBuffer.wrap(array, currentOffset, length - (currentOffset - arrayOffset));
	}
	
	/** Used when a ByteBuffer as been converted into a RawByteBuffer, and we want to update the ByteBuffer's position
	 * to the position of this buffer.
	 */
	public void setPosition(ByteBuffer originalBuffer) {
		originalBuffer.position(currentOffset - arrayOffset);
	}
	
	@Override
	public RawByteBuffer subBuffer(int startPosition, int length) {
		return new RawByteBuffer(array, arrayOffset + startPosition, length);
	}
}
