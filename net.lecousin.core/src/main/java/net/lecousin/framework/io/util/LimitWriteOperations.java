package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
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
public class LimitWriteOperations {
	
	/** Constructor. */
	public LimitWriteOperations(IO.Writable io, int maxOperations) {
		this.io = io;
		waiting = new TurnArray<>(maxOperations);
	}
	
	private IO.Writable io;
	private TurnArray<Pair<ByteBuffer,AsyncWork<Integer,IOException>>> waiting;
	private SynchronizationPoint<NoException> lock = null;
	private AsyncWork<Integer, IOException> lastWrite = new AsyncWork<>(Integer.valueOf(0), null);
	
	/**
	 * Queue the buffer to write. If there is no pending write, the write operation is started.
	 * If too many write operations are pending, the method is blocking.
	 * @param buffer the buffer to write.
	 */
	public AsyncWork<Integer,IOException> write(ByteBuffer buffer) throws IOException {
		do {
			SynchronizationPoint<NoException> lk;
			synchronized (waiting) {
				if (lastWrite.isCancelled()) return lastWrite;
				if (waiting.isEmpty() && lastWrite.isUnblocked()) {
					lastWrite = io.writeAsync(buffer, new RunnableWithParameter<Pair<Integer,IOException>>() {
						@Override
						public void run(Pair<Integer, IOException> param) {
							writeDone();
						}
					});
					lastWrite.onCancel((cancel) -> {
						writeDone();
					});
					return lastWrite;
				}
				if (!waiting.isFull()) {
					AsyncWork<Integer,IOException> res = new AsyncWork<>();
					waiting.addLast(new Pair<>(buffer, res));
					return res;
				}
				if (lock != null)
					throw new IOException("Concurrent write");
				lock = new SynchronizationPoint<>();
				lk = lock;
			}
			lk.block(0);
		} while (true);
	}
	
	private void writeDone() {
		SynchronizationPoint<NoException> sp = null;
		synchronized (waiting) {
			Pair<ByteBuffer,AsyncWork<Integer,IOException>> b = waiting.pollFirst();
			if (b != null) {
				if (lock != null) {
					sp = lock;
					lock = null;
				}
				if (lastWrite.isCancelled()) {
					while (b != null) {
						b.getValue2().cancel(lastWrite.getCancelEvent());
						b = waiting.pollFirst();
					}
				} else {
					lastWrite = io.writeAsync(b.getValue1(), new RunnableWithParameter<Pair<Integer,IOException>>() {
						@Override
						public void run(Pair<Integer, IOException> param) {
							writeDone();
						}
					});
					lastWrite.onCancel((cancel) -> { writeDone(); });
					lastWrite.listenInline(b.getValue2());
				}
			}
		}
		if (sp != null)
			sp.unblock();
	}
	
	/** Return the last pending operation, or null. */
	public AsyncWork<Integer, IOException> getLastPendingOperation() {
		Pair<ByteBuffer,AsyncWork<Integer,IOException>> b = waiting.peekLast();
		if (b == null)
			return lastWrite.isUnblocked() ? null : lastWrite;
		return b.getValue2();
	}
	
	/** Same as getLastPendingOperation but never return null (return an unblocked synchronization point instead). */
	public ISynchronizationPoint<IOException> flush() {
		ISynchronizationPoint<IOException> sp = getLastPendingOperation();
		if (sp == null) sp = new SynchronizationPoint<>(true);
		return sp;
	}

}
