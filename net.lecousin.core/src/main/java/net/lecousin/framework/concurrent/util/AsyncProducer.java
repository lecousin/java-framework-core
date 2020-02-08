package net.lecousin.framework.concurrent.util;

import java.util.function.Function;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.util.Runnables.ConsumerThrows;

/** Asynchronous supplier of data.
 * @param <T> type of data produced
 * @param <TError> type of error
 */
public interface AsyncProducer<T, TError extends Exception> {

	/** Ask to produce new data, return null when the end is reached. */
	AsyncSupplier<T, TError> produce();
	
	/** Ask to produce new data in a dedicated CPU task, return null when the end is reached. */
	default AsyncSupplier<T, TError> produce(String description, byte priority) {
		AsyncSupplier<T, TError> result = new AsyncSupplier<>();
		new Task.Cpu.FromRunnable(description, priority, () -> produce().forward(result)).start();
		return result;
	}
	
	/** Produce and give the data to the consumer.
	 * While the data is consumed, this producer is asked to produce new data so it can be consumed
	 * as soon as it is produced and the previous data has been consumed.
	 */
	default Async<TError> toConsumer(AsyncConsumer<T, TError> consumer, String description, byte priority) {
		Async<TError> result = new Async<>();
		ConsumerThrows<T, TError> consume = new ConsumerThrows<T, TError>() {
			@Override
			public void accept(T data) {
				ConsumerThrows<T, TError> that = this;
				if (data == null) {
					consumer.end().onDone(result);
					return;
				}
				AsyncSupplier<T, TError> nextProduction = produce(description, priority);
				IAsync<TError> consumption = consumer.consume(data, null);
				consumption.onDone(() -> nextProduction.thenStart(
					new Task.Cpu.Parameter.FromConsumerThrows<>(description, priority, that), result), result);
			}
		};
		produce().thenStart(new Task.Cpu.Parameter.FromConsumerThrows<>(description, priority, consume), result);
		result.onError(consumer::error);
		return result;
	}
	
	/** Produce data, convert it, and give the converted data to the consumer. */
	default <T2> Async<TError> toConsumer(
		Function<T, AsyncSupplier<T2, TError>> converter,
		AsyncConsumer<T2, TError> consumer,
		String description, byte priority
	) {
		Async<TError> result = new Async<>();
		ConsumerThrows<T, TError> consume = new ConsumerThrows<T, TError>() {
			@Override
			public void accept(T data) {
				ConsumerThrows<T, TError> that = this;
				if (data == null) {
					consumer.end().onDone(result);
					return;
				}
				AsyncSupplier<T, TError> nextProduction = produce(description, priority);
				AsyncSupplier<T2, TError> conversion = converter.apply(data);
				conversion.thenStart(new Task.Cpu.Parameter.FromConsumerThrows<T2, TError>(description, priority, converted -> {
					IAsync<TError> consumption = consumer.consume(converted, null);
					consumption.onDone(() -> nextProduction.thenStart(
						new Task.Cpu.Parameter.FromConsumerThrows<>(description, priority, that), result), result);
				}), result);
			}
		};
		produce().thenStart(new Task.Cpu.Parameter.FromConsumerThrows<>(description, priority, consume), result);
		result.onError(consumer::error);
		return result;
	}
	
	/** Utility class implementing AsyncProducer holding a single data.
	 * @param <T> type of data produced
	 * @param <TError> type of error
	 */
	class SingleData<T, TError extends Exception> implements AsyncProducer<T, TError> {

		public SingleData(T data) {
			this.data = data;
		}
		
		private T data;
		
		@Override
		public AsyncSupplier<T, TError> produce() {
			T d = data;
			data = null;
			return new AsyncSupplier<>(d, null);
		}
		
	}
	
	/** Utility class implementing AsyncProducer but producing nothing.
	 * @param <T> type of data produced
	 * @param <TError> type of error
	 */
	class Empty<T, TError extends Exception> implements AsyncProducer<T, TError> {

		@Override
		public AsyncSupplier<T, TError> produce() {
			return new AsyncSupplier<>(null, null);
		}
		
	}
	
}
