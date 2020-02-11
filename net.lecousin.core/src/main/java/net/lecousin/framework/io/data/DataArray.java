package net.lecousin.framework.io.data;

/** Abstract base class for an array of data.
 * @param <T> array type
 */
@SuppressWarnings({"squid:ClassVariableVisibilityCheck","squid:S1845"})
public abstract class DataArray<T> implements DataBuffer.Readable {

	protected T array;
	protected int arrayOffset;
	protected int currentOffset;
	protected int length;

	/** Constructor. */
	protected DataArray(T buffer, int offset, int length) {
		this.array = buffer;
		this.arrayOffset = offset;
		this.currentOffset = offset;
		this.length = length;
	}
	
	/** Constructor. */
	protected DataArray(DataArray<T> copy) {
		this(copy.array, copy.arrayOffset, copy.length);
		this.currentOffset = copy.currentOffset;
	}
	
	protected DataArray() {
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
	
	/** The start of this buffer is moved to the current position, so the position becomes 0. */
	public void slice() {
		length -= currentOffset - arrayOffset;
		arrayOffset = currentOffset;
	}
	
	public T getArray() {
		return array;
	}
	
	public int getCurrentArrayOffset() {
		return currentOffset;
	}

}
