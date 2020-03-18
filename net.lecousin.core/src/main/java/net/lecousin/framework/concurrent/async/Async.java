package net.lecousin.framework.concurrent.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.util.ThreadUtil;

/**
 * Simplest implementation of a synchronization point.
 * @param <TError> type of exception it may raise
 */
public class Async<TError extends Exception> implements IAsync<TError>, Future<Void> {

	/** Constructor. */
	public Async() {
		// nothing
	}
	
	/** Constructor with an initial state. */
	public Async(boolean unblocked) {
		this.unblocked = unblocked;
	}
	
	/** Create an unblocked point with an error. */ 
	public Async(TError error) {
		this.unblocked = true;
		this.error = error;
	}
	
	/** Create an unblocked point with a cancellation. */
	public Async(CancelException cancel) {
		this.unblocked = true;
		this.cancelled = cancel;
	}
	
	/** Create an asynchronous unit from the given one, converting the error using the given converter. */
	public <TError2 extends Exception> Async(IAsync<TError2> toConvert, Function<TError2, TError> errorConverter) {
		toConvert.onDone(this, errorConverter);
	}
	
	private boolean unblocked = false;
	private TError error = null;
	private CancelException cancelled = null;
	private ArrayList<Runnable> listenersInline = null;
	
	@Override
	public Collection<?> getAllListeners() {
		if (listenersInline == null) return new ArrayList<>(0);
		return new ArrayList<>(listenersInline);
	}
	
	@Override
	public final void onDone(Runnable r) {
		synchronized (this) {
			if (!unblocked || listenersInline != null) {
				if (listenersInline == null) listenersInline = new ArrayList<>(5);
				listenersInline.add(r);
				return;
			}
		}
		r.run();
	}
	
	/** Unblock this synchronization point without error. */
	@SuppressWarnings("squid:S3776") // complexity
	public final void unblock() {
		ArrayList<Runnable> listeners;
		synchronized (this) {
			if (unblocked) return;
			unblocked = true;
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
					try { listeners.get(i).run(); }
					catch (Exception t) { logListenerError(log, listeners.get(i), t); }
			else
				for (int i = 0; i < listeners.size(); ++i) {
					Runnable listener = listeners.get(i);
					long start = System.nanoTime();
					try { listener.run(); }
					catch (Exception t) { logListenerError(log, listener, t); }
					Threading.debugListenerCall(listener, System.nanoTime() - start);
				}
			synchronized (this) {
				if (listenersInline.isEmpty()) {
					listenersInline = null;
					listeners = null;
					this.notifyAll();
				} else {
					listeners.clear();
					ArrayList<Runnable> tmp = listeners;
					listeners = listenersInline;
					listenersInline = tmp;
				}
			}
		} while (listeners != null);
	}
	
	@Override
	public final void error(TError error) {
		this.error = error;
		unblock();
	}
	
	@Override
	public final void cancel(CancelException reason) {
		this.cancelled = reason;
		unblock();
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
	
	@Override
	public boolean blockPauseCondition() {
		return !unblocked || listenersInline != null;
	}
	
	@Override
	public final synchronized boolean isDone() {
		return unblocked;
	}
	
	@Override
	public final boolean isCancelled() {
		return cancelled != null;
	}
	
	@Override
	public final boolean hasError() {
		return error != null;
	}
	
	@Override
	public final CancelException getCancelEvent() {
		return cancelled;
	}
	
	@Override
	public final TError getError() {
		return error;
	}
	
	/** Reset this synchronization point to reuse it.
	 * This method remove any previous error or cancellation and mark this synchronization point as blocked.
	 * Any previous listener is also removed, and won't be called.
	 */
	public final synchronized void reset() {
		unblocked = false;
		cancelled = null;
		error = null;
		listenersInline = null;
	}
	
	/** Same as the method reset except that listeners are kept. */
	public final synchronized void restart() {
		unblocked = false;
		cancelled = null;
		error = null;
	}

	/* --- Future implementation --- */
	
	@Override
	public final Void get() throws InterruptedException, ExecutionException {
		block(0);
		if (!isDone()) throw new InterruptedException();
		if (hasError()) throw new ExecutionException(error);
		if (isCancelled()) throw new ExecutionException(cancelled);
		return null;
	}
	
	@Override
	public final Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		block(unit.toMillis(timeout));
		if (!isDone()) throw new TimeoutException();
		if (hasError()) throw new ExecutionException(error);
		if (isCancelled()) throw new ExecutionException(cancelled);
		return null;
	}
	
	// skip checkstyle: OverloadMethodsDeclarationOrder
	@Override
	public final boolean cancel(boolean mayInterruptIfRunning) {
		if (isDone()) return false;
		cancel(new CancelException("Cancelled"));
		return true;
	}
	
}
