package net.lecousin.framework.concurrent.util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import net.lecousin.framework.concurrent.async.AsyncSupplier;

/**
 * List of producer as a single producer.
 * @param <T> type of data
 * @param <TError> type of error
 */
public class LinkedAsyncProducer<T, TError extends Exception> implements AsyncProducer<T, TError> {

	/** Constructor. */
	@SafeVarargs
	public LinkedAsyncProducer(AsyncProducer<T, TError>... producers) {
		this(new ArrayDeque<>(Arrays.asList(producers)));
	}
	
	/** Constructor. */
	public LinkedAsyncProducer(Deque<AsyncProducer<T, TError>> producers) {
		this.producers = producers;
	}
	
	private Deque<AsyncProducer<T, TError>> producers;
	private AsyncProducer<T, TError> currentProducer = null;
	
	@Override
	public AsyncSupplier<T, TError> produce() {
		do {
			if (currentProducer == null) {
				currentProducer = producers.pollFirst();
				if (currentProducer == null) {
					return new AsyncSupplier<>(null, null);
				}
			}
			AsyncSupplier<T, TError> production = currentProducer.produce();
			if (production.isDone()) {
				if (production.getResult() == null) {
					currentProducer = null;
					continue;
				}
				return production;
			}
			AsyncSupplier<T, TError> result = new AsyncSupplier<>();
			production.onDone(data -> {
				if (data == null) {
					currentProducer = null;
					if (producers.isEmpty())
						result.unblockSuccess(null);
					else
						produce().forward(result);
				} else {
					result.unblockSuccess(data);
				}
			}, result);
			return result;
		} while (true);
	}
	
}
