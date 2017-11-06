package net.lecousin.framework.io.out2in;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.LockPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.AbstractIO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Implementation of IO.OutputToInput using a list of ByteBuffer, that are stored in memory while
 * writing, and removed while reading them.<br/>
 * Optionally a maximum number of pending buffers can be specified. In that case, a write operation can be blocked
 * if the maximum is reached, until a buffer is consumed.
 */
public class OutputToInputBuffers extends AbstractIO implements IO.OutputToInput, IO.Writable, IO.Readable, IO.Readable.Buffered {

	/** Constructor.
	 * @param copyReceivedBuffers if true, the buffer receive through write operation are copied, so they can be reused by the calling process
	 * @param maxPendingBuffers maximum number of buffers before to block write operations, or 0 for no limit.
	 * @param priority asynchronous operations priority
	 */
	public OutputToInputBuffers(boolean copyReceivedBuffers, int maxPendingBuffers, byte priority) {
		if (maxPendingBuffers < 0) maxPendingBuffers = 0;
		this.copyReceivedBuffers = copyReceivedBuffers;
		this.maxPendingBuffers = maxPendingBuffers;
		this.priority = priority;
		if (maxPendingBuffers > 0) lockMaxBuffers = new LinkedList<>();
	}

	/** Constructor.
	 * @param copyReceivedBuffers if true, the buffer receive through write operation are copied, so they can be reused by the calling process
	 * @param priority asynchronous operations priority
	 */
	public OutputToInputBuffers(boolean copyReceivedBuffers, byte priority) {
		this(copyReceivedBuffers, 0, priority);
	}
	
	private boolean copyReceivedBuffers;
	private int maxPendingBuffers;
	private LinkedList<ByteBuffer> buffers = new LinkedList<>();
	private boolean eof = false;
	private LockPoint<IOException> lock = new LockPoint<>();
	private LinkedList<SynchronizationPoint<NoException>> lockMaxBuffers;
	private byte priority;
	private AsyncWork<?,?> lastWrite = null;
	
	@Override
	public void signalErrorBeforeEndOfData(IOException error) {
		lock.error(error);
	}
	
	@Override
	public void endOfData() {
		AsyncWork<?,?> lw;
		synchronized (this) {
			lw = lastWrite;
		}
		if (lw == null || lw.isUnblocked()) {
			eof = true;
			lock.unlock();
			return;
		}
		lw.listenInline(new Runnable() {
			@Override
			public void run() {
				eof = true;
				lock.unlock();
			}
		});
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartWriting() {
		return new SynchronizationPoint<>(true);
	}
	
	@Override
	public int writeSync(ByteBuffer buffer) {
		if (maxPendingBuffers > 0) {
			do {
				SynchronizationPoint<NoException> sp = null;
				synchronized (this) {
					if (buffers.size() >= maxPendingBuffers) {
						sp = new SynchronizationPoint<>();
						lockMaxBuffers.addLast(sp);
					}
				}
				if (sp == null) break;
				sp.block(0);
			} while (true);
		}
		if (copyReceivedBuffers) {
			ByteBuffer b = ByteBuffer.allocate(buffer.remaining());
			b.put(buffer);
			b.flip();
			synchronized (this) {
				buffers.add(b);
			}
			lock.unlock();
			return b.remaining();
		}
		synchronized (this) {
			buffers.add(buffer);
		}
		lock.unlock();
		return buffer.remaining();
	}
	
	@Override
	public AsyncWork<Integer, IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		 Task.Cpu<Integer, IOException> task = new Task.Cpu<Integer, IOException>("OutputToInput.write", getPriority(), ondone) {
			@Override
			public Integer run() {
				return Integer.valueOf(writeSync(buffer));
			}
		};
		SynchronizationPoint<NoException> sp = null;
		synchronized (this) {
			lastWrite = task.getSynch();
			if (maxPendingBuffers > 0) {
				if (buffers.size() >= maxPendingBuffers) {
					sp = new SynchronizationPoint<>();
					lockMaxBuffers.addLast(sp);
				}
			}
		}
		if (sp == null)
			task.start();
		else
			task.startOn(sp, true);
		return task.getSynch();
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		synchronized (this) {
			if (!buffers.isEmpty()) return new SynchronizationPoint<>(true);
			if (eof) return new SynchronizationPoint<>(true);
			if (lock.hasError()) return lock;
		}
		return lock;
	}
	
	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		ByteBuffer b = null;
		do {
			synchronized (this) {
				if (!buffers.isEmpty()) {
					b = buffers.get(0);
					break;
				}
				if (eof)
					return -1;
				if (lock.hasError())
					throw new IOException("An error occured during the transfer of data", lock.getError());
			}
			lock.lock();
		} while (true);
		
