package net.lecousin.framework.concurrent.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.util.Runnables.ConsumerThrows;
import net.lecousin.framework.util.Runnables.FunctionThrows;
import net.lecousin.framework.util.ThreadUtil;

/**
 * Same as a SynchronizationPoint, except that it contains a result.
 * @param <T> type of result
 * @param <TError> type of error
 */
public class AsyncSupplier<T,TError extends Exception> implements IAsync<TError>, Future<T> {
	
	/** Constructor. */
	public AsyncSupplier() {}
	
	/** Create an unblocked point with the given result and error. */
	public AsyncSupplier(T result, TError error) {
		unblocked = true;
		this.result = result;
		this.error = error;
	}
	
	/** Create an unblocked point with the given result, error and cancellation. */
	public AsyncSupplier(T result, TError error, CancelException cancel) {
		this(result, error);
		this.cancel = cancel;
	}
	
	/** Listener with one method by possible state.
	 * @param <T> type of result
	 * @param <TError> type of error
	 */
	public static interface Listener<T, TError extends Exception> {
		/** Called when the AsyncSupplier is unblocked with a result. */
		void ready(T result);
		
		/** Called when the AsyncSupplier is unblocked by an error. */
		void error(TError error);
		
		/** Called when the AsyncSupplier is unblocked by a cancellation. */
		void cancelled(CancelException event);
		
		/** Create a Listener with the given listeners. */
		static <T, TError extends Exception> Listener<T, TError> from(
			Consumer<T> onReady,
			Consumer<TError> onError,
			Consumer<CancelException> onCancel,
			String name
		) {
			return new Listener<T, TError>() {
				@Override
				public void ready(T result) {
					if (onReady != null) onReady.accept(result);
				}
				
				@Override
				public void error(TError error) {
					if (onError != null) onError.accept(error);
				}
				
				@Override
				public void cancelled(CancelException event) {
					if (onCancel != null) onCancel.accept(event);
				}
				
				@Override
				public String toString() {
					return name != null ? name : ("" + onReady + " / " + onError + " / " + onCancel);
				}
			};
		}
		
		/** Create a Listener with the given listeners. */
		static <T, TError extends Exception> Listener<T, TError> from(
			Runnable onReady,
			Consumer<TError> onError,
			Consumer<CancelException> onCancel,
			String name
		) {
			return from(new Consumer<T>() {
				@Override
				public void accept(T t) {
					if (onReady != null) onReady.run();
				}

				@Override
				public String toString() {
					return onReady != null ? onReady.toString() : "null";
				}
			}, onError, onCancel, name);
		}
	}
	
	private boolean unblocked = false;
	private T result = null;
	private TError error = null;
	private CancelException cancel = null;
	private ArrayList<Listener<T,TError>> listenersInline = null;
	
	public final T getResult() { return result; }
	
	@Override
	public final TError getError() { return error; }
	
	@Override
	public final CancelException getCancelEvent() { return cancel; }
	
	@Override
	public final boolean isCancelled() { return cancel != null; }
	
	@Override
	public final boolean hasError() {
		return error != null;
	}
	
	@Override
	public Collection<?> getAllListeners() {
		if (listenersInline == null) return new ArrayList<>(0);
		return new ArrayList<>(listenersInline);
	}
	
	/** Add a listener to be called when this AsyncSupplier is unblocked. */
	public final void listen(Listener<T,TError> listener) {
		synchronized (this) {
			if (!unblocked || listenersInline != null) {
				if (listenersInline == null) listenersInline = new ArrayList<>(5);
				listenersInline.add(listener);
				return;
			}
		}
		if (error != null) listener.error(error);
		else if (cancel != null) listener.cancelled(cancel);
		else listener.ready(result);
	}
	
	/** Forward the result, error, or cancellation to the given AsyncSupplier. */
	public final void forward(AsyncSupplier<T,TError> sp) {
		listen(Listener.from(sp::unblockSuccess, sp::error, sp::cancel, sp.toString()));
	}
	
	/** Forward the result, error, or cancellation to the given AsyncSupplier. */
	public final <TError2 extends Exception> void forward(AsyncSupplier<T, TError2> sp, Function<TError, TError2> errorConverter) {
		listen(Listener.from(sp::unblockSuccess, e -> sp.unblockError(errorConverter.apply(e)), sp::cancel, sp.toString()));
	}
	
