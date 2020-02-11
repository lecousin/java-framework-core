package net.lecousin.framework.concurrent.util;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;

/**
 * Consumer of data.<br/>
 * 
 * @param <T> type of data
 * @param <TError> type of error
 */
public interface AsyncConsumer<T, TError extends Exception> {

	/** Consume the given data.
	 * @param data the data to consume
	 * @return an IAsync unblocked when the method consume can be called again or the method end can be called.
	 */
	IAsync<TError> consume(T data);
	
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
				consume(it.next()).onDone(this, result);
			}
		};
		pushNext.run();
		return result;
	}
	
	/** Consume data and call end. */
	default IAsync<TError> consumeEnd(T data) {
		Async<TError> result = new Async<>();
		consume(data).onDone(() -> end().onDone(result), result);
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
		public IAsync<TError> consume(T data) {
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
			public IAsync<TError> consume(T2 data) {
				return parent.consume(converter.apply(data));
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
