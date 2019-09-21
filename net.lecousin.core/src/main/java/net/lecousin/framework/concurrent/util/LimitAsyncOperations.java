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
	private CancelException cancelled = null;
	private OutputErrorType error = null;
	private boolean isReady = true;
	
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
				// if cancelled or errored, return immediately
				if (error != null) return new AsyncWork<>(null, error);
				if (cancelled != null) return new AsyncWork<>(null, null, cancelled);
				
				AsyncWork<OutputResultType, OutputErrorType> op;
				// if ready, write immediately
				if (isReady) {
					isReady = false;
					op = lastWrite = executor.execute(data);
					lastWrite.listenInline(new WriteListener(data, op, null));
					return op;
				}
				// not ready
				if (!waiting.isFull()) {
					op = new AsyncWork<>();
					waiting.addLast(new Pair<>(data, op));
					lastWrite = op;
					return op;
				}
				// full
				if (lock != null)
					throw new IOException("Concurrent write");
				lock = new SynchronizationPoint<>();
				lk = lock;
			}
			lk.block(0);
		} while (true);
	}

	private class WriteListener implements Runnable {
		public WriteListener(
			InputType data, AsyncWork<OutputResultType, OutputErrorType> op, AsyncWork<OutputResultType, OutputErrorType> result
		) {
			this.data = data;
			this.op = op;
			this.result = result;
		}
		
		private InputType data;
		private AsyncWork<OutputResultType, OutputErrorType> op;
		private AsyncWork<OutputResultType, OutputErrorType> result;
		
		@Override
		public void run() {
			SynchronizationPoint<NoException> lk = null;
			synchronized (waiting) {
				if (lock != null) {
					lk = lock;
					lock = null;
				}
				if (op.hasError()) error = op.getError();
				else if (op.isCancelled()) cancelled = op.getCancelEvent();
				else {
					Pair<InputType, AsyncWork<OutputResultType, OutputErrorType>> b = waiting.pollFirst();
					if (b != null) {
						// something is waiting
						AsyncWork<OutputResultType, OutputErrorType> newOp = executor.execute(b.getValue1());
						lastWrite = newOp;
						newOp.listenInline(new WriteListener(b.getValue1(), newOp, b.getValue2()));
					} else {
						isReady = true;
					}
				}
			}
			if (result != null)
				op.listenInline(result);
			if (lk != null)
				lk.unblock();
			writeDone(data, op);
		}
	}
	
	@SuppressWarnings("unused")
	protected void writeDone(InputType data, AsyncWork<OutputResultType, OutputErrorType> result) {
		// to be overriden if needed
	}
	
	/** Return the last pending operation, or null. */
	public AsyncWork<OutputResultType, OutputErrorType> getLastPendingOperation() {
		return lastWrite.isUnblocked() ? null : lastWrite;
	}
	
	/** Same as getLastPendingOperation but never return null (return an unblocked synchronization point instead). */
	public ISynchronizationPoint<OutputErrorType> flush() {
		SynchronizationPoint<OutputErrorType> sp = new SynchronizationPoint<>();
		Runnable callback = new Runnable() {
			@Override
			public void run() {
				AsyncWork<OutputResultType, OutputErrorType> last = null;
				synchronized (waiting) {
					if (error != null) sp.error(error);
					else if (cancelled != null) sp.cancel(cancelled);
					else if (isReady) sp.unblock();
					else last = lastWrite;
				}
				if (last != null)
					last.listenInline(this);
			}
		};
		callback.run();
		return sp;
	}

}
