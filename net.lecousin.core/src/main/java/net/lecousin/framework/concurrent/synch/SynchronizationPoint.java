package net.lecousin.framework.concurrent.synch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.BlockedThreadHandler;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.ThreadingDebugHelper;
import net.lecousin.framework.log.Logger;

/**
 * Simplest implementation of a synchronization point.
 * @param <TError> type of exception it may raise
 */
public class SynchronizationPoint<TError extends Exception> implements ISynchronizationPoint<TError>, Future<Void> {

	/** Constructor. */
	public SynchronizationPoint() {}
	
	/** Constructor with an initial state. */
	public SynchronizationPoint(boolean unblocked) {
		this.unblocked = unblocked;
	}
	
	/** Create an unblocked point with an error. */ 
	public SynchronizationPoint(TError error) {
		this.unblocked = true;
		this.error = error;
	}
	
	/** Create an unblocked point with a cancellation. */
	public SynchronizationPoint(CancelException cancel) {
		this.unblocked = true;
		this.cancelled = cancel;
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
	public final void listenInline(Runnable r) {
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
	public final void unblock() {
		if (Threading.debugSynchronization) ThreadingDebugHelper.unblocked(this);
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
		Logger log = LCCore.getApplication().getLoggerFactory().getLogger(SynchronizationPoint.class);
		while (true) {
			if (!log.debug())
				for (int i = 0; i < listeners.size(); ++i)
					try { listeners.get(i).run(); }
					catch (Throwable t) { log.error(
						"Exception thrown by an inline listener of SynchronizationPoint", t); }
			else
				for (int i = 0; i < listeners.size(); ++i) {
					long start = System.nanoTime();
					try { listeners.get(i).run(); }
					catch (Throwable t) { log.error(
						"Exception thrown by an inline listener of SynchronizationPoint", t); }
					long time = System.nanoTime() - start;
					if (time > 1000000) // more than 1ms
						log.debug("Listener took " + (time / 1000000.0d) + "ms: " + listeners.get(i));
				}
			synchronized (this) {
				if (listenersInline.isEmpty()) {
					listenersInline = null;
					listeners = null;
					this.notifyAll();
					break;
				}
				listeners.clear();
				ArrayList<Runnable> tmp = listeners;
				listeners = listenersInline;
				listenersInline = tmp;
			}
		}
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
						catch (InterruptedException e) { return; }
				} else
					try { this.wait(timeout); }
					catch (InterruptedException e) { return; }
			}
		}
		if (blockedHandler != null)
			blockedHandler.blocked(this, timeout);
	}
	
	@Override
	public final void blockPause(long logAfter) {
		synchronized (this) {
			while (!unblocked || listenersInline != null) {
				long start = System.currentTimeMillis();
				try { this.wait(logAfter + 1000); }
				catch (InterruptedException e) { return; }
				if (System.currentTimeMillis() - start <= logAfter)
					continue;
				System.err.println("Still blocked after " + (logAfter / 1000) + "s.");
				new Exception("").printStackTrace(System.err);
			}
		}
	}
	
	@Override
	public final synchronized boolean isUnblocked() {
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
		if (!isUnblocked()) throw new InterruptedException();
		if (hasError()) throw new ExecutionException(error);
		if (isCancelled()) throw new ExecutionException(cancelled);
		return null;
	}
	
	@Override
	public final Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		block(unit.toMillis(timeout));
		if (!isUnblocked()) throw new TimeoutException();
		if (hasError()) throw new ExecutionException(error);
		if (isCancelled()) throw new ExecutionException(cancelled);
		return null;
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
