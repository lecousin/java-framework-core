package net.lecousin.framework.concurrent.synch;

import java.util.Collection;
import java.util.function.Function;

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
	
	/** Pause the current thread to wait for this synchronization point to be unblocked,
	 * and if it has an error it is thrown. */
	default void blockException(long timeout) throws TError {
		block(timeout);
		if (hasError()) throw getError();
	}
	
	/** Pause the current thread to wait for this synchronization point to be unblocked.
	 * If it has an error or a cancellation it is thrown. */
	default void blockThrow(long timeout) throws TError, CancelException {
		block(timeout);
		if (hasError()) throw getError();
		if (isCancelled()) throw getCancelEvent();
	}

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
			
			@Override
			public String toString() {
				return "SynchronzationPoint.listenInline:" + onReady;
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
			
			@Override
			public String toString() {
				return "SynchronzationPoint.listenInline:" + onReady;
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
			
			@Override
			public String toString() {
				return "SynchronzationPoint.listenInline: " + sp;
			}
		});
	}
	
	/** Transfer the result of this blocking point to the given synchronization point:
	 * unblock it on success, call error method on error, or call cancel method on cancellation.
	 */
	default <SPError extends Exception> void listenInline(SynchronizationPoint<SPError> sp, Function<TError, SPError> errorConverter) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				if (isCancelled())
					sp.cancel(getCancelEvent());
				else if (hasError())
					sp.error(errorConverter.apply(getError()));
				else
					sp.unblock();
			}
			
			@Override
			public String toString() {
				return "SynchronzationPoint.listenInline: " + sp;
			}
		});
	}
	
	/** Call listener when unblocked, or forward error/cancellation to onErrorOrCancel. */
	default void listenInlineSP(Runnable onReady, ISynchronizationPoint<Exception> onErrorOrCancel) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				if (hasError()) onErrorOrCancel.error(getError());
				else if (isCancelled()) onErrorOrCancel.cancel(getCancelEvent());
				else onReady.run();
			}
			
			@Override
			public String toString() {
				return "SynchronzationPoint.listenInline: " + onReady;
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
			
			@Override
			public String toString() {
				return "SynchronzationPoint.listenInlineSP: " + sp;
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
			
			@Override
			public String toString() {
				return "SynchronzationPoint.synchWithNoError: " + sp;
			}
		});
	}
	
	/** Call the given listener, only when this synchronization point is unblocked without error or cancellation. */
	default void onSuccess(Runnable listener) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				if (isSuccessful()) listener.run();
			}
			
			@Override
			public String toString() {
				return "SynchronzationPoint.onSuccess: " + listener;
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
			
			@Override
			public String toString() {
				return "SynchronzationPoint.onError: " + listener;
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
			
			@Override
			public String toString() {
				return "SynchronzationPoint.onException: " + listener;
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
			
			@Override
			public String toString() {
				return "SynchronzationPoint.onCancel: " + listener;
			}
		});
	}
	
	/** If this synchronization point is cancelled, the given synchronization point will be cancelled as well with the same reason. */
	default void forwardCancel(ISynchronizationPoint<?> sp) {
		onCancel((reason) -> { sp.cancel(reason); });
	}
	
	/** If this synchronization point is errored, the given synchronization point will be unblocked as well with the same error. */
	default void forwardError(ISynchronizationPoint<TError> sp) {
		onError((error) -> { sp.error(error); });
	}
	
	/** Start the given task when this synchronization point is unblocked. */
	default void listenAsync(Task<?,? extends Exception> task, boolean evenIfErrorOrCancel) {
		task.startOn(this, evenIfErrorOrCancel);
	}
	
	/** Start the given task when this synchronization point is unblocked. */
	default void listenAsync(String description, byte priority, Runnable task, boolean evenIfErrorOrCancel) {
		new Task.Cpu.FromRunnable(task, description, priority).startOn(this, evenIfErrorOrCancel);
	}
	
	/** Start the given task when this synchronization point is successfully unblocked, else the error or cancel
	 * event are forwarded to the given synchronization point.
	 */
	default void listenAsync(Task<?,? extends Exception> task, ISynchronizationPoint<TError> onErrorOrCancel) {
		listenInline(new Runnable() {
			@Override
			public void run() {
				task.start();
				task.getOutput().onCancel((cancel) -> { if (!onErrorOrCancel.isUnblocked()) onErrorOrCancel.cancel(cancel); });
			}
			
			@Override
			public String toString() {
				return "SynchronzationPoint.listenAsync: " + task.getDescription();
			}
		}, onErrorOrCancel);
	}
	
	/** Start the given task when this synchronization point is successfully unblocked, else the error or cancel
	 * event are forwarded to the given synchronization point.
	 */
	default void listenAsync(String description, byte priority, Runnable task, ISynchronizationPoint<TError> onErrorOrCancel) {
		Task.Cpu.FromRunnable t = new Task.Cpu.FromRunnable(task, description, priority);
		listenInline(new Runnable() {
			@Override
			public void run() {
				t.start();
				t.getOutput().onCancel((cancel) -> { if (!onErrorOrCancel.isUnblocked()) onErrorOrCancel.cancel(cancel); });
			}
			
			@Override
			public String toString() {
				return "SynchronzationPoint.listenAsync: " + description;
			}
		}, onErrorOrCancel);
	}

	/** Start the given task when this synchronization point is successfully unblocked, else the error or cancel
	 * event are forwarded to the given synchronization point.
	 */
	default void listenAsyncSP(Task<?,? extends Exception> task, ISynchronizationPoint<Exception> onErrorOrCancel) {
		listenInlineSP(new Runnable() {
			@Override
			public void run() {
				task.start();
				task.getOutput().onCancel((cancel) -> { if (!onErrorOrCancel.isUnblocked()) onErrorOrCancel.cancel(cancel); });
			}
			
			@Override
			public String toString() {
				return "SynchronzationPoint.listenAsyncSP: " + task.getDescription();
			}
		}, onErrorOrCancel);
	}
	
	/** Convert this synchronization point into an AsyncWork with Void result. */
	default AsyncWork<Void, TError> toAsyncWorkVoid() {
		AsyncWork<Void, TError> aw = new AsyncWork<>();
		listenInline(() -> {
			if (hasError()) aw.error(getError());
			else if (isCancelled()) aw.cancel(getCancelEvent());
			else aw.unblockSuccess(null);
		});
		return aw;
	}
	
	/** Return a collection with all listeners, only for debugging purposes. */
	public Collection<?> getAllListeners();
	
}
