package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.Pair;

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
					LCCore.getApplication().getConsole().out("write direct");
					lastWrite = io.writeAsync(buffer);
					lastWrite.listenInline(
						(nb) -> { writeDone(buffer, null); },
						(error) -> { writeDone(buffer, null); },
						(cancel) -> { writeDone(buffer, cancel); }
					);
					return lastWrite;
				}
				if (!waiting.isFull()) {
					LCCore.getApplication().getConsole().out("push write");
					AsyncWork<Integer,IOException> res = new AsyncWork<>();
					waiting.addLast(new Pair<>(buffer, res));
					lastWrite = res;
					return res;
				}
				LCCore.getApplication().getConsole().out("waiting has " + waiting.size());
				if (lock != null)
					throw new IOException("Concurrent write");
				lock = new SynchronizationPoint<>();
				lk = lock;
			}
			LCCore.getApplication().getConsole().out("block");
			lk.block(0);
			LCCore.getApplication().getConsole().out("unblocked");
		} while (true);
	}
	
	protected void writeDone(@SuppressWarnings("unused") ByteBuffer buffer, CancelException cancelled) {
		LCCore.getApplication().getConsole().out("writeDone");
		SynchronizationPoint<NoException> sp = null;
		synchronized (waiting) {
			Pair<ByteBuffer,AsyncWork<Integer,IOException>> b = waiting.pollFirst();
			LCCore.getApplication().getConsole().out("b = " + b);
			if (b != null) {
				if (lock != null) {
					sp = lock;
					lock = null;
				}
				if (cancelled != null) {
					while (b != null) {
						b.getValue2().cancel(cancelled);
						b = waiting.pollFirst();
					}
				} else {
					ByteBuffer buf = b.getValue1();
					AsyncWork<Integer, IOException> write = io.writeAsync(buf);
					Pair<ByteBuffer,AsyncWork<Integer,IOException>> bb = b;
					write.listenInline(
						(nb) -> {
							bb.getValue2().unblockSuccess(nb);
							writeDone(buf, null);
						},
						(error) -> {
							bb.getValue2().error(error);
							writeDone(buf, null);
						},
						(cancel) -> {
							bb.getValue2().cancel(cancel);
							writeDone(buf, cancel);
						}
					);
				}
			}
		}
		if (sp != null)
			sp.unblock();
	}
	
	/** Return the last pending operation, or null. */
	public AsyncWork<Integer, IOException> getLastPendingOperation() {
		return lastWrite.isUnblocked() ? null : lastWrite;
	}
	
	/** Same as getLastPendingOperation but never return null (return an unblocked synchronization point instead). */
	public ISynchronizationPoint<IOException> flush() {
		ISynchronizationPoint<IOException> sp = getLastPendingOperation();
		if (sp == null) sp = new SynchronizationPoint<>(true);
		return sp;
	}

}
