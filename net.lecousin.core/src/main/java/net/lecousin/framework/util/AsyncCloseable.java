package net.lecousin.framework.util;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;

/**
 * Resource that can be closed asynchronously.
 * @param <TError> type of exception it may return.
 */
public interface AsyncCloseable<TError extends Exception> {
	
	/** Close asynchronously. */
	public ISynchronizationPoint<TError> closeAsync();
	
}
