package net.lecousin.framework.concurrent.async;

import java.util.ArrayList;
import java.util.Collection;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.BlockedThreadHandler;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.log.Logger;

/**
 * A LockPoint is similar to a mutual exclusion, but can be locked and unlocked by any thread.<br/>
 * For example, a first thread can lock it, and a second thread (after some processing is done) can unlock it.<br/>
 * A LockPoint can also be seen as a SynchronizationPoint but <i>reusable</i>.<br/>
 * When a thread calls the lock method:<ul>
 * <li>if the lock point is not yet locked it becomes locked and the calling thread can continue working,
 * 		and must call the method unlock once done.</li>
 * <li>if the lock point is already locked, the calling thread is paused, waiting for the lock point to be unlocked.</li>
 * </ul>
 * If several threads are waiting for the lock point to be unlocked:<ul>
 * <li>Only one is resume and obtain the lock, the others are still paused</li>
 * <li>The order the threads are resumed is undetermined, meaning it is not necessarily in the order they called the lock method</li> 
 * </ul>
 * @param <TError> type of exception it may raise
 */
public class LockPoint<TError extends Exception> implements IAsync<TError> {

	private boolean locked = false;
	private TError error = null;
	private CancelException cancel = null;
	private ArrayList<Runnable> listeners = null;
	
	/** Request the lock. If it is already locked this method will block until it become unblocked and the lock has been obtained. */
	@SuppressWarnings("squid:S2142") // InterruptedException
	public void lock() {
		if (cancel != null)
			return;
		if (error != null)
			return;
		Thread t;
		BlockedThreadHandler blockedHandler;
		do {
			synchronized (this) {
				if (!locked) {
					locked = true;
					return;
				}
				t = Thread.currentThread();
				blockedHandler = Threading.getBlockedThreadHandler(t);
				if (blockedHandler != null) break;
				try { this.wait(0); }
				catch (InterruptedException e) { /* continue anyway */ }
			}
		} while (true);
		blockedHandler.blocked(this, 0);
	}
	
	/** Release the lock. */
	public void unlock() {
		ArrayList<Runnable> list;
		synchronized (this) {
			locked = false;
			list = listeners;
			listeners = null;
		}
		if (list != null) {
			for (Runnable r : list) r.run();
		}
		// notify after listeners
		synchronized (this) {
			this.notifyAll();
		}
	}
	
	@Override
	public boolean isDone() {
		return !locked;
	}
	
	@Override
	public void blockPause(long logAfter) {
		synchronized (this) {
			if (!locked) return;
			do {
				long start = System.currentTimeMillis();
				try { this.wait(logAfter + 1000); }
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				if (System.currentTimeMillis() - start <= logAfter)
					break;
				Logger logger = LCCore.get().getThreadingLogger();
				logger.warn("Still blocked after " + (logAfter / 1000) + "s.", new Exception(""));
			} while (true);
		}
	}
	
	@Override
	public Collection<?> getAllListeners() {
		if (listeners == null) return new ArrayList<>(0);
		return new ArrayList<>(listeners);
	}

	@Override
	public void onDone(Runnable r) {
		synchronized (this) {
			if (!locked) r.run();
			else {
				if (listeners == null) listeners = new ArrayList<>();
				listeners.add(r);
			}
		}
	}
	
	@Override
	public void block(long timeout) {
		if (!locked) return;
		Async<Exception> sp = new Async<>();
		onDone(sp::unblock);
		sp.block(timeout);
	}

	@Override
	public boolean isCancelled() { return cancel != null; }
	
	@Override
	public void cancel(CancelException reason) {
		cancel = reason;
		unlock();
	}

	@Override
	public CancelException getCancelEvent() {
		return cancel;
	}

	@Override
	public boolean hasError() {
		return error != null;
	}

	@Override
	public TError getError() {
		return error;
	}

	@Override
	public void error(TError error) {
		this.error = error;
		unlock();
	}
	
}
