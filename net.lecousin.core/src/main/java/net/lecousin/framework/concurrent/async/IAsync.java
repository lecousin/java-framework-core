package net.lecousin.framework.concurrent.async;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.log.Logger;

/**
 * Base interface for an asynchronous unit.<br/>
 * <br/>
 * An asynchronous unit can have 4 states:<ul>
 * 	<li>Blocked: this is the first state, until something happens (resource available, task done...)</li>
 *  <li>Unblocked with error: when an error occurs (while processing the task, if the waited resource cannot be obtained,
 *  	or if the underlying process was itself waiting for an asynchronous unit that raised an error)</li>
 *  <li>Unblocked with cancellation: when explicitly cancelled or because the underlying
 *  	process was itself waiting for an asynchronous unit that has been cancelled.</li>
 *  <li>Unblocked with success</li> 
 * </ul>
 * <br/>
 * The state can be consulted using methods {@link #isDone}, {@link #hasError} and {@link #isCancelled}.<br/>
 * <br/>
 * Listeners can be added to be called when the asynchronous unit is unblocked.
 * The order listeners are called is not guaranteed, especially because if a listener is added while
 * the previously registered listeners are called, the new listener will be immediately executed by
 * the calling thread, without waiting for other listeners to finish.
 * 
 * @param <TError> type of exception it may raise.
 */
public interface IAsync<TError extends Exception> extends Cancellable {

	/** Return true if unblocked (success or cancelled). */
	boolean isDone();
	
	/** Return true if unblocked because of an error. */
	boolean hasError();
	
	/** Unblock with an error. */
	void error(TError error);
	
	/** Return the error. */
	TError getError();
	
	/** Return true if unblocked with success: it must be unblocked, without error, and without a cancel event. */
	default boolean isSuccessful() { return isDone() && !hasError() && !isCancelled(); }

	/** Pause the current thread to wait for this asynchronous unit to be unblocked. */
	void block(long timeout);
	
	/** Pause the current thread to wait for this asynchronous unit to be unblocked,
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

	/** Call the given listener, only when done without error or cancellation. */
	default void onSuccess(Runnable listener) {
		onDone(new Runnable() {
			@Override
			public void run() {
				if (isSuccessful()) listener.run();
			}
			
			@Override
			public String toString() {
				return listener.toString();
			}
		});
	}
	
	/** Call the given listener, only when done with an error. */
	default void onError(Consumer<TError> listener) {
		onDone(new Runnable() {
			@Override
			public void run() {
				if (hasError()) listener.accept(getError());
			}
			
			@Override
			public String toString() {
				return listener.toString();
			}
		});
	}
	
	/** Call the given listener, only when done due to cancellation. */
	default void onCancel(Consumer<CancelException> listener) {
		onDone(new Runnable() {
			@Override
			public void run() {
				if (isCancelled()) listener.accept(getCancelEvent());
			}
			
			@Override
			public String toString() {
				return listener.toString();
			}
		});
	}

	/** Call the given runnable on error or cancellation. */
	default void onErrorOrCancel(Runnable runnable) {
		onDone(new Runnable() {
			@Override
			public void run() {
				if (!isSuccessful())
					runnable.run();
			}
			
			@Override
			public String toString() {
				return runnable.toString();
			}
		});
	}
	
	/** Call listener when unblocked (whatever the result is successful, has error or is cancelled). */
	void onDone(Runnable listener);
	
	/** Transfer the result of this asynchronous unit to the given one:
	 * unblock it on success, call error method on error, or call cancel method on cancellation.
	 */
	default void onDone(Async<TError> sp) {
		onDone(() -> {
			if (isCancelled())
				sp.cancel(getCancelEvent());
			else if (hasError())
				sp.error(getError());
			else
				sp.unblock();
		});
	}
	
	/** Transfer the result of this asynchronous unit to the given one:
	 * unblock it on success, call error method on error, or call cancel method on cancellation.
	 */
	default <TError2 extends Exception> void onDone(Async<TError2> sp, Function<TError, TError2> errorConverter) {
		onDone(() -> {
			if (isCancelled())
				sp.cancel(getCancelEvent());
			else if (hasError())
				sp.error(errorConverter.apply(getError()));
			else
				sp.unblock();
		});
	}
	
	/** Unblock the given AsyncSupplier, or forward the error or cancellation. */
	default <T> void onDone(AsyncSupplier<T, TError> sp, Supplier<T> resultSupplier) {
		onDone(() -> sp.unblockSuccess(resultSupplier.get()), sp);
	}
	
	/** Call listeners when unblocked. */
	default void onDone(Runnable onReady, Consumer<TError> onError, Consumer<CancelException> onCancel) {
		onDone(new Runnable() {
			@Override
			public void run() {
				if (hasError()) onError.accept(getError());
				else if (isCancelled()) onCancel.accept(getCancelEvent());
				else onReady.run();
			}
			
			@Override
			public String toString() {
				return onReady.toString() + '/' + onError + '/' + onCancel;
			}
		});
	}
	
