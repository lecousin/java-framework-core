package net.lecousin.framework.concurrent.synch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.BlockedThreadHandler;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.util.Pair;

/**
 * Same as a SynchronizationPoint, except that it contains a result.
 * @param <T> type of result
 * @param <TError> type of error
 */
public class AsyncWork<T,TError extends Exception> implements ISynchronizationPoint<TError>, Future<T> {
	
	private static final String TO_STRING_INLINE = "AsyncWork.listenInline: ";

	/** Constructor. */
	public AsyncWork() {}
	
	/** Create an unblocked point with the given result and error. */
	public AsyncWork(T result, TError error) {
		unblocked = true;
		this.result = result;
		this.error = error;
	}
	
	/** Create an unblocked point with the given result, error and cancellation. */
	public AsyncWork(T result, TError error, CancelException cancel) {
		this(result, error);
		this.cancel = cancel;
	}
	
	/** Listener with one method by possible state.
	 * @param <T> type of result
	 * @param <TError> type of error
	 */
	public static interface AsyncWorkListener<T, TError extends Exception> {
		/** Called when the AsyncWork is unblocked with a result. */
		void ready(T result);
		
		/** Called when the AsyncWork is unblocked by an error. */
		void error(TError error);
		
		/** Called when the AsyncWork is unblocked by a cancellation. */
		void cancelled(CancelException event);
	}
	
	/** Implementation with a listener and an ISynchronizationPoint for error or cancel event.
	 * @param <T> type of result
	 * @param <TError> type of error
	 */
	public static class AsyncWorkListenerReady<T, TError extends Exception> implements AsyncWorkListener<T, TError> {
		
		/** Listener on ready.
		 * @param <T> type of result
		 * @param <TError> type of error
		 */
		public static interface OnReady<T, TError extends Exception> {
			/** Listener on ready. */
			void ready(T result, AsyncWorkListenerReady<T, TError> listener);
		}
		
		/** Constructor. */
		public AsyncWorkListenerReady(OnReady<T, TError> listener, ISynchronizationPoint<TError> onErrorOrCancel) {
			this.listener = listener;
			this.onErrorOrCancel = onErrorOrCancel;
		}

		/** Constructor. */
		public AsyncWorkListenerReady(OnReady<T, TError> listener, ISynchronizationPoint<TError> onErrorOrCancel,
			Consumer<Pair<T, TError>> onDoneError) {
			this.listener = listener;
			this.onErrorOrCancel = onErrorOrCancel;
			this.onError = onDoneError;
		}
		
		protected OnReady<T, TError> listener;
		protected ISynchronizationPoint<TError> onErrorOrCancel;
		protected Consumer<Pair<T, TError>> onError;
		
		@Override
		public final void ready(T result) {
			listener.ready(result, this);
		}
		
		@Override
		public final void error(TError error) {
			if (onError != null) onError.accept(new Pair<>(null, error));
			onErrorOrCancel.error(error);
		}
		
		@Override
		public final void cancelled(CancelException event) {
			onErrorOrCancel.cancel(event);
		}
	}

	private boolean unblocked = false;
	private T result = null;
	private TError error = null;
	private CancelException cancel = null;
	private ArrayList<AsyncWorkListener<T,TError>> listenersInline = null;
	
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
	
	/** Add a listener to be called when this AsyncWork is unblocked. */
	public final void listenInline(AsyncWorkListener<T,TError> listener) {
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
	
	/** Forward the result, error, or cancellation to the given AsyncWork. */
	public final void listenInline(AsyncWork<T,TError> sp) {
		listenInline(new AsyncWorkListener<T,TError>() {
			@Override
			public void ready(T result) {
				sp.unblockSuccess(result);
			}
			
			@Override
			public void error(TError error) {
				sp.unblockError(error);
			}
			
			@Override
			public void cancelled(CancelException event) {
				sp.unblockCancel(event);
			}
		});
	}
	
	@Override
	public final void listenInline(SynchronizationPoint<TError> sp) {
		listenInline(new AsyncWorkListener<T,TError>() {
			@Override
			public void ready(T result) {
				sp.unblock();
			}
			
			@Override
			public void error(TError error) {
				sp.error(error);
			}
			
			@Override
			public void cancelled(CancelException event) {
				sp.cancel(event);
			}
		});
	}

	/** Call one of the given listener depending on result. */ 
	public final void listenInline(Listener<T> onready, Listener<TError> onerror, Listener<CancelException> oncancel) {
		listenInline(new AsyncWorkListener<T,TError>() {
			@Override
			public void ready(T result) {
				onready.fire(result);
			}
			
			@Override
			public void error(TError error) {
				onerror.fire(error);
			}
			
			@Override
			public void cancelled(CancelException event) {
				oncancel.fire(event);
			}
			
			@Override
			public String toString() {
				return TO_STRING_INLINE + onready;
			}
		});
	}
	
	/** Call onready on success, or forward error/cancellation to onErrorAndCancel. */
	public final void listenInline(Listener<T> onready, ISynchronizationPoint<TError> onErrorAndCancel) {
		listenInline(new AsyncWorkListener<T,TError>() {
			@Override
			public void ready(T result) {
				onready.fire(result);
			}
			
			@Override
			public void error(TError error) {
				onErrorAndCancel.error(error);
			}
			
			@Override
			public void cancelled(CancelException event) {
				onErrorAndCancel.cancel(event);
			}
			
			@Override
			public String toString() {
				return TO_STRING_INLINE + onready;
			}
		});
	}

	@Override
	public final void listenInline(Runnable onready, ISynchronizationPoint<TError> onErrorAndCancel) {
		listenInline(new AsyncWorkListener<T,TError>() {
			@Override
			public void ready(T result) {
				onready.run();
			}
			
			@Override
			public void error(TError error) {
				onErrorAndCancel.error(error);
			}
			
			@Override
			public void cancelled(CancelException event) {
				onErrorAndCancel.cancel(event);
			}
			
			@Override
			public String toString() {
				return TO_STRING_INLINE + onready;
			}
		});
	}
	
	@Override
	public final void listenInline(Runnable r) {
		listenInline(new AsyncWorkListener<T,TError>() {
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
				return TO_STRING_INLINE + r;
			}
		});
	}
	
