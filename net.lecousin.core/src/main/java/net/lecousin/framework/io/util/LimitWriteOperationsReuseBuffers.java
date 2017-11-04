package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * This class allows to queue write operations, but blocks if too many are waiting.
 * This can typically used in operations reading from an IO, and writing to another, when the amount of data can be large:
 * usually read operations are faster than write operations, and we need to avoid having too much buffers in memory waiting
 * to write.
 */
public class LimitWriteOperationsReuseBuffers {
	
	/** Constructor. */
	public LimitWriteOperationsReuseBuffers(IO.Writable io, int bufferSize, int maxOperations) {
		this.io = io;
		buffers = new Buffers(bufferSize, maxOperations);
		waiting = new TurnArray<>(maxOperations);
	}
	
	private IO.Writable io;
	private Buffers buffers;
	private TurnArray<Pair<ByteBuffer,AsyncWork<Integer,IOException>>> waiting;
	private SynchronizationPoint<NoException> lock = null;
	
	/**
	 * @return a buffer to put data to write.
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
	
	/**
	 * Queue the buffer to write. If there is no pending write, the write operation is started.
	 * If too many write operations are pending, the method is blocking.
	 * @param buffer the buffer to write.
	 */
	public AsyncWork<Integer,IOException> write(ByteBuffer buffer) throws IOException {
		do {
			synchronized (waiting) {
				if (waiting.isEmpty()) {
					return io.writeAsync(buffer, new RunnableWithParameter<Pair<Integer,IOException>>() {
						@Override
						public void run(Pair<Integer, IOException> param) {
							writeDone(buffer);
						}
					});
				}
				if (!waiting.isFull()) {
					AsyncWork<Integer,IOException> res = new AsyncWork<>();
					waiting.addLast(new Pair<>(buffer, res));
					return res;
				}
				if (lock != null)
					throw new IOException("Concurrent write");
				lock = new SynchronizationPoint<>();
			}
			lock.block(0);
		} while (true);
	}
	
	private void writeDone(ByteBuffer buffer) {
		buffers.freeBuffer(buffer);
		SynchronizationPoint<NoException> sp = null;
		synchronized (waiting) {
			Pair<ByteBuffer,AsyncWork<Integer,IOException>> b = waiting.pollFirst();
			if (b != null) {
				io.writeAsync(b.getValue1(), new RunnableWithParameter<Pair<Integer,IOException>>() {
					@Override
					public void run(Pair<Integer, IOException> param) {
						writeDone(b.getValue1());
					}
				}).listenInline(b.getValue2());
				if (lock != null) {
					sp = lock;
					lock = null;
				}
			}
		}
		if (sp != null)
			sp.unblock();
	}
	
	/** Return the last pending operation, or null. */
	public AsyncWork<Integer, IOException> getLastPendingOperation() {
		Pair<ByteBuffer,AsyncWork<Integer,IOException>> b = waiting.pollLast();
		if (b == null)
			return null;
		return b.getValue2();
	}

}
