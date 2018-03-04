package net.lecousin.framework.io.util;

import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.io.IO;

/**
 * This class allows to queue write operations, but blocks if too many are waiting.
 * This can typically used in operations reading from an IO, and writing to another, when the amount of data can be large:
 * usually read operations are faster than write operations, and we need to avoid having too much buffers in memory waiting
 * to write.
 */
public class LimitWriteOperationsReuseBuffers extends LimitWriteOperations {
	
	/** Constructor. */
	public LimitWriteOperationsReuseBuffers(IO.Writable io, int bufferSize, int maxOperations) {
		super(io, maxOperations);
		buffers = new Buffers(bufferSize, maxOperations);
	}
	
	private Buffers buffers;
	
	/**
	 * Return a buffer to put data to write.
	 */
	public ByteBuffer getBuffer() {
		return buffers.getBuffer();
	}
	
	/** Must be called only if the user has not been used for a write operation,
	 * else it will be automatically free when write operation is done.
	 */
	public void freeBuffer(ByteBuffer buffer) {
		buffers.freeBuffer(buffer);
	}
	
	@Override
	protected void writeDone(ByteBuffer buffer, CancelException cancelled) {
		buffers.freeBuffer(buffer);
		super.writeDone(buffer, cancelled);
	}

}
