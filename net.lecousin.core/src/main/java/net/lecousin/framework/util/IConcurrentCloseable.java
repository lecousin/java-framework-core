package net.lecousin.framework.util;

/** A closeable resource having asynchronous operations.
 * @param <TError> type of error when closing.
 */ 
public interface IConcurrentCloseable<TError extends Exception> extends AutoCloseable, AsyncCloseable<TError>, CloseableListenable {

	/** Calling this method avoid this resource to be closed. For each call to this function, a call to the unlockClose method must be done. */
	boolean lockClose();
	
	/** Allow to close this resource, if this is the last lock on it.
	 * If the close method was previously called but locked, the resource is closed. */
	void unlockClose();
	
}
