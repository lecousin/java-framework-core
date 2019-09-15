package net.lecousin.framework.util;

import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;

/**
 * Resource that can be closed asynchronously.
 * @param <TError> type of exception it may return.
 */
public interface AsyncCloseable<TError extends Exception> {
	
	/** Close asynchronously. */
	ISynchronizationPoint<TError> closeAsync();
	
	/** Close asynchronously this closeable once the given synchronization point is done. */
	default void closeAfter(ISynchronizationPoint<?> sp) {
		sp.listenInline(this::closeAsync);
	}

}
