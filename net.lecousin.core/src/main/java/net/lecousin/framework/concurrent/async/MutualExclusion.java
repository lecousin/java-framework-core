package net.lecousin.framework.concurrent.async;

import java.util.ArrayList;

import net.lecousin.framework.concurrent.threads.TaskExecutor;
import net.lecousin.framework.concurrent.threads.Threading;

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
public class MutualExclusion<TError extends Exception> extends AbstractLock<TError> {

	private Thread lockingThread = null;
	private int lockedTimes = 0;
	
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
		TaskExecutor executor = null;
		do {
			synchronized (this) {
				if (lockingThread == null) {
					lockingThread = t;
					lockedTimes = 1;
					return;
				}
				if (executor == null)
					executor = Threading.getTaskExecutor(t);
				if (executor == null) {
					try { this.wait(0); }
					catch (InterruptedException e) { /* ignore */ }
					continue;
				}
			}
			executor.blocked(this, 0);
		} while (true);
	}
	
	/** Release the lock. */
	@Override
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
	public boolean isDone() {
		return lockingThread == null;
	}
	
	@Override
	public boolean blockPauseCondition() {
		return lockingThread != null;
	}
	
	@Override
	public void onDone(Runnable r) {
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
		Async<Exception> sp = new Async<>();
		onDone(sp::unblock);
		sp.block(timeout);
	}

}
