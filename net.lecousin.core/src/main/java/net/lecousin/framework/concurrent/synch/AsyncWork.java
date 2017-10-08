package net.lecousin.framework.concurrent.synch;

import java.util.ArrayList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.BlockedThreadHandler;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.event.Listener;

/**
 * Same as a SynchronizationPoint, except that it contains a result.
 * @param <T> type of result
 * @param <TError> type of error
 */
@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
public class AsyncWork<T,TError extends Exception> implements ISynchronizationPoint<TError> {

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
		cancel(cancel);
	}
	
	/** Listener with one method by possible state.
	 * @param <T> type of result
	 * @param <TError> type of error
	 */
	public static interface AsyncWorkListener<T,TError> {
		/** Called when the AsyncWork is unblocked with a result. */
		public void ready(T result);
		
		/** Called when the AsyncWork is unblocked by an error. */
		public void error(TError error);
		
		/** Called when the AsyncWork is unblocked by a cancellation. */
		public void cancelled(CancelException event);
	}
	
	private boolean unblocked = false;
	private T result = null;
	private TError error = null;
	private CancelException cancel = null;
	private ArrayList<AsyncWorkListener<T,TError>> listenersInline = null;
	private ArrayList<Listener<CancelException>> onCancel = null;
	
	public T getResult() { return result; }
	
	@Override
	public TError getError() { return error; }
	
	@Override
	public CancelException getCancelEvent() { return cancel; }
	
	@Override
	public boolean isCancelled() { return cancel != null; }
	
	@Override
	public boolean hasError() {
		return error != null;
	}
	
	/** Add a listener to be called when this AsyncWork is unblocked. */
	public void listenInline(AsyncWorkListener<T,TError> listener) {
		synchronized (this) {
			if (unblocked) {
				if (error != null) listener.error(error);
				else if (cancel != null) listener.cancelled(cancel);
				else listener.ready(result);
				return;
			}
			if (listenersInline == null) listenersInline = new ArrayList<>(5);
			listenersInline.add(listener);
		}
	}

	/** Forward the result, error, or cancellation to the given AsyncWork. */
	public void listenInline(AsyncWork<T,TError> sp) {
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
	public void listenInline(SynchronizationPoint<TError> sp) {
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
	
	@Override
	public void listenInline(Runnable r) {
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
				return "Delegate to Runnable " + r;
			}
		});
	}
	
	/** Add a listener to call when this AsyncWork is cancelled. */
	public void listenCancel(Listener<CancelException> listener) {
		synchronized (this) {
			if (unblocked) {
				if (cancel != null) listener.fire(cancel);
				return;
			}
			if (onCancel == null) onCancel = new ArrayList<>(2);
			onCancel.add(listener);
		}
	}
	
	/** Unblock this AsyncWork with the given result. */
	public void unblockSuccess(T result) {
		ArrayList<AsyncWorkListener<T,TError>> listeners;
		synchronized (this) {
			if (unblocked) return;
			unblocked = true;
			this.result = result;
			listeners = listenersInline;
			listenersInline = null;
			if (listeners != null) {
				Application app = LCCore.getApplication();
				if (app.isReleaseMode())
					for (int i = 0; i < listeners.size(); ++i)
						try { listeners.get(i).ready(result); }
						catch (Throwable t) {
							app.getDefaultLogger().error(
								"Exception thrown by an inline listener of AsyncWork: "
								+ listeners.get(i), t);
						}
				else
					for (int i = 0; i < listeners.size(); ++i) {
						long start = System.nanoTime();
						try { listeners.get(i).ready(result); }
						catch (Throwable t) {
							app.getDefaultLogger().error(
								"Exception thrown by an inline listener of AsyncWork: "
								+ listeners.get(i), t);
						}
						long time = System.nanoTime() - start;
						if (time > 1000000) // more than 1ms
							app.getDefaultLogger().debug(
								"Listener ready took " + (time / 1000000.0d) + "ms: " + listeners.get(i));
					}
			}
			// notify after listeners
			this.notifyAll();
		}
	}
	
	/** Unblock this AsyncWork with an error. Equivalent to the method error. */
	public void unblockError(TError error) {
		ArrayList<AsyncWorkListener<T,TError>> listeners;
		synchronized (this) {
			if (unblocked) return;
			unblocked = true;
			this.error = error;
			listeners = listenersInline;
			listenersInline = null;
			if (listeners != null) {
				Application app = LCCore.getApplication();
				if (app.isReleaseMode())
					for (int i = 0; i < listeners.size(); ++i)
						try { listeners.get(i).error(error); }
						catch (Throwable t) {
							app.getDefaultLogger().error(
								"Exception thrown by an inline listener of AsyncWork: "
								+ listeners.get(i), t);
						}
				else
					for (int i = 0; i < listeners.size(); ++i) {
						long start = System.nanoTime();
						try { listeners.get(i).error(error); }
						catch (Throwable t) {
							app.getDefaultLogger().error(
								"Exception thrown by an inline listener of AsyncWork: "
								+ listeners.get(i), t);
						}
						long time = System.nanoTime() - start;
						if (time > 1000000) // more than 1ms
							app.getDefaultLogger()
								.debug("Listener error took " + (time / 1000000.0d) + "ms: " + listeners.get(i));
					}
			}
			this.notifyAll();
		}
	}
	
	@Override
	public void error(TError error) {
		unblockError(error);
	}

	/** Cancel this AsyncWork. Equivalent to the method cancel. */
	public void unblockCancel(CancelException event) {
		ArrayList<AsyncWorkListener<T,TError>> listeners;
		ArrayList<Listener<CancelException>> cancel;
		synchronized (this) {
			if (unblocked) return;
			unblocked = true;
			this.cancel = event;
			listeners = listenersInline;
			listenersInline = null;
			cancel = onCancel;
			onCancel = null;
			if (listeners != null) {
				Application app = LCCore.getApplication();
				if (app.isReleaseMode())
					for (int i = 0; i < listeners.size(); ++i)
						try { listeners.get(i).cancelled(event); }
						catch (Throwable t) {
							app.getDefaultLogger().error(
								"Exception thrown by an inline listener of AsyncWork: "
								+ listeners.get(i), t);
						}
				else
					for (int i = 0; i < listeners.size(); ++i) {
						long start = System.nanoTime();
						try { listeners.get(i).cancelled(event); }
						catch (Throwable t) {
							app.getDefaultLogger().error(
								"Exception thrown by an inline listener of AsyncWork: "
								+ listeners.get(i), t);
						}
						long time = System.nanoTime() - start;
						if (time > 1000000) // more than 1ms
							app.getDefaultLogger().debug(
								"Listener cancelled took " + (time / 1000000.0d) + "ms: " + listeners.get(i));
					}
			}
			this.notifyAll();
		}
		if (cancel != null) {
			for (Listener<CancelException> l : cancel)
				l.fire(event);
		}
	}
	
	@Override
	public void cancel(CancelException reason) {
		unblockCancel(reason);
	}
	
	@Override
	public void block(long timeout) {
		Thread t;
		BlockedThreadHandler blockedHandler;
		synchronized (this) {
			if (unblocked) return;
			t = Thread.currentThread();
			blockedHandler = Threading.getBlockedThreadHandler(t);
			if (blockedHandler == null) {
				if (timeout <= 0) {
					while (!unblocked)
						try { this.wait(0); }
						catch (InterruptedException e) { return; }
				} else
					try { this.wait(timeout); }
					catch (InterruptedException e) { return; }
			}
		}
		if (blockedHandler != null)
			blockedHandler.blocked(this, timeout);
		return;
	}
	
	/** Block until this AsyncWork is unblocked or the given timeout expired,
	 * and return the result in case of success, or throw the error or cancellation.
	 * @param timeout in milliseconds. 0 or negative value means infinite.
	 */
	public T blockResult(long timeout) throws TError, CancelException {
		Thread t;
		BlockedThreadHandler blockedHandler;
		synchronized (this) {
			if (unblocked) {
				if (error != null) throw error;
				if (cancel != null) throw cancel;
				return result;
			}
			t = Thread.currentThread();
			blockedHandler = Threading.getBlockedThreadHandler(t);
			if (blockedHandler == null)
				while (!unblocked)
					try { this.wait(timeout < 0 ? 0 : timeout); }
					catch (InterruptedException e) { return null; }
		}
		if (blockedHandler != null)
			blockedHandler.blocked(this, timeout);
		if (error != null) throw error;
		if (cancel != null) throw cancel;
		return result;
	}
	
	@Override
	public void blockPause(long logAfter) {
		synchronized (this) {
			if (unblocked) return;
			do {
				long start = System.currentTimeMillis();
				try { this.wait(logAfter + 1000); }
				catch (InterruptedException e) { return; }
				if (System.currentTimeMillis() - start <= logAfter)
					break;
				System.err.println("Still blocked after " + (logAfter / 1000) + "s.");
				new Exception("").printStackTrace(System.err);
			} while (true);
		}
	}

	@Override
	public synchronized boolean isUnblocked() {
		return unblocked;
	}
	
	/** Reset this AsyncWork point to reuse it.
	 * This method remove any previous result, error or cancellation, and mark this AsyncWork as blocked.
	 * Any previous listener is also removed.
	 */
	public void reset() {
		unblocked = false;
		result = null;
		error = null;
		cancel = null;
		listenersInline = null;
	}
	
}