	@Override
	public final void onDone(Runnable r) {
		listen(new Listener<T,TError>() {
			@Override
			public void ready(T result) {
				r.run();
			}
			
			@Override
			public void error(TError error) {
				r.run();
			}
			
			@Override
			public void cancelled(CancelException event) {
				r.run();
			}
			
			@Override
			public String toString() {
				return r.toString();
			}
		});
	}

	@Override
	public final void onDone(Async<TError> sp) {
		listen(Listener.from(r -> sp.unblock(), sp::error, sp::cancel, sp.toString()));
	}

	/** Call one of the given listener depending on result. */ 
	public final void onDone(Consumer<T> onready, Consumer<TError> onerror, Consumer<CancelException> oncancel) {
		listen(Listener.from(onready, onerror, oncancel, null));
	}

	/** Call one of the given listener on success. */ 
	public final void onDone(Consumer<T> onready) {
		listen(Listener.from(onready, null, null, null));
	}
	
	/** Call onready on success, or forward error/cancellation to onErrorAndCancel. */
	public final void onDone(Consumer<T> onready, IAsync<TError> onErrorAndCancel) {
		listen(Listener.from(onready, onErrorAndCancel::error, onErrorAndCancel::cancel, null));
	}

	@Override
	public final void onDone(Runnable onready, IAsync<TError> onErrorAndCancel) {
		listen(Listener.from(onready, onErrorAndCancel::error, onErrorAndCancel::cancel, null));
	}
	
	/** Call onready on success, or forward error/cancellation to onErrorAndCancel. */
	public final <TError2 extends Exception> void onDone(
		Consumer<T> onready, IAsync<TError2> onErrorAndCancel, Function<TError, TError2> errorConverter
	) {
		listen(Listener.from(onready, err -> onErrorAndCancel.error(errorConverter.apply(err)), onErrorAndCancel::cancel, null));
	}
		
	/** Create an AsyncSupplier from the given one, adapting the error. */
	public final <TError2 extends Exception> AsyncSupplier<T, TError2> convertError(Function<TError, TError2> converter) {
		AsyncSupplier<T, TError2> a = new AsyncSupplier<>();
		forward(a, converter);
		return a;
	}
	
	/** Start the given task when this asynchronous unit is unblocked. */
	public <T2> AsyncSupplier<T2, TError> thenStart(
		String taskDescription, Task.Priority priority,
		FunctionThrows<T, T2, TError> fct, boolean evenIfErrorOrCancel
	) {
		Executable.FromFunctionThrows<T, T2, TError> executable = new Executable.FromFunctionThrows<>(fct);
		Task<T2, TError> task = Task.cpu(taskDescription, priority, executable);
		if (evenIfErrorOrCancel)
			onDone(() -> {
				executable.setInput(getResult());
				task.start();
			});
		else
			onDone(
				res -> {
					executable.setInput(res);
					task.start();
				},
				err -> task.setDone(null, err),
				task::cancel
			);
		return task.getOutput();
	}
	
	/** Start the given task when this asynchronous unit is unblocked. */
	public <T2> AsyncSupplier<T2, TError> thenStart(
		String taskDescription, Task.Priority priority,
		FunctionThrows<T, T2, TError> fct, IAsync<TError> onErrorOrCancel
	) {
		Executable.FromFunctionThrows<T, T2, TError> executable = new Executable.FromFunctionThrows<>(fct);
		Task<T2, TError> task = Task.cpu(taskDescription, priority, executable);
		onDone(() -> {
			executable.setInput(getResult());
			task.start();
		}, onErrorOrCancel);
		return task.getOutput();
	}
	

	
	/** Start the given task when this asynchronous unit is unblocked. */
	public IAsync<TError> thenStart(
		String taskDescription, Task.Priority priority,
		ConsumerThrows<T, TError> consumer, boolean evenIfErrorOrCancel
	) {
		Executable.FromConsumerThrows<T, TError> executable = new Executable.FromConsumerThrows<>(consumer);
		Task<Void, TError> task = Task.cpu(taskDescription, priority, executable);
		if (evenIfErrorOrCancel)
			onDone(() -> {
				executable.setInput(getResult());
				task.start();
			});
		else
			onDone(
				res -> {
					executable.setInput(res);
					task.start();
				},
				err -> task.setDone(null, err),
				task::cancel
			);
		return task.getOutput();
	}
	
