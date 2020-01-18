package net.lecousin.framework.concurrent.util;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;

/**
 * Consumer of data.<br/>
 * The time the data can be released/reused is distinguished from the time new data can be consumed.
 *
 * <p>In case we are in a chain of consumer, it is possible that the data can be released once processed
 * by the first consumer, but new data cannot come until end of the chain. It is also possible that
 * new data can come before the data is released (for example using a BufferedAsyncConsumer).</p>
 * 
 * @param <T> type of data
 * @param <TError> type of error
 */
public interface AsyncConsumer<T, TError extends Exception> {

	/** Consume the given data.
	 * Note that it is allowed for a consumer to never call the onDataRelease.<br/>
	 * onDataRelease should be called before the result is unblock if possible.
	 * @param data the data to consume
	 * @param onDataRelease called when the data can be released or reused (may be null).
	 * @return an IAsync unblocked when the method consume can be called again or the method end can be called.
	 */
	IAsync<TError> consume(T data, Consumer<T> onDataRelease);
	
	/** End of consumption, no more data to come. */
	IAsync<TError> end();
	
	/** Interrupt before the end. */
	void error(TError error);
	
	/** Call the consume method for each data in the given list. */
	default IAsync<TError> push(List<T> dataList) {
		Iterator<T> it = dataList.iterator();
		Async<TError> result = new Async<>();
		Runnable pushNext = new Runnable() {
			@Override
				public void run() {
				if (!it.hasNext()) {
					result.unblock();
					return;
				}
				consume(it.next(), null).onDone(this, result);
			}
		};
		pushNext.run();
		return result;
	}
	
	/** Consume data and call end. */
	default IAsync<TError> consumeEnd(T data, Consumer<T> onDataRelease) {
		Async<TError> result = new Async<>();
		consume(data, onDataRelease).onDone(() -> end().onDone(result), result);
		return result;
	}
	
	/**
	 * Simple AsyncConsumer that does nothing for end and error methods.
	 * @param <T> type of data
	 * @param <TError> type of error
	 */
	interface Simple<T, TError extends Exception> extends AsyncConsumer<T, TError> {
		
		@Override
		default IAsync<TError> end() {
			return new Async<>(true);
		}
		
		@Override
		default void error(TError error) {
			// nothing to do
		}
	}

	/** AsyncConsumer that consumes nothing and return immediately an error.
	 * @param <T> type of data
	 * @param <TError> type of error
	 */
	static class Error<T, TError extends Exception> implements AsyncConsumer<T, TError> {
		
		public Error(TError error) {
			this.error = error;
		}
		
		protected TError error;

		@Override
		public IAsync<TError> consume(T data, Consumer<T> onDataRelease) {
			return new Async<>(error);
		}

		@Override
		public IAsync<TError> end() {
			return new Async<>(error);
		}

		@Override
		public void error(TError error) {
			// nothing
		}
		
	}
	
	/** Convert the data. */
	default <T2> AsyncConsumer<T2, TError> convert(Function<T2, T> converter) {
		final AsyncConsumer<T, TError> parent = this;
		return new AsyncConsumer<T2, TError>() {

			@Override
			public IAsync<TError> consume(T2 data, Consumer<T2> onDataRelease) {
				return parent.consume(converter.apply(data), d -> {
					if (onDataRelease != null) onDataRelease.accept(data);
				});
			}

			@Override
			public IAsync<TError> end() {
				return parent.end();
			}

			@Override
			public void error(TError error) {
				parent.error(error);
			}
			
		};
	}
	
}
