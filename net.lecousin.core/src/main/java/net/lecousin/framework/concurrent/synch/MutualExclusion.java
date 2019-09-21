package net.lecousin.framework.concurrent.synch;

import java.util.ArrayList;
import java.util.Collection;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.BlockedThreadHandler;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.log.Logger;

/**
 * A MutualExclusion must be unlocked by the same thread that locked it.
 * If the same thread is locking it several times, it must unlock it several times as well.<br/>
 * When a thread calls the lock method:<ul>
 * <li>if the mutual exclusion is not yet locked by another thread, it becomes locked and the calling thread can continue working,
 * 		and must call the method unlock once done.</li>
 * <li>if the mutual exclusion is already locked by another thread, the calling thread is paused,
 * 		waiting for the mutual exclusion to be unlocked.</li>
 * </ul>
 * If several threads are waiting for the mutual exclusion to be unlocked:<ul>
 * <li>Only one is resume and obtain the lock, the others are still paused</li>
 * <li>The order the threads are resumed is undetermined, meaning it is not necessarily in the order they called the lock method</li> 
 * </ul>
 * @param <TError> type of exception it may raise
 */
public class MutualExclusion<TError extends Exception> implements ISynchronizationPoint<TError> {

	private Thread lockingThread = null;
	private int lockedTimes = 0;
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
		Thread t = Thread.currentThread();
		if (t == lockingThread) {
			lockedTimes++;
			return;
		}
		BlockedThreadHandler blockedHandler = null;
		do {
			synchronized (this) {
				if (lockingThread == null) {
					lockingThread = t;
					lockedTimes = 1;
					return;
				}
				if (blockedHandler == null)
					blockedHandler = Threading.getBlockedThreadHandler(t);
				if (blockedHandler == null) {
					try { this.wait(0); }
					catch (InterruptedException e) { /* ignore */ }
					continue;
				}
			}
			blockedHandler.blocked(this, 0);
		} while (true);
	}
	
	/** Release the lock. */
	public void unlock() {
		ArrayList<Runnable> list;
		Thread t = Thread.currentThread();
		synchronized (this) {
			if (t != lockingThread) return;
			if (--lockedTimes > 0) return;
			lockingThread = null;
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
	public boolean isUnblocked() {
		return lockingThread == null;
	}
	
	@Override
	public void blockPause(long logAfter) {
		synchronized (this) {
			if (lockingThread == null) return;
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
	public void listenInline(Runnable r) {
		synchronized (this) {
			if (lockingThread == null) r.run();
			else {
				if (listeners == null) listeners = new ArrayList<>();
				listeners.add(r);
			}
		}
	}
	
	@Override
	public void block(long timeout) {
		if (lockingThread == null) return;
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		listenInline(sp::unblock);
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
