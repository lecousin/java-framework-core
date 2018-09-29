package net.lecousin.framework.concurrent.synch;

import java.util.LinkedList;

import net.lecousin.framework.exception.NoException;

/**
 * A ReadWriteLockPoint is like a lock point but allows several <i>readers</i> at the same time.
 * <ul>
 * <li>Several threads can be in <i>read mode</i> at the same time</li>
 * <li>Only one thread can be in <i>write mode</i> at the same time</li>
 * <li>The read and write modes are exclusive</li>
 * </ul>
 */
public class ReadWriteLockPoint {

	private int readers = 0;
	private boolean writer = false;
	private SynchronizationPoint<NoException> readerWaiting = null;
	private LinkedList<SynchronizationPoint<NoException>> writersWaiting = null;
	
	/** To call when a thread wants to enter read mode.
	 * If the lock point is used in write mode, this method will block until it is released.
	 */
	public void startRead() {
		SynchronizationPoint<NoException> sp;
		synchronized (this) {
			if (!writer) {
				// nobody is writing, we can read
				readers++;
				return;
			}
			// if someone is writing, we need to wait
			if (readerWaiting == null)
				readerWaiting = new SynchronizationPoint<NoException>();
			sp = readerWaiting;
			readers++;
		}
		sp.block(0);
	}

	/** To call when a thread wants to enter read mode.
	 * If read can start immediately, this method returns null.
	 * If the lock point is used in write mode, this method will return a SynchronizationPoint unblocked when read can start.
	 */
	public SynchronizationPoint<NoException> startReadAsync() {
		return startReadAsync(false);
	}

	/** To call when a thread wants to enter read mode.
	 * If the lock point is used in write mode, this method will return a SynchronizationPoint unblocked when read can start.
	 */
	public SynchronizationPoint<NoException> startReadAsync(boolean returnNullIfReady) {
		SynchronizationPoint<NoException> sp;
		synchronized (this) {
			if (!writer) {
				// nobody is writing, we can read
				readers++;
				return returnNullIfReady ? null : new SynchronizationPoint<>(true);
			}
			// if someone is writing, we need to wait
			if (readerWaiting == null)
				readerWaiting = new SynchronizationPoint<NoException>();
			sp = readerWaiting;
			readers++;
		}
		return sp;
	}
	
	/** To call when the thread leaves the read mode and release this lock point. */
	public void endRead() {
		SynchronizationPoint<NoException> sp;
		synchronized (this) {
			// if others are still reading, nothing to do
			if (--readers > 0) return;
			// if nobody is waiting to write, nothing to do
			if (writersWaiting == null) return;
			sp = writersWaiting.removeFirst();
			if (writersWaiting.isEmpty()) writersWaiting = null;
			writer = true;
		}
		sp.unblock();
	}
	
	/** To call when a thread wants to enter write mode.
	 * If the lock point is used in read mode, this method will block until it is released.
	 */
	public void startWrite() {
		SynchronizationPoint<NoException> sp;
		synchronized (this) {
			// if nobody is using the resource, we can start writing
			if (readers == 0 && !writer) {
				writer = true;
				return;
			}
			// someone is doing something, we need to block
			sp = new SynchronizationPoint<NoException>();
			if (writersWaiting == null) writersWaiting = new LinkedList<>();
			writersWaiting.add(sp);
		}
		sp.block(0);
	}

	/** To call when a thread wants to enter write mode.
	 * If write can start immediately, this method returns null.
	 * If the lock point is used in read mode, this method will return a SynchronizationPoint unblocked when write can start.
	 */
	public SynchronizationPoint<NoException> startWriteAsync() {
		return startWriteAsync(false);
	}
	
	/** To call when a thread wants to enter write mode.
	 * If write can start immediately, this method returns null.
	 * If the lock point is used in read mode, this method will return a SynchronizationPoint unblocked when write can start.
	 */
	public SynchronizationPoint<NoException> startWriteAsync(boolean returnNullIfReady) {
		SynchronizationPoint<NoException> sp;
		synchronized (this) {
			// if nobody is using the resource, we can start writing
			if (readers == 0 && !writer) {
				writer = true;
				return returnNullIfReady ? null : new SynchronizationPoint<>(true);
			}
			// someone is doing something, we need to block
			sp = new SynchronizationPoint<NoException>();
			if (writersWaiting == null) writersWaiting = new LinkedList<>();
			writersWaiting.add(sp);
		}
		return sp;
	}

	/** To call when the thread leaves the write mode and release this lock point. */
	public void endWrite() {
		SynchronizationPoint<NoException> sp;
		synchronized (this) {
			if (readerWaiting != null) {
				// some readers are waiting, we unblock them
				sp = readerWaiting;
				readerWaiting = null;
				writer = false;
			} else if (writersWaiting != null) {
				sp = writersWaiting.removeFirst();
				if (writersWaiting.isEmpty()) writersWaiting = null;
				// writer stay true
			} else {
				// nobody is waiting
				writer = false;
				return;
			}
		}
		sp.unblock();
	}
	
	/**
	 * Return true if there is currently neither reader or writer.
	 * Note that this does not mean starting operation won't block because between the call of this method and the call to a startXXX method,
	 * another thread may have called a startXXX method.
	 */
	public boolean isUsed() {
		return readers > 0 || writer;
	}
	
}
