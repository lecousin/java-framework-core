package net.lecousin.framework.concurrent.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.lecousin.framework.concurrent.async.AsyncSupplier;

/**
 * Consume data until end is detected.<br/>
 * Detection of end is implementation dependent.<br/>
 *
 * @param <T> type of data
 * @param <TError> type of error
 */
public interface PartialAsyncConsumer<T, TError extends Exception> {

	/** Consume data, return true if end is detected or false if more data is expected. */
	AsyncSupplier<Boolean, TError> consume(T data);
	
	/** Return true if more data is expected, or false if end has been reached. */
	boolean isExpectingData();
	
	/** Convert this consumer. */
	default <Target, TErrorTarget extends Exception> PartialAsyncConsumer<Target, TErrorTarget> convert(
		Function<Target, T> convertAtTheBeginning,
		BiConsumer<T, Target> updateAtTheEnd,
		Function<TError, TErrorTarget> errorConverter
	) {
		return new Converter<>(this, convertAtTheBeginning, updateAtTheEnd, errorConverter);
	}
	
	/** Convert error type of this consumer. */
	default <TErrorTarget extends Exception> PartialAsyncConsumer<T, TErrorTarget> convertError(
		Function<TError, TErrorTarget> errorConverter
	) {
		return new ErrorConverter<>(this, errorConverter);
	}
	
	/**
	 * Multiple consumers, one by one.
	 *
	 * @param <T> type of data
	 * @param <TError> type of error
	 */
	public static class ConsumerQueue<T, TError extends Exception> implements PartialAsyncConsumer<T, TError> {
		
		private PartialAsyncConsumer<T, TError> currentConsumer;
		protected Queue<PartialAsyncConsumer<T, TError>> queue;
		
		/** Constructor. */
		public ConsumerQueue(Queue<PartialAsyncConsumer<T, TError>> consumers) {
			this.queue = consumers;
			nextConsumer();
		}
		
		/** Constructor. */
		@SafeVarargs
		public ConsumerQueue(PartialAsyncConsumer<T, TError>... consumers) {
			this.queue = new LinkedList<>();
			Collections.addAll(queue, consumers);
			nextConsumer();
		}
		
		/** Constructor for extending classes that MUST call the method {@link #nextConsumer()} once the queue is filled. */
		protected ConsumerQueue() {
			this.queue = new LinkedList<>();
		}
		
		protected void nextConsumer() {
			do {
				currentConsumer = queue.poll();
				if (currentConsumer == null || currentConsumer.isExpectingData())
					return;
			} while (true);
		}
		
		@Override
		public AsyncSupplier<Boolean, TError> consume(T data) {
			AsyncSupplier<Boolean, TError> result = new AsyncSupplier<>();
			if (currentConsumer == null) {
				result.unblockSuccess(Boolean.TRUE);
				return result;
			}
			currentConsumer.consume(data).onDone(endOfConsumer -> {
				if (!endOfConsumer.booleanValue()) {
					result.unblockSuccess(Boolean.FALSE);
					return;
				}
				nextConsumer();
				consume(data).forward(result);
			}, result);
			return result;
		}
		
		@Override
		public boolean isExpectingData() {
			return currentConsumer != null;
		}
		
	}
	
	/** Convert a PartialAsyncConsumer to another type.
	 * @param <T> type of source data
	 * @param <TError> type of source error
	 * @param <Target> type of converted data
	 * @param <TErrorTarget> type of converted error
	 */
	public static class Converter<T, TError extends Exception, Target, TErrorTarget extends Exception>
	implements PartialAsyncConsumer<T, TError> {
	
		/** Constructor. */
		public Converter(
			PartialAsyncConsumer<Target, TErrorTarget> consumer,
			Function<T, Target> convertAtTheBeginning,
			BiConsumer<Target, T> updateAtTheEnd,
			Function<TErrorTarget, TError> errorConverter
		) {
			this.consumer = consumer;
			this.convertAtTheBeginning = convertAtTheBeginning;
			this.updateAtTheEnd = updateAtTheEnd;
			this.errorConverter = errorConverter;
		}
		
		private PartialAsyncConsumer<Target, TErrorTarget> consumer;
		private Function<T, Target> convertAtTheBeginning;
		private BiConsumer<Target, T> updateAtTheEnd;
		private Function<TErrorTarget, TError> errorConverter;
		
		@Override
		public AsyncSupplier<Boolean, TError> consume(T data) {
			Target target = convertAtTheBeginning.apply(data);
			AsyncSupplier<Boolean, TErrorTarget> consumption = consumer.consume(target);
			AsyncSupplier<Boolean, TError> result = new AsyncSupplier<>();
			consumption.onDone(() -> {
				if (consumption.hasError())
					result.error(errorConverter.apply(consumption.getError()));
				else if (consumption.isCancelled())
					result.cancel(consumption.getCancelEvent());
				else {
					updateAtTheEnd.accept(target, data);
					result.unblockSuccess(consumption.getResult());
				}
			});
			return result;
		}
		
		@Override
		public boolean isExpectingData() {
			return consumer.isExpectingData();
		}
		
	}
	
	/**
	 * Wrap an existing consumer and convert its error type.
	 * @param <T> type of data
	 * @param <TErrorSource> type of error
	 * @param <TErrorTarget> source type of error
	 */
	public static class ErrorConverter<T, TErrorSource extends Exception, TErrorTarget extends Exception>
	implements PartialAsyncConsumer<T, TErrorTarget> {
		
		/** Constructor. */
		public ErrorConverter(PartialAsyncConsumer<T, TErrorSource> consumer, Function<TErrorSource, TErrorTarget> errorConverter) {
			this.consumer = consumer;
			this.errorConverter = errorConverter;
		}
		
		private PartialAsyncConsumer<T, TErrorSource> consumer;
		private Function<TErrorSource, TErrorTarget> errorConverter;
		
		@Override
		public AsyncSupplier<Boolean, TErrorTarget> consume(T data) {
			AsyncSupplier<Boolean, TErrorTarget> result = new AsyncSupplier<>();
			consumer.consume(data).forward(result, errorConverter);
			return result;
		}
		
		@Override
		public boolean isExpectingData() {
			return consumer.isExpectingData();
		}
		
	}
	
}
