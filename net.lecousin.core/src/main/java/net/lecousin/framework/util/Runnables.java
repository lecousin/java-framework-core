package net.lecousin.framework.util;

/** Runnable utility classes or interfaces. */
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
	 * Same as Function but that throws an exception.
	 * @param <T1> type of data given
	 * @param <T2> type of data returned
	 * @param <TError> type of the exception
	 */
	public interface FunctionThrows<T1, T2, TError extends Exception> {

		/** Called to execute the runnable. */
		T2 apply(T1 input) throws TError;
		
	}
	
	/**
	 * A simple abstract class that implements Runnable and holds an object given in the constructor.
	 * @param <T> type of object to hold
	 */
	public abstract class WithData<T> implements Runnable {

		/** Constructor. */
		public WithData(T data) {
			this.data = data;
		}
		
		protected T data;
		
		public T getData() { return data; }
		
	}

}
