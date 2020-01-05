package net.lecousin.framework.io.data;

/** Abstract base class for raw buffers.
 * @param <T> array type
 */
@SuppressWarnings({"squid:ClassVariableVisibilityCheck","squid:S1845"})
public abstract class RawBuffer<T> implements DataBuffer {

	public T array;
	public int arrayOffset;
	public int currentOffset;
	public int length;

	/** Constructor. */
	public RawBuffer(T buffer, int offset, int length) {
		this.array = buffer;
		this.arrayOffset = offset;
		this.currentOffset = offset;
		this.length = length;
	}
	
	/** Constructor. */
	public RawBuffer(RawBuffer<T> copy) {
		this(copy.array, copy.arrayOffset, copy.length);
		this.currentOffset = copy.currentOffset;
	}
	
	protected RawBuffer() {
	}

	@Override
	public int length() {
		return length;
	}
	
	@Override
	public int remaining() {
		return length - (currentOffset - arrayOffset);
	}
	
	@Override
	public boolean hasRemaining() {
		return currentOffset - arrayOffset < length;
	}

	@Override
	public int position() {
		return currentOffset - arrayOffset;
	}
	
	@Override
	public void setPosition(int position) {
		currentOffset = arrayOffset + position;
	}
	
	@Override
	public void moveForward(int offset) {
		currentOffset += offset;
	}
	
	@Override
	public void goToEnd() {
		currentOffset = arrayOffset + length;
	}
	
	/** Flip this buffer. */
	public void flip() {
		length = currentOffset - arrayOffset;
		currentOffset = arrayOffset;
	}

}
