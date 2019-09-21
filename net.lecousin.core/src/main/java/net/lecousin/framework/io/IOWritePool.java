package net.lecousin.framework.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.AsyncWork.AsyncWorkListener;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;

/**
 * This class can be used if you are writing to a IO.Writable, but you don't want to wait for the previous write to be done before
 * to write again. When writing to this class, if the previous write is not yet finished, the buffer is pushed to a queue,
 * and will be written as soon as the previous write is finished.
 * At the end of all write operations, you can use the method onDone to get a synchronization point that will be unblocked
 * as soon as the last write operation is finished.
 * <br/>
 * In case of many write operations, this class may queue many buffers to be written, then using an important amount of memory.
 * You can consider using {@link net.lecousin.framework.io.util.LimitWriteOperations} class instead.
 */
public class IOWritePool {
	
	/** Constructor. */
	public IOWritePool(IO.Writable io) {
		this.io = io;
	}

	private IO.Writable io;
	private LinkedList<ByteBuffer> buffers = new LinkedList<>();
	private AsyncWork<Integer,IOException> writing = null;
	private AsyncWorkListener<Integer, IOException> listener = new Listener();
	private SynchronizationPoint<IOException> waitDone = null;
	
	/** Write the given buffer. */
	public void write(ByteBuffer buffer) throws IOException {
		synchronized (buffers) {
			if (writing == null) {
				writing = io.writeAsync(buffer);
				writing.listenInline(listener);
				return;
			}
			if (writing.hasError()) throw writing.getError();
			buffers.add(buffer);
		}
	}
	
	/**
	 * Must be called once all write operations have been done, and only one time.
	 */
	public SynchronizationPoint<IOException> onDone() {
		synchronized (buffers) {
			if (writing == null) return new SynchronizationPoint<>(true);
			if (writing.hasError()) return new SynchronizationPoint<>(writing.getError());
			if (writing.isCancelled()) return new SynchronizationPoint<>(writing.getCancelEvent());
			if (waitDone == null) waitDone = new SynchronizationPoint<>();
		}
		return waitDone;
	}
	
	private class Listener implements AsyncWorkListener<Integer, IOException> {

		@Override
		public void ready(Integer result) {
			synchronized (buffers) {
				if (!buffers.isEmpty()) {
					writing = io.writeAsync(buffers.removeFirst());
					writing.listenInline(listener);
					return;
				}
				writing = null;
				if (waitDone != null)
					waitDone.unblock();
			}
		}

		@Override
		public void error(IOException error) {
			if (waitDone != null) waitDone.error(error);
		}

		@Override
		public void cancelled(CancelException event) {
			if (waitDone != null) waitDone.cancel(event);
		}
		
	}
	
}
