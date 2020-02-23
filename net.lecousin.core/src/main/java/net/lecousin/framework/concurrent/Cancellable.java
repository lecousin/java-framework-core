package net.lecousin.framework.concurrent;

/** Interface for cancellable asynchronous unit. */
public interface Cancellable {
	
	/** Return true if this object is cancelled. */
	boolean isCancelled();
	
	/** Cancel this object: unblock it immediately, and set the cancellation reason. */
	void cancel(CancelException reason);
	
	/** Get the reason of the cancellation. */
	CancelException getCancelEvent();

}
