package net.lecousin.framework.util;

public interface IConcurrentCloseable extends AutoCloseable, AsyncCloseable<Exception>, CloseableListenable {

	/** Calling this method avoid this resource to be closed. For each call to this function, a call to the unlockClose method must be done. */
	boolean lockClose();
	
	/** Allow to close this resource, if this is the last lock on it.
	 * If the close method was previously called but locked, the resource is closed. */
	void unlockClose();
	
}
