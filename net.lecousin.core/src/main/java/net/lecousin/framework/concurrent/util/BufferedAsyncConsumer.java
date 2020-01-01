package net.lecousin.framework.concurrent.util;

import java.util.function.Consumer;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.Triple;

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
	protected TurnArray<Pair<T, Consumer<T>>> queue;
	protected IAsync<TError> lastOperation;
	protected Triple<T, Consumer<T>, Async<TError>> waiting;
	protected Async<TError> end;
	protected TError error;
	
	@Override
	public IAsync<TError> consume(T data, Consumer<T> onDataRelease) {
		synchronized (queue) {
			if (lastOperation == null) {
				// nothing currently consumed, start it
				lastOperation = consumer.consume(data, onDataRelease);
				lastOperation.onDone(this::nextPending, this::error, c -> { /* ignore */ });
				return new Async<>(true);
			}
			if (!queue.isFull()) {
				queue.add(new Pair<>(data, onDataRelease));
				return new Async<>(true);
			}
			waiting = new Triple<>(data, onDataRelease, new Async<>());
			return waiting.getValue3();
		}
	}
	
	private void nextPending() {
		synchronized (queue) {
			if (error != null) {
				consumer.error(error);
				return;
			}
			Pair<T, Consumer<T>> next = queue.pollFirst();
			if (next != null) {
				lastOperation = consumer.consume(next.getValue1(), next.getValue2());
				if (waiting != null) {
					queue.add(new Pair<>(waiting.getValue1(), waiting.getValue2()));
					waiting.getValue3().unblock();
					waiting = null;
				}
				lastOperation.onDone(this::nextPending, this::error, c -> { /* ignore */ });
			} else {
				lastOperation = null;
				if (end != null)
					consumer.end().onDone(end);
			}
		}
	}

	@Override
	public IAsync<TError> end() {
		synchronized (queue) {
			if (queue.isEmpty())
				return consumer.end();
			end = new Async<>();
			return end;
		}
	}
	
	@Override
	public void error(TError error) {
		synchronized (queue) {
			this.error = error;
			if (lastOperation == null)
				consumer.error(error);
		}
	}
	
}
