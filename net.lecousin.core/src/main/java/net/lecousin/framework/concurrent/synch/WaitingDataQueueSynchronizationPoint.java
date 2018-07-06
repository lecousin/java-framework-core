package net.lecousin.framework.concurrent.synch;

import java.util.ArrayList;
import java.util.Collection;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.BlockedThreadHandler;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Threading;

/**
 * Like a SynchronizationPoint, but with a queue of waiting data.
 * If no data is available, it is considered as blocked, and any thread calling the method waitForData
 * will be paused until a new data arrives in the pool.
 * Each call to the method waitForData will return the next data, in the order they arrived in the queue.
 * <br/>
 * When no more data will be added, the method endOfData can be called, so if any thread arrives, and no
 * more data is available in the queue, the thread is not paused, and a null data is returned.
 * @param <DataType> type of data in the queue
 * @param <TError> type of error
 */
public class WaitingDataQueueSynchronizationPoint<DataType,TError extends Exception> implements ISynchronizationPoint<TError> {

	private TurnArray<DataType> waitingData = new TurnArray<>();
	private TError error = null;
	private CancelException cancel = null;
	private boolean end = false;
	private ArrayList<Runnable> listeners = null;

	/** To call when data is needed.
	 * @return the next data, or null if no more data will be available
	 */
	public DataType waitForData(long timeout) {
		long start = System.currentTimeMillis();
		do {
			Thread t;
			BlockedThreadHandler blockedHandler;
			synchronized (this) {
				if (cancel != null)
					return null;
				if (error != null)
					return null;
				if (!waitingData.isEmpty())
					return waitingData.removeFirst();
				if (end)
					return null;
				t = Thread.currentThread();
				blockedHandler = Threading.getBlockedThreadHandler(t);
				if (blockedHandler == null) {
					try { this.wait(timeout); }
					catch (InterruptedException e) { return null; }
				}
			}
			if (blockedHandler != null)
				blockedHandler.blocked(this, timeout);
		} while (timeout <= 0 || (System.currentTimeMillis() - start) < timeout);
		return null;
	}
	
	/** Queue a new data, which may unblock a thread waiting for it. */
	public void newDataReady(DataType data) {
		ArrayList<Runnable> list;
		synchronized (this) {
			if (end) throw new IllegalStateException("method endOfData already called, method newDataReady is not allowed anymore");
			waitingData.addLast(data);
			list = listeners;
			listeners = null;
		}
		if (list != null) {
			for (Runnable listener : list) listener.run();
		}
		// notify after listeners
		synchronized (this) {
			this.notify();
		}
	}
	
	/** Signal that no more data will be queued, so any waiting thread can be unblocked. */
	public void endOfData() {
		ArrayList<Runnable> list = null;
		synchronized (this) {
			end = true;
			if (waitingData.isEmpty()) {
				list = listeners;
				listeners = null;
			}
		}
		if (list != null) {
			for (Runnable listener : list) listener.run();
		}
		// notify after listeners
		synchronized (this) {
			this.notifyAll();
		}
	}
	
	@Override
	public Collection<?> getAllListeners() {
		if (listeners == null) return new ArrayList<>(0);
		return new ArrayList<>(listeners);
	}
	
	@Override
	public boolean isUnblocked() {
		return !waitingData.isEmpty() || cancel != null || error != null || end;
	}
	
	@Override
	public void block(long timeout) {
		long start = System.currentTimeMillis();
		do {
			Thread t;
			BlockedThreadHandler blockedHandler;
			synchronized (this) {
				if (cancel != null) return;
				if (error != null) return;
				if (!waitingData.isEmpty()) return;
				if (end) return;
				t = Thread.currentThread();
				blockedHandler = Threading.getBlockedThreadHandler(t);
				if (blockedHandler == null)
					try { this.wait(timeout); }
					catch (InterruptedException e) { return; }
			}
			if (blockedHandler != null)
				blockedHandler.blocked(this, timeout);
		} while (timeout <= 0 || (System.currentTimeMillis() - start) < timeout);
	}
	
	@Override
	public void blockPause(long logAfter) {
		synchronized (this) {
			if (cancel != null) return;
			if (error != null) return;
			if (!waitingData.isEmpty()) return;
			if (end) return;
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
	public void listenInline(Runnable listener) {
		synchronized (this) {
			if (waitingData.isEmpty() && !end) {
				if (listeners == null) listeners = new ArrayList<>();
				listeners.add(listener);
				return;
			}
		}
		listener.run();
	}

	@Override
	public boolean isCancelled() {
		return cancel != null;
	}

	@Override
	public void cancel(CancelException reason) {
		cancel = reason;
		ArrayList<Runnable> list;
		synchronized (this) {
			this.notify();
			list = listeners;
			listeners = null;
		}
		if (list != null) {
			for (Runnable listener : list) listener.run();
		}
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
		ArrayList<Runnable> list;
		synchronized (this) {
			this.notify();
			list = listeners;
			listeners = null;
		}
		if (list != null) {
			for (Runnable listener : list) listener.run();
		}
	}
	
}
