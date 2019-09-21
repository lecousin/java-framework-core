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
		list = new TurnArray<>(maxBuffersToKeep);
	}
	
	private int bufferSize;
	
	private TurnArray<ByteBuffer> list;
	
	/** Get a buffer. */
	public ByteBuffer getBuffer() {
		ByteBuffer buf = list.pollFirst();
		if (buf != null) {
			buf.clear();
			return buf;
		}
		return ByteBuffer.wrap(new byte[bufferSize]);
	}
	
	/** Release a buffer. */
	public void freeBuffer(ByteBuffer buf) {
		synchronized (list) {
			if (list.isFull()) return;
			list.addLast(buf);
		}
	}
	
}