	/** Register a listener to be called only on success, with the result. */
	public final void listenInline(Listener<T> onSuccess) {
		listenInline(new AsyncWorkListener<T,TError>() {
			@Override
			public void ready(T result) {
				onSuccess.fire(result);
			}
			
			@Override
			public void error(TError error) {
				// ignore
			}
			
			@Override
			public void cancelled(CancelException event) {
				// ignore
			}
			
			@Override
			public String toString() {
				return TO_STRING_INLINE + onSuccess;
			}
		});
	}
	
	/** Forward the result, error, or cancellation to the given AsyncWork. */
	public final void listenInlineGenericError(AsyncWork<T, Exception> sp) {
		listenInline(new AsyncWorkListener<T,TError>() {
			@Override
			public void ready(T result) {
				sp.unblockSuccess(result);
			}
			
			@Override
			public void error(TError error) {
				sp.unblockError(error);
			}
			
			@Override
			public void cancelled(CancelException event) {
				sp.unblockCancel(event);
			}
		});
	}
	
	/** Unblock this AsyncWork with the given result. */
	public final void unblockSuccess(T result) {
		ArrayList<AsyncWorkListener<T,TError>> listeners;
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
		Logger log = LCCore.getApplication().getLoggerFactory().getLogger(SynchronizationPoint.class);
		do {
			if (!log.debug())
				for (int i = 0; i < listeners.size(); ++i)
					try { listeners.get(i).ready(result); }
					catch (Exception t) { logListenerError(log, listeners.get(i), t); }
			else
				for (int i = 0; i < listeners.size(); ++i) {
					long start = System.nanoTime();
					try { listeners.get(i).ready(result); }
					catch (Exception t) { logListenerError(log, listeners.get(i), t); }
					long time = System.nanoTime() - start;
					if (time > 1000000) // more than 1ms
						log.debug("Listener ready took " + (time / 1000000.0d) + "ms: " + listeners.get(i));
				}
			listeners = getNextListeners(listeners);
		} while (listeners != null);
	}

	private synchronized ArrayList<AsyncWorkListener<T,TError>> getNextListeners(ArrayList<AsyncWorkListener<T,TError>> previousListeners) {
		if (listenersInline.isEmpty()) {
			listenersInline = null;
			this.notifyAll();
			return null;
		}
		ArrayList<AsyncWorkListener<T,TError>> nextListeners = listenersInline;
		previousListeners.clear();
		listenersInline = previousListeners;
		return nextListeners;
	}

	/** Unblock this AsyncWork with an error. Equivalent to the method error. */
	@SuppressWarnings("squid:S3776") // complexity
	public final void unblockError(TError error) {
		ArrayList<AsyncWorkListener<T,TError>> listeners;
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
		Logger log = LCCore.getApplication().getLoggerFactory().getLogger(SynchronizationPoint.class);
		do {
			if (!log.debug())
				for (int i = 0; i < listeners.size(); ++i)
					try { listeners.get(i).error(error); }
					catch (Exception t) {
						logListenerError(log, listeners.get(i), t);
						try { listeners.get(i).cancelled(new CancelException("Error in listener", t)); }
						catch (Exception t2) { log.error(
							"Exception thrown while cancelling inline listener of AsyncWork after error: "
							+ listeners.get(i), t2); }
					}
			else
				for (int i = 0; i < listeners.size(); ++i) {
					long start = System.nanoTime();
					try { listeners.get(i).error(error); }
					catch (Exception t) {
						logListenerError(log, listeners.get(i), t);
						try { listeners.get(i).cancelled(new CancelException("Error in listener", t)); }
						catch (Exception t2) { log.error(
							"Exception thrown while cancelling inline listener of AsyncWork after error: "
							+ listeners.get(i), t2); }
					}
					long time = System.nanoTime() - start;
					if (time > 1000000) // more than 1ms
						log.debug("Listener error took " + (time / 1000000.0d) + "ms: " + listeners.get(i));
				}
			listeners = getNextListeners(listeners);
		} while (listeners != null);
	}
	