		int nb = b.remaining();
		if (nb <= buffer.remaining()) {
			buffer.put(b);
		} else {
			int l = b.limit();
			b.limit(l - (nb - buffer.remaining()));
			nb = buffer.remaining();
			buffer.put(b);
			b.limit(l);
		}
		if (b.remaining() == 0) {
			SynchronizationPoint<NoException> sp = null;
			synchronized (this) {
				buffers.removeFirst();
				if (maxPendingBuffers > 0)
					sp = lockMaxBuffers.pollFirst();
			}
			if (sp != null) sp.unblock();
		}
		return nb;
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}
	
	@Override
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		Task<Integer, IOException> task = new Task.Cpu<Integer, IOException>("OutputToInput.read", getPriority(), ondone) {
			@Override
			public Integer run() throws IOException {
				return Integer.valueOf(readSync(buffer));
			}
		};
		task.start();
		return task.getSynch();
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsynch(this, buffer, ondone);
	}
	
	@Override
	public long skipSync(long n) throws IOException {
		long done = 0;
		while (n > 0) {
			ByteBuffer b = null;
			do {
				synchronized (this) {
					if (!buffers.isEmpty()) {
						b = buffers.get(0);
						break;
					}
					if (eof)
						return done;
					if (lock.hasError())
						throw new IOException("An error occured during the transfer of data", lock.getError());
				}
				lock.lock();
			} while (true);
			
			int nb = b.remaining();
			if (nb > n) {
				b.position(b.position() + (int)n);
				return done + n;
			}
			SynchronizationPoint<NoException> sp = null;
			synchronized (this) {
				buffers.removeFirst();
				if (maxPendingBuffers > 0)
					sp = lockMaxBuffers.pollFirst();
			}
			if (sp != null) sp.unblock();
			done += nb;
			n -= nb;
		}
		return done;
	}
	
	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		return IOUtil.skipAsyncUsingSync(this, n, ondone);
	}
	
	@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
	@Override
	protected ISynchronizationPoint<IOException> closeIO() {
		eof = true;
		lock.unlock();
		if (maxPendingBuffers > 0)
			while (!lockMaxBuffers.isEmpty())
				lockMaxBuffers.removeFirst().unblock();
		return new SynchronizationPoint<>(true);
	}
	
	@Override
	public byte getPriority() { return priority; }
	
	@Override
	public void setPriority(byte priority) { this.priority = priority; }
	
	@Override
	public String getSourceDescription() {
		return "OutputToInput";
	}
	
	@Override
	public TaskManager getTaskManager() {
		return Threading.getCPUTaskManager();
	}
	
	@Override
	public IO getWrappedIO() {
		return null;
	}

	@Override
	public int read() throws IOException {
		ByteBuffer b = null;
		do {
			synchronized (this) {
				if (!buffers.isEmpty()) {
					b = buffers.get(0);
					break;
				}
				if (eof)
					return -1;
				if (lock.hasError())
					throw new IOException("An error occured during the transfer of data", lock.getError());
			}
			lock.lock();
		} while (true);
		int res = b.get() & 0xFF;
		if (b.remaining() == 0) {
			SynchronizationPoint<NoException> sp = null;
			synchronized (this) {
				buffers.removeFirst();
				if (maxPendingBuffers > 0)
					sp = lockMaxBuffers.pollFirst();
			}
			if (sp != null) sp.unblock();
		}
		return res;
	}

	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		ByteBuffer b = null;
		do {
			synchronized (this) {
				if (!buffers.isEmpty()) {
					b = buffers.get(0);
					break;
				}
				if (eof)
					return -1;
				if (lock.hasError())
					throw new IOException("An error occured during the transfer of data", lock.getError());
			}
			lock.lock();
		} while (true);
		
		int nb = b.remaining();
		if (nb <= len) {
			b.get(buffer, offset, nb);
			len = nb;
		} else
			b.get(buffer, offset, len);
		if (b.remaining() == 0) {
			SynchronizationPoint<NoException> sp = null;
			synchronized (this) {
				buffers.removeFirst();
				if (maxPendingBuffers > 0)
					sp = lockMaxBuffers.pollFirst();
			}
			if (sp != null) sp.unblock();
		}
		return len;
	}

	@Override
	public int readFully(byte[] buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}

	@Override
	public int skip(int skip) throws IOException {
		if (skip <= 0) return 0;
		ByteBuffer b = null;
		do {
			synchronized (this) {
				if (!buffers.isEmpty()) {
					b = buffers.get(0);
					break;
				}
				if (eof)
					return 0;
				if (lock.hasError())
					throw new IOException("An error occured during the transfer of data", lock.getError());
			}
			lock.lock();
		} while (true);
		int nb = b.remaining();
		if (nb <= skip) {
			SynchronizationPoint<NoException> sp = null;
			synchronized (this) {
				buffers.removeFirst();
				if (maxPendingBuffers > 0)
					sp = lockMaxBuffers.pollFirst();
			}
			if (sp != null) sp.unblock();
			if (nb == skip) return skip;
			return nb + skip(skip - nb);
		}
		b.position(b.position() + skip);
		return skip;
	}

	@Override
	public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		Task.Cpu<ByteBuffer, IOException> task = new Task.Cpu<ByteBuffer, IOException>(
			"Peek next buffer from OutputToInputBuffers", getPriority(), ondone
		) {
			@Override
			public ByteBuffer run() throws IOException {
				SynchronizationPoint<NoException> sp = null;
				ByteBuffer b = null;
				do {
					synchronized (this) {
						if (!buffers.isEmpty()) {
							b = buffers.removeFirst();
							if (maxPendingBuffers > 0)
								sp = lockMaxBuffers.pollFirst();
							break;
						}
						if (eof) break;
						if (lock.hasError())
							throw new IOException("An error occured during the transfer of data", lock.getError());
					}
					lock.lock();
				} while (true);
				if (sp != null) sp.unblock();
				return b;
			}
		};
		task.startOn(canStartReading(), true);
		return task.getSynch();
	}
}
