package net.lecousin.framework.io.util;

import java.nio.ByteBuffer;

import net.lecousin.framework.collections.TurnArray;

/**
 * Utility class to reuse buffers instead of re-allocating memory.
 */
public class Buffers {

	/** Constructor. */
	public Buffers(int bufferSize, int maxBuffersToKeep) {
		this.bufferSize = bufferSize;
		buffers = new TurnArray<>(maxBuffersToKeep);
	}
	
	private int bufferSize;
	
	private TurnArray<ByteBuffer> buffers;
	
	/** Get a buffer. */
	public ByteBuffer getBuffer() {
		ByteBuffer buf = buffers.removeFirst();
		if (buf != null) {
			buf.clear();
			return buf;
		}
		return ByteBuffer.wrap(new byte[bufferSize]);
	}
	
	/** Release a buffer. */
	public void freeBuffer(ByteBuffer buf) {
		synchronized (buffers) {
			if (buffers.isFull()) return;
			buffers.addLast(buf);
		}
	}
	
}
