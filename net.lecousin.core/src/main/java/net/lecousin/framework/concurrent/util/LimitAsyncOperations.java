package net.lecousin.framework.concurrent.util;

import java.io.IOException;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.Pair;

/**
 * This class allows to queue asynchronous operations, but blocks if too many are waiting.
 * This can typically used in operations receiving data, and writing to an IO, when the amount of data can be large:
 * if we receive more data than the write operations can do, and we need to avoid having too much buffers in memory waiting
 * to write.
 *
 * @param <InputType> input data
 * @param <OutputResultType> output result
 * @param <OutputErrorType> error
 */
public class LimitAsyncOperations<InputType, OutputResultType, OutputErrorType extends Exception> {
	
	/** Constructor. */
	public LimitAsyncOperations(int maxOperations, Executor<InputType, OutputResultType, OutputErrorType> executor) {
		waiting = new TurnArray<>(maxOperations);
		this.executor = executor;
	}
	
	private Executor<InputType, OutputResultType, OutputErrorType> executor;
	private TurnArray<Pair<InputType,AsyncWork<OutputResultType,OutputErrorType>>> waiting;
	private SynchronizationPoint<NoException> lock = null;
	private AsyncWork<OutputResultType, OutputErrorType> lastWrite = new AsyncWork<>(null, null);
	
	/**
	 * Executor of write operation.
	 *
	 * @param <InputType> input data
	 * @param <OutputResultType> output result
	 * @param <OutputErrorType> error
	 */
	public static interface Executor<InputType, OutputResultType, OutputErrorType extends Exception> {
		/** Launch asynchronous operation. */
		AsyncWork<OutputResultType, OutputErrorType> execute(InputType data);
	}
	
	/**
	 * Queue the data to write. If there is no pending write, the write operation is started.
	 * If too many write operations are pending, the method is blocking.
	 * @param data the data to write.
	 */
	public AsyncWork<OutputResultType, OutputErrorType> write(InputType data) throws IOException {
		do {
			SynchronizationPoint<NoException> lk;
			synchronized (waiting) {
				if (lastWrite.isCancelled()) return lastWrite;
				if (waiting.isEmpty() && lastWrite.isUnblocked()) {
					AsyncWork<OutputResultType, OutputErrorType> res = executor.execute(data);
					AsyncWork<OutputResultType, OutputErrorType> done = new AsyncWork<>();
					lastWrite = done;
					res.listenInline(
						(nb) -> {
							writeDone(data, null);
							done.unblockSuccess(nb);
						},
						(error) -> {
							writeDone(data, null);
							done.error(error);
						},
						(cancel) -> {
							writeDone(data, cancel);
							done.cancel(cancel);
						}
					);
					return lastWrite;
				}
				if (!waiting.isFull()) {
					AsyncWork<OutputResultType, OutputErrorType> res = new AsyncWork<>();
					waiting.addLast(new Pair<>(data, res));
					lastWrite = res;
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
	
	protected void writeDone(@SuppressWarnings("unused") InputType data, CancelException cancelled) {
		SynchronizationPoint<NoException> sp = null;
		synchronized (waiting) {
			Pair<InputType, AsyncWork<OutputResultType, OutputErrorType>> b = waiting.pollFirst();
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
					InputType newData = b.getValue1();
					AsyncWork<OutputResultType, OutputErrorType> write = executor.execute(newData);
					Pair<InputType, AsyncWork<OutputResultType, OutputErrorType>> bb = b;
					write.listenInline(
						(nb) -> {
							writeDone(newData, null);
							bb.getValue2().unblockSuccess(nb);
						},
						(error) -> {
							writeDone(newData, null);
							bb.getValue2().error(error);
						},
						(cancel) -> {
							writeDone(newData, cancel);
							bb.getValue2().cancel(cancel);
						}
					);
				}
			}
		}
		if (sp != null)
			sp.unblock();
	}
	
	/** Return the last pending operation, or null. */
	public AsyncWork<OutputResultType, OutputErrorType> getLastPendingOperation() {
		return lastWrite.isUnblocked() ? null : lastWrite;
	}
	
	/** Same as getLastPendingOperation but never return null (return an unblocked synchronization point instead). */
	public ISynchronizationPoint<OutputErrorType> flush() {
		ISynchronizationPoint<OutputErrorType> sp = getLastPendingOperation();
		if (sp == null) sp = new SynchronizationPoint<>(true);
		return sp;
	}

}