	/** Call listener when unblocked, or forward error/cancellation to onErrorOrCancel. */
	default void onDone(Runnable onReady, IAsync<TError> onErrorOrCancel) {
		onDone(new Runnable() {
			@Override
			public void run() {
				if (hasError()) onErrorOrCancel.error(getError());
				else if (isCancelled()) onErrorOrCancel.cancel(getCancelEvent());
				else onReady.run();
			}
			
			@Override
			public String toString() {
				return onReady.toString();
			}
		});
	}
	
	/** Call listener when unblocked, or forward error/cancellation to onErrorOrCancel. */
	default <TError2 extends Exception> void onDone(
		Runnable onReady, IAsync<TError2> onErrorOrCancel, Function<TError, TError2> errorConverter
	) {
		onDone(new Runnable() {
			@Override
			public void run() {
				if (hasError()) onErrorOrCancel.error(errorConverter.apply(getError()));
				else if (isCancelled()) onErrorOrCancel.cancel(getCancelEvent());
				else onReady.run();
			}
			
			@Override
			public String toString() {
				return onReady.toString();
			}
		});
	}
	
	/** If this asynchronous unit is not successful, forward the result to the given one (this object MUST be done). */
	default boolean forwardIfNotSuccessful(IAsync<TError> sp) {
		if (hasError()) {
			sp.error(getError());
			return true;
		}
		if (isCancelled()) {
			sp.cancel(getCancelEvent());
			return true;
		}
		return false;
	}
	
	/** Start the given task when this asynchronous unit is unblocked. */
	default void thenStart(Task<?,? extends Exception> task, boolean evenIfErrorOrCancel) {
		task.startOn(this, evenIfErrorOrCancel);
	}
	
	/** Start the given task when this asynchronous unit is unblocked. */
	default void thenStart(String description, byte priority, Runnable task, boolean evenIfErrorOrCancel) {
		new Task.Cpu.FromRunnable(task, description, priority).startOn(this, evenIfErrorOrCancel);
	}
	
	/** Start the given task when this asynchronous unit is unblocked. */
	default void thenStart(Task<?,? extends Exception> task, Runnable onErrorOrCancel) {
		task.startOn(this, false);
		onErrorOrCancel(onErrorOrCancel);
	}
	
	/** Start the given task when this asynchronous unit is successfully unblocked, else the error or cancel
	 * event are forwarded to the given asynchronous unit.
	 */
	default void thenStart(Task<?,? extends Exception> task, IAsync<TError> onErrorOrCancel) {
		onDone(() -> {
			task.start();
			task.getOutput().onCancel(cancel -> { if (!onErrorOrCancel.isDone()) onErrorOrCancel.cancel(cancel); });
		}, onErrorOrCancel);
	}

	/** Start the given task when this synchronization point is successfully unblocked, else the error or cancel
	 * event are forwarded to the given synchronization point.
	 */
	default <TError2 extends Exception> void thenStart(
		Task<?,? extends Exception> task, IAsync<TError2> onErrorOrCancel,
		Function<TError, TError2> errorConverter
	) {
		onDone(() -> {
			task.start();
			task.getOutput().onCancel(cancel -> { if (!onErrorOrCancel.isDone()) onErrorOrCancel.cancel(cancel); });
		}, onErrorOrCancel, errorConverter);
	}
	
	/** Start the given task when this synchronization point is successfully unblocked, else the error or cancel
	 * event are forwarded to the given synchronization point.
	 */
	default void thenStart(String description, byte priority, Runnable task, IAsync<TError> onErrorOrCancel) {
		Task.Cpu.FromRunnable t = new Task.Cpu.FromRunnable(task, description, priority);
		onDone(() -> {
			t.start();
			t.getOutput().onCancel(cancel -> { if (!onErrorOrCancel.isDone()) onErrorOrCancel.cancel(cancel); });
		}, onErrorOrCancel);
	}

	/** Convert this synchronization point into an AsyncWork with Void result. */
	default AsyncSupplier<Void, TError> toAsyncSupplier() {
		AsyncSupplier<Void, TError> aw = new AsyncSupplier<>();
		onDone(() -> {
			if (hasError()) aw.error(getError());
			else if (isCancelled()) aw.cancel(getCancelEvent());
			else aw.unblockSuccess(null);
		});
		return aw;
	}
	
	/** Return a collection with all listeners, only for debugging purposes. */
	Collection<?> getAllListeners();
	
	/** Log an error thrown by a listener. */
	default void logListenerError(Logger log, Object listener, Throwable error) {
		log.error("Exception thrown by an inline listener of " + getClass().getSimpleName() + ": " + listener, error);
	}
	
}
