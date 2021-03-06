package net.lecousin.framework.concurrent.util;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.util.Pair;

/**
 * AsyncConsumer with a waiting queue.
 * When new data comes, it is queue and returned as it is consumed but not released.
 * 
 * @param <T> type of data
 * @param <TError> type of error
 */
public class BufferedAsyncConsumer<T, TError extends Exception> implements AsyncConsumer<T, TError> {

	/**
	 * Constructor.
	 * @param nbPending maximum number of pending data. Minimum value is 2.
	 * @param consumer the consumer
	 */
	public BufferedAsyncConsumer(int nbPending, AsyncConsumer<T, TError> consumer) {
		this.consumer = consumer;
		if (nbPending < 2) nbPending = 2;
		queue = new TurnArray<>(nbPending - 1);
	}
	
	protected AsyncConsumer<T, TError> consumer;
	protected TurnArray<T> queue;
	protected IAsync<TError> lastOperation;
	protected Pair<T, Async<TError>> waiting;
	protected Async<TError> end;
	protected TError error;
	
	@Override
	public IAsync<TError> consume(T data) {
		synchronized (queue) {
			if (error != null)
				return new Async<>(error);
			if (lastOperation == null) {
				// nothing currently consumed, start it
				lastOperation = consumer.consume(data);
				lastOperation.onDone(this::nextPending, this::error, c -> { /* ignore */ });
				return new Async<>(true);
			}
			if (!queue.isFull()) {
				queue.add(data);
				return new Async<>(true);
			}
			waiting = new Pair<>(data, new Async<>());
			return waiting.getValue2();
		}
	}
	
	private void nextPending() {
		Task.cpu("Consume next buffer", Task.Priority.NORMAL, t -> {
			Async<TError> unblock = null;
			synchronized (queue) {
				if (error != null) {
					consumer.error(error);
					return null;
				}
				T next = queue.pollFirst();
				if (next != null) {
					lastOperation = consumer.consume(next);
					if (waiting != null) {
						queue.add(waiting.getValue1());
						unblock = waiting.getValue2();
						waiting = null;
					}
					lastOperation.onDone(this::nextPending, this::error, c -> { /* ignore */ });
				} else {
					lastOperation = null;
					if (end != null)
						consumer.end().onDone(end);
				}
			}
			if (unblock != null)
				unblock.unblock();
			return null;
		}).start();
	}

	@Override
	public IAsync<TError> end() {
		synchronized (queue) {
			if (error != null)
				return new Async<>(error);
			if (lastOperation == null && queue.isEmpty())
				return consumer.end();
			end = new Async<>();
			return end;
		}
	}
	
	@Override
	public void error(TError error) {
		synchronized (queue) {
			if (this.error != null)
				return;
			this.error = error;
			while (!queue.isEmpty())
				queue.pollFirst();
			if (waiting != null)
				waiting.getValue2().error(error);
			waiting = null;
			if (lastOperation == null)
				consumer.error(error);
			else
				lastOperation = null;
			if (end != null)
				end.error(error);
		}
	}
	
}