	/** Start the given task when this asynchronous unit is unblocked. */
	public IAsync<TError> thenStart(
		String taskDescription, Task.Priority priority,
		ConsumerThrows<T, TError> consumer, IAsync<TError> onErrorOrCancel
	) {
		Executable.FromConsumerThrows<T, TError> executable = new Executable.FromConsumerThrows<>(consumer);
		Task<Void, TError> task = Task.cpu(taskDescription, priority, executable);
		onDone(() -> {
			executable.setInput(getResult());
			task.start();
		}, onErrorOrCancel);
		return task.getOutput();
	}
	
	

	/** Call consumer immediately (in current thread) if done, or start a CPU task on done. */
	public boolean thenDoOrStart(String taskDescription, Task.Priority taskPriority, Consumer<T> consumer, IAsync<TError> onErrorOrCancel) {
		if (isDone()) {
			if (!forwardIfNotSuccessful(onErrorOrCancel))
				consumer.accept(getResult());
			return true;
		}
		Executable.FromConsumer<T> executable = new Executable.FromConsumer<>(consumer);
		onDone(() -> {
			executable.setInput(getResult());
			Task.cpu(taskDescription, taskPriority, executable).start();
		}, onErrorOrCancel);
		return false;
	}
	
	/** Unblock this AsyncSupplier with the given result. */
	public final void unblockSuccess(T result) {
		ArrayList<Listener<T,TError>> listeners;
		synchronized (this) {
			if (unblocked) return;
			unblocked = true;
			this.result = result;
			if (listenersInline == null) {
				this.notifyAll();
				return;
			}
			listeners = listenersInline;
			listenersInline = new ArrayList<>(2);
		}
		Logger log = Threading.getLogger();
		do {
			if (!log.debug())
				for (int i = 0; i < listeners.size(); ++i)
					try { listeners.get(i).ready(result); }
					catch (Exception t) { logListenerError(log, listeners.get(i), t); }
			else
				for (int i = 0; i < listeners.size(); ++i) {
					Listener<T, TError> listener = listeners.get(i); 
					long start = System.nanoTime();
					try { listener.ready(result); }
					catch (Exception t) { logListenerError(log, listener, t); }
					Threading.debugListenerCall(listener, System.nanoTime() - start);
				}
			listeners = getNextListeners(listeners);
		} while (listeners != null);
	}

	private synchronized ArrayList<Listener<T,TError>> getNextListeners(ArrayList<Listener<T,TError>> previousListeners) {
		if (listenersInline.isEmpty()) {
			listenersInline = null;
			this.notifyAll();
			return null;
		}
		ArrayList<Listener<T,TError>> nextListeners = listenersInline;
		previousListeners.clear();
		listenersInline = previousListeners;
		return nextListeners;
	}

	/** Unblock this AsyncSupplier with an error. Equivalent to the method error. */
	@SuppressWarnings("squid:S3776") // complexity
	public final void unblockError(TError error) {
		ArrayList<Listener<T,TError>> listeners;
		synchronized (this) {
			if (unblocked) return;
			unblocked = true;
			this.error = error;
			if (listenersInline == null) {
				this.notifyAll();
				return;
			}
			listeners = listenersInline;
			listenersInline = new ArrayList<>(2);
		}
		Logger log = Threading.getLogger();
		do {
			if (!log.debug())
				for (int i = 0; i < listeners.size(); ++i)
					try { listeners.get(i).error(error); }
					catch (Exception t) {
						logListenerError(log, listeners.get(i), t);
						try { listeners.get(i).cancelled(new CancelException("Error in listener", t)); }
						catch (Exception t2) { log.error(
							"Exception thrown while cancelling inline listener of AsyncSupplier after error: "
							+ listeners.get(i), t2); }
					}
			else
				for (int i = 0; i < listeners.size(); ++i) {
					Listener<T, TError> listener = listeners.get(i);
					long start = System.nanoTime();
					try { listener.error(error); }
					catch (Exception t) {
						logListenerError(log, listener, t);
						try { listener.cancelled(new CancelException("Error in listener", t)); }
						catch (Exception t2) { log.error(
							"Exception thrown while cancelling inline listener of AsyncSupplier after error: "
							+ listener, t2); }
					}
					Threading.debugListenerCall(listener, System.nanoTime() - start);
				}
			listeners = getNextListeners(listeners);
		} while (listeners != null);
	}
	
