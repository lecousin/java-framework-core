package net.lecousin.framework.io.util;

/** Abstract base class for raw buffers. */
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public abstract class RawBuffer implements ArrayBuffer {

	public int arrayOffset;
	public int currentOffset;
	public int length;
	
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
