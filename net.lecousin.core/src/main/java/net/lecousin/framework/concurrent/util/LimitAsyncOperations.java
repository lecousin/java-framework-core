package net.lecousin.framework.concurrent.util;

import java.io.IOException;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.Pair;

/**
 * This class allows to queue asynchronous operations, but blocks if too many are waiting.
 * This can typically be used in operations receiving data, and writing to an IO, when the amount of data can be large:
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
	private TurnArray<Pair<InputType,AsyncSupplier<OutputResultType,OutputErrorType>>> waiting;
	private Async<NoException> lock = null;
	private AsyncSupplier<OutputResultType, OutputErrorType> lastWrite = new AsyncSupplier<>(null, null);
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
		AsyncSupplier<OutputResultType, OutputErrorType> execute(InputType data);
	}
	
	/**
	 * Queue the data to write. If there is no pending write, the write operation is started.
	 * If too many write operations are pending, the method is blocking.
	 * @param data the data to write.
	 */
	public AsyncSupplier<OutputResultType, OutputErrorType> write(InputType data) throws IOException {
		do {
			Async<NoException> lk;
			synchronized (waiting) {
				// if cancelled or errored, return immediately
				if (error != null) return new AsyncSupplier<>(null, error);
				if (cancelled != null) return new AsyncSupplier<>(null, null, cancelled);
				
				AsyncSupplier<OutputResultType, OutputErrorType> op;
				// if ready, write immediately
				if (isReady) {
					isReady = false;
					op = lastWrite = executor.execute(data);
					lastWrite.onDone(new WriteListener(data, op, null));
					return op;
				}
				// not ready
				if (!waiting.isFull()) {
					op = new AsyncSupplier<>();
					waiting.addLast(new Pair<>(data, op));
					lastWrite = op;
					return op;
				}
				// full
				if (lock != null)
					throw new IOException("Concurrent write");
				lock = new Async<>();
				lk = lock;
			}
			lk.block(0);
		} while (true);
	}

	private class WriteListener implements Runnable {
		public WriteListener(
			InputType data, AsyncSupplier<OutputResultType, OutputErrorType> op, AsyncSupplier<OutputResultType, OutputErrorType> result
		) {
			this.data = data;
			this.op = op;
			this.result = result;
		}
		
		private InputType data;
		private AsyncSupplier<OutputResultType, OutputErrorType> op;
		private AsyncSupplier<OutputResultType, OutputErrorType> result;
		
		@Override
		public void run() {
			Async<NoException> lk = null;
			synchronized (waiting) {
				if (lock != null) {
					lk = lock;
					lock = null;
				}
				if (op.hasError()) error = op.getError();
				else if (op.isCancelled()) cancelled = op.getCancelEvent();
				else {
					Pair<InputType, AsyncSupplier<OutputResultType, OutputErrorType>> b = waiting.pollFirst();
					if (b != null) {
						// something is waiting
						AsyncSupplier<OutputResultType, OutputErrorType> newOp = executor.execute(b.getValue1());
						lastWrite = newOp;
						newOp.onDone(new WriteListener(b.getValue1(), newOp, b.getValue2()));
					} else {
						isReady = true;
					}
				}
			}
			if (result != null)
				op.forward(result);
			if (lk != null)
				lk.unblock();
			writeDone(data, op);
		}
	}
	
	@SuppressWarnings("unused")
	protected void writeDone(InputType data, AsyncSupplier<OutputResultType, OutputErrorType> result) {
		// to be overriden if needed
	}
	
	/** Return the last pending operation, or null. */
	public AsyncSupplier<OutputResultType, OutputErrorType> getLastPendingOperation() {
		return lastWrite.isDone() ? null : lastWrite;
	}
	
	/** Same as getLastPendingOperation but never return null (return an unblocked synchronization point instead). */
	public IAsync<OutputErrorType> flush() {
		Async<OutputErrorType> sp = new Async<>();
		Runnable callback = new Runnable() {
			@Override
			public void run() {
				AsyncSupplier<OutputResultType, OutputErrorType> last = null;
				synchronized (waiting) {
					if (error != null) sp.error(error);
					else if (cancelled != null) sp.cancel(cancelled);
					else if (isReady) sp.unblock();
					else last = lastWrite;
				}
				if (last != null)
					last.onDone(this);
			}
		};
		callback.run();
		return sp;
	}

}
