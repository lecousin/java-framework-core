package net.lecousin.framework.concurrent.synch;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.event.Listener;

/**
 * Base interface for synchronization points.<br/>
 * <br/>
 * A synchronization point can have 4 states:<ul>
 * 	<li>Blocked: this is the first state, until something happens (resource available, task done...)</li>
 *  <li>Unblocked with error: when an error occurs (while processing the task, if the waited resource cannot be obtained,
 *  	or if the underlying process was itself waiting for a synchronization point that raised an error)</li>
 *  <li>Unblocked with cancellation: when explicitly cancelled (because we don't care anymore) or because the underlying
 *  	process was itself waiting for a synchronization point that has been cancelled.</li>
 *  <li>Unblocked with success</li> 
 * </ul>
 * <br/>
 * The state can be consulted using methods isUnblocked, hasError and isCancelled.<br/>
 * <br/>
 * Listeners can be added to be called when the synchronization point is unblocked.
 * The order listeners are called is not guaranteed, especially because if a listener is added while
 * the previously registered listeners are called, the new listener will be immediately executed by
 * the calling thread, without waiting for other listeners to finish.
 * 
 * @param <TError> type of exception it may raise.
 */
public interface ISynchronizationPoint<TError extends Exception> {

	/** Return true if unblocked (success or cancelled). */
	boolean isUnblocked();
	
	/** Return true if unblocked because of a cancellation. */
	boolean isCancelled();
	
	/** Cancel this synchronization point: unblock it immediately, and set the cancellation reason. */
	void cancel(CancelException reason);
	
	/** Get the reason of the cancellation. */
	CancelException getCancelEvent();

	/** Return true if unblocked because of an error. */
	boolean hasError();
	
	/** Unblock with an error. */
	void error(TError error);
	
	/** Return the error. */
	TError getError();
	
	/** Return true if unblocked with success: it must be unblocked, without error, and without a cancel event. */
	default boolean isSuccessful() { return isUnblocked() && !hasError() && !isCancelled(); }

	/** Pause the current thread to wait for this synchronization point to be unblocked. */
	void block(long timeout);

	/** Really block, using wait method (used by threading system to know when a thread can be resumed). */
	void blockPause(long logWarningAfterMillis);
	
	/** Call listener when unblocked (whatever the result is successful, has error or is cancelled). */
	void listenInline(Runnable listener);
	
	/** Call listeners when unblocked. */
	default void listenInline(Runnable onReady, Listener<TError> onError, Listener<CancelException> onCancel) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				if (hasError()) onError.fire(getError());
				else if (isCancelled()) onCancel.fire(getCancelEvent());
				else onReady.run();
			}
		});
	}
	
	/** Call listener when unblocked, or forward error/cancellation to onErrorOrCancel. */
	default void listenInline(Runnable onReady, ISynchronizationPoint<TError> onErrorOrCancel) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				if (hasError()) onErrorOrCancel.error(getError());
				else if (isCancelled()) onErrorOrCancel.cancel(getCancelEvent());
				else onReady.run();
			}
		});
	}
	
	/** Transfer the result of this blocking point to the given synchronization point:
	 * unblock it on success, call error method on error, or call cancel method on cancellation.
	 */
	default void listenInline(SynchronizationPoint<TError> sp) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				if (isCancelled())
					sp.cancel(getCancelEvent());
				else if (hasError())
					sp.error(getError());
				else
					sp.unblock();
			}
		});
	}
	
	/** Same as listenInline but does not restrict to a synchronization point with the same exception type as parameter. */
	default void listenInlineSP(SynchronizationPoint<Exception> sp) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				if (isCancelled())
					sp.cancel(getCancelEvent());
				else if (hasError())
					sp.error(getError());
				else
					sp.unblock();
			}
		});
	}
	
	/** Unblock the given synchronization point whatever the result is successful, has error or is cancelled. */
	default void synchWithNoError(SynchronizationPoint<?> sp) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				sp.unblock();
			}
		});
	}
	
	/** Call the given listener, only when this synchronization point is unblocked with an error. */
	default void onError(Listener<TError> listener) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				if (hasError()) listener.fire(getError());
			}
		});
	}
	
	/** Call the given listener, only when this synchronization point is unblocked with an error. */
	default void onException(Listener<Exception> listener) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				if (hasError()) listener.fire(getError());
			}
		});
	}
	
	/** Call the given listener, only when this synchronization point is unblocked due to cancellation. */
	default void onCancel(Listener<CancelException> listener) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				if (isCancelled()) listener.fire(getCancelEvent());
			}
		});
	}
	
	/** Start the given task when this synchronization point is unblocked. */
	default void listenAsynch(Task<?,? extends Exception> task, boolean evenIfErrorOrCancel) {
		task.startOn(this, evenIfErrorOrCancel);
	}
	
}
