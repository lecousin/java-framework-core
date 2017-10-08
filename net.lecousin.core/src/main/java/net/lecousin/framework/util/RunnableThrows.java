package net.lecousin.framework.util;

/**
 * Same as Runnable but that throws an exception.
 * @param <TError> type of the exception
 */
public interface RunnableThrows<TError extends Exception> {

	/** Called to execute the runnable. */
	void run() throws TError;
	
}
