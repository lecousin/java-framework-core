package net.lecousin.framework.concurrent.async;

import java.util.ArrayList;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.BlockedThreadHandler;
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
public class WaitingDataQueueSynchronizationPoint<DataType,TError extends Exception> extends AbstractLock<TError> {

	private TurnArray<DataType> waitingData = new TurnArray<>();
	private boolean end = false;

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
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return null;
					}
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
	public boolean isDone() {
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
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
			}
			if (blockedHandler != null)
				blockedHandler.blocked(this, timeout);
		} while (timeout <= 0 || (System.currentTimeMillis() - start) < timeout);
	}
	
	@Override
	public boolean blockPauseCondition() {
		return cancel == null && error == null && !end && waitingData.isEmpty();
	}
	
	@Override
	public void onDone(Runnable listener) {
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
	protected void unlock() {
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