	@Override
	public final void error(TError error) {
		unblockError(error);
	}

	/** Cancel this AsyncWork. Equivalent to the method cancel. */
	public void unblockCancel(CancelException event) {
		ArrayList<AsyncWorkListener<T,TError>> listeners;
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
		Logger log = LCCore.getApplication().getLoggerFactory().getLogger(SynchronizationPoint.class);
		do {
			if (!log.debug())
				for (int i = 0; i < listeners.size(); ++i)
					try { listeners.get(i).cancelled(event); }
					catch (Exception t) { logListenerError(log, listeners.get(i), t); }
			else
				for (int i = 0; i < listeners.size(); ++i) {
					long start = System.nanoTime();
					try { listeners.get(i).cancelled(event); }
					catch (Exception t) { logListenerError(log, listeners.get(i), t); }
					long time = System.nanoTime() - start;
					if (time > 1000000) // more than 1ms
						log.debug("Listener cancelled took " + (time / 1000000.0d) + "ms: " + listeners.get(i));
				}
			listeners = getNextListeners(listeners);
		} while (listeners != null);
	}
	
	@Override
	public final void cancel(CancelException reason) {
		unblockCancel(reason);
	}

	/** Unblock this AsyncWork once the given synchronization point is done,
	 * with error, cancel event, or success according to the result of sp. */
	public final void unblockOn(ISynchronizationPoint<TError> sp, Supplier<T> resultOnSuccess) {
		sp.listenInline(() -> {
			if (sp.hasError())
				unblockError(sp.getError());
			else if (sp.isCancelled())
				unblockCancel(sp.getCancelEvent());
			else
				unblockSuccess(resultOnSuccess.get());
		});
	}
	
	@Override
	@SuppressWarnings("squid:S2274")
	public final void block(long timeout) {
		Thread t;
		BlockedThreadHandler blockedHandler;
		synchronized (this) {
			if (unblocked && listenersInline == null) return;
			t = Thread.currentThread();
			blockedHandler = Threading.getBlockedThreadHandler(t);
			if (blockedHandler == null) {
				if (timeout <= 0) {
					while (!unblocked || listenersInline != null)
						try { this.wait(0); }
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
				} else {
					try { this.wait(timeout); }
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
		}
		if (blockedHandler != null)
			blockedHandler.blocked(this, timeout);
	}
	
	/** Block until this AsyncWork is unblocked or the given timeout expired,
	 * and return the result in case of success, or throw the error or cancellation.
	 * @param timeout in milliseconds. 0 or negative value means infinite.
	 */
	public final T blockResult(long timeout) throws TError, CancelException {
		Thread t;
		BlockedThreadHandler blockedHandler;
		synchronized (this) {
			if (unblocked && listenersInline == null) {
				if (error != null) throw error;
				if (cancel != null) throw cancel;
				return result;
			}
			t = Thread.currentThread();
			blockedHandler = Threading.getBlockedThreadHandler(t);
			if (blockedHandler == null)
				while (!unblocked || listenersInline != null)
					try { this.wait(timeout < 0 ? 0 : timeout); }
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return null;
					}
		}
		if (blockedHandler != null)
			blockedHandler.blocked(this, timeout);
		if (error != null) throw error;
		if (cancel != null) throw cancel;
		return result;
	}
	
	@Override
	public final void blockPause(long logAfter) {
		synchronized (this) {
			while (!unblocked || listenersInline != null) {
				long start = System.currentTimeMillis();
				try { this.wait(logAfter + 1000); }
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				if (System.currentTimeMillis() - start <= logAfter)
					continue;
				Logger logger = LCCore.get().getThreadingLogger();
				logger.warn("Still blocked after " + (logAfter / 1000) + "s.", new Exception(""));
			}
		}
	}

	@Override
	public final synchronized boolean isUnblocked() {
		return unblocked;
	}
	
	/** Reset this AsyncWork point to reuse it.
	 * This method remove any previous result, error or cancellation, and mark this AsyncWork as blocked.
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
		if (!isUnblocked()) throw new InterruptedException();
		if (hasError()) throw new ExecutionException(error);
		if (isCancelled()) throw new ExecutionException(cancel);
		return result;
	}
	
	@Override
	public final T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		block(unit.toMillis(timeout));
		if (!isUnblocked()) throw new TimeoutException();
		if (hasError()) throw new ExecutionException(error);
		if (isCancelled()) throw new ExecutionException(cancel);
		return result;
	}
	
	// skip checkstyle: OverloadMethodsDeclarationOrder
	@Override
	public final boolean cancel(boolean mayInterruptIfRunning) {
		if (isUnblocked()) return false;
		cancel(new CancelException("Cancelled"));
		return true;
	}
	
	@Override
	public final boolean isDone() {
		return isUnblocked();
	}
	
}