	@Override
	public final void error(TError error) {
		unblockError(error);
	}

	/** Cancel this AsyncSupplier. Equivalent to the method cancel. */
	public void unblockCancel(CancelException event) {
		ArrayList<Listener<T,TError>> listeners;
		synchronized (this) {
			if (unblocked) return;
			unblocked = true;
			this.cancel = event;
			if (listenersInline == null) {
				this.notifyAll();
				return;
			}
			listeners = listenersInline;
			listenersInline = new ArrayList<>(2);
		}
		Logger log = Threading.getLogger();
		do {
			if (!log.debug())
				for (int i = 0; i < listeners.size(); ++i)
					try { listeners.get(i).cancelled(event); }
					catch (Exception t) { logListenerError(log, listeners.get(i), t); }
			else
				for (int i = 0; i < listeners.size(); ++i) {
					Listener<T, TError> listener = listeners.get(i);
					long start = System.nanoTime();
					try { listener.cancelled(event); }
					catch (Exception t) { logListenerError(log, listener, t); }
					Threading.debugListenerCall(listener, System.nanoTime() - start);
				}
			listeners = getNextListeners(listeners);
		} while (listeners != null);
	}
	
	@Override
	public final void cancel(CancelException reason) {
		unblockCancel(reason);
	}

	@Override
	@SuppressWarnings("squid:S2274")
	public final void block(long timeout) {
		Blockable blockable;
		synchronized (this) {
			if (unblocked && listenersInline == null) return;
			blockable = Threading.getBlockable();
			if (blockable == null) {
				if (timeout <= 0) {
					while (!unblocked || listenersInline != null)
						if (!ThreadUtil.wait(this, 0)) return;
				} else {
					if (!ThreadUtil.wait(this, timeout)) return;
				}
			}
		}
		if (blockable != null)
			blockable.blocked(this, timeout);
	}
	
	/** Block until this AsyncSupplier is unblocked or the given timeout expired,
	 * and return the result in case of success, or throw the error or cancellation.
	 * @param timeout in milliseconds. 0 or negative value means infinite.
	 */
	public final T blockResult(long timeout) throws TError, CancelException {
		Blockable blockable;
		synchronized (this) {
			if (unblocked && listenersInline == null) {
				if (error != null) throw error;
				if (cancel != null) throw cancel;
				return result;
			}
			blockable = Threading.getBlockable();
			if (blockable == null)
				while (!unblocked || listenersInline != null)
					if (!ThreadUtil.wait(this, timeout < 0 ? 0 : timeout)) return null;
		}
		if (blockable != null)
			blockable.blocked(this, timeout);
		if (error != null) throw error;
		if (cancel != null) throw cancel;
		return result;
	}
	
	@Override
	public boolean blockPauseCondition() {
		return !unblocked || listenersInline != null;
	}

	@Override
	public final synchronized boolean isDone() {
		return unblocked;
	}
	
	/** Reset this AsyncSupplier point to reuse it.
	 * This method remove any previous result, error or cancellation, and mark this AsyncSupplier as blocked.
	 * Any previous listener is also removed.
	 */
	public final void reset() {
		unblocked = false;
		result = null;
		error = null;
		cancel = null;
		listenersInline = null;
	}
	
	/* --- Future implementation --- */
	
	@Override
	public final T get() throws InterruptedException, ExecutionException {
		block(0);
		if (!isDone()) throw new InterruptedException();
		if (hasError()) throw new ExecutionException(error);
		if (isCancelled()) throw new ExecutionException(cancel);
		return result;
	}
	
	@Override
	public final T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		block(unit.toMillis(timeout));
		if (!isDone()) throw new TimeoutException();
		if (hasError()) throw new ExecutionException(error);
		if (isCancelled()) throw new ExecutionException(cancel);
		return result;
	}
	
	// skip checkstyle: OverloadMethodsDeclarationOrder
	@Override
	public final boolean cancel(boolean mayInterruptIfRunning) {
		if (isDone()) return false;
		cancel(new CancelException("Cancelled"));
		return true;
	}
	
}
