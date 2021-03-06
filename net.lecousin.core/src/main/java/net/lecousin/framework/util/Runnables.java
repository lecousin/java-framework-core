package net.lecousin.framework.util;

/** Runnable utility interfaces. */
public interface Runnables {

	/**
	 * Same as Runnable but that throws an exception.
	 * @param <TError> type of the exception
	 */
	public interface Throws<TError extends Exception> {

		/** Called to execute the runnable. */
		void run() throws TError;
		
	}
	
	/**
	 * Same as Supplier but that throws an exception.
	 * @param <T> type of data returned
	 * @param <TError> type of the exception
	 */
	public interface SupplierThrows<T, TError extends Exception> {

		/** Called to execute the runnable. */
		T get() throws TError;
		
	}
	
	/**
	 * Same as Consumer but that throws an exception.
	 * @param <T> type of data returned
	 * @param <TError> type of the exception
	 */
	public interface ConsumerThrows<T, TError extends Exception> {

		/** Called to execute the runnable. */
		void accept(T t) throws TError;
		
	}
	
	/**
	 * Same as IntConsumer but that throws an exception.
	 * @param <TError> type of the exception
	 */
	public interface IntConsumerThrows<TError extends Exception> {

		/** Called to execute the runnable. */
		void accept(int value) throws TError;
		
	}
	
	/**
	 * Same as LongConsumer but that throws an exception.
	 * @param <TError> type of the exception
	 */
	public interface LongConsumerThrows<TError extends Exception> {

		/** Called to execute the runnable. */
		void accept(long value) throws TError;
		
	}
	
	/**
	 * Same as Function but that throws an exception.
	 * @param <T1> type of data given
	 * @param <T2> type of data returned
	 * @param <TError> type of the exception
	 */
	public interface FunctionThrows<T1, T2, TError extends Exception> {

		/** Called to execute the runnable. */
		T2 apply(T1 input) throws TError;
		
	}
	
}
