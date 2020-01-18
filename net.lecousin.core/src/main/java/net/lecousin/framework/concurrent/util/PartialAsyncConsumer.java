package net.lecousin.framework.concurrent.util;

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
	
}
