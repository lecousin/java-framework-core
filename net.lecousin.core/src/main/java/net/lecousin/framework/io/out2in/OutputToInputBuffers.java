package net.lecousin.framework.io.out2in;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.LockPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Implementation of IO.OutputToInput using a list of ByteBuffer, that are stored in memory while
 * writing, and removed while reading them.<br/>
 * Optionally a maximum number of pending buffers can be specified. In that case, a write operation can be blocked
 * if the maximum is reached, until a buffer is consumed.
 */
public class OutputToInputBuffers extends ConcurrentCloseable<IOException>
	implements IO.OutputToInput, IO.Writable, IO.Readable, IO.Readable.Buffered {

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
	private LinkedList<Async<NoException>> lockMaxBuffers;
	private byte priority;
	private AsyncSupplier<?,?> lastWrite = null;
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		eof = true;
		lock.unlock();
		if (maxPendingBuffers > 0)
			while (!lockMaxBuffers.isEmpty())
				lockMaxBuffers.removeFirst().unblock();
		return null;
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		buffers = null;
		ondone.unblock();
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
	public void signalErrorBeforeEndOfData(IOException error) {
		lock.error(error);
	}
	
	@Override
	public void endOfData() {
		AsyncSupplier<?,?> lw;
		synchronized (this) {
			lw = lastWrite;
		}
		if (lw == null || lw.isDone()) {
			eof = true;
			lock.unlock();
			return;
		}
		lw.onDone(() -> {
			eof = true;
			lock.unlock();
		});
	}
	
	
	@Override
	public boolean isFullDataAvailable() {
		return eof;
	}
	
	@Override
	public long getAvailableDataSize() {
		long total = 0;
		synchronized (this) {
			for (ByteBuffer b : buffers)
				total += b.remaining();
		}
		return total;
	}
	
	@Override
	public IAsync<IOException> canStartWriting() {
		return new Async<>(true);
	}
	
	@Override
	public int writeSync(ByteBuffer buffer) {
		if (maxPendingBuffers > 0) {
			do {
				Async<NoException> sp = null;
				synchronized (this) {
					if (isClosing() || isClosed()) return 0;
					if (buffers.size() >= maxPendingBuffers) {
						sp = new Async<>();
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
	public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		 Task.Cpu<Integer, IOException> task = new Task.Cpu<Integer, IOException>("OutputToInput.write", getPriority(), ondone) {
			@Override
			public Integer run() {
				return Integer.valueOf(writeSync(buffer));
			}
		};
		Async<NoException> sp = null;
		synchronized (this) {
			lastWrite = task.getOutput();
			if (maxPendingBuffers > 0) {
				if (isClosing() || isClosed()) return new AsyncSupplier<>(null, null, IO.cancelClosed());
				if (buffers.size() >= maxPendingBuffers) {
					sp = new Async<>();
					lockMaxBuffers.addLast(sp);
				}
			}
		}
		if (sp == null)
			task.start();
		else
			task.startOn(sp, true);
		return operation(task).getOutput();
	}
	
	@Override
	public IAsync<IOException> canStartReading() {
		synchronized (this) {
			if (!buffers.isEmpty()) return new Async<>(true);
			if (eof) return new Async<>(true);
			if (lock.hasError()) return lock;
		}
		if (lock.isDone()) lock.lock();
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
					throw new OutputToInputTransferException(lock.getError());
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
			Async<NoException> sp = null;
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
	public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		int done = 0;
		do {
			ByteBuffer b = null;
			synchronized (this) {
				if (isClosing() || isClosed()) {
					IOException e = new IOException("IO closed");
					if (ondone != null) ondone.accept(new Pair<>(null, e));
					return new AsyncSupplier<>(null, e);
				}
				if (!buffers.isEmpty())
					b = buffers.get(0);
				else if (eof) {
					Integer r = Integer.valueOf(done > 0 ? done : -1);
					if (ondone != null) ondone.accept(new Pair<>(r, null));
					return new AsyncSupplier<>(r, null);
				} else if (lock.hasError()) {
					IOException e = new OutputToInputTransferException(lock.getError());
					if (ondone != null) ondone.accept(new Pair<>(null, e));
					return new AsyncSupplier<>(null, e);
				} else /*if (!lock.isUnblocked())*/ {
					if (done == 0)
						return readFullyAsync(buffer, ondone);
					AsyncSupplier<Integer, IOException> r = new AsyncSupplier<>();
					int d = done;
					readFullyAsync(buffer, res -> {
						if (ondone != null) {
							if (res.getValue1() != null) {
								int n = res.getValue1().intValue();
								if (n < 0) n = 0;
								n += d;
								ondone.accept(new Pair<>(Integer.valueOf(n), null));
							} else {
								ondone.accept(res);
							}
						}
					}).onDone(nb -> {
						int n = nb.intValue();
						if (n < 0) n = 0;
						n += d;
						r.unblockSuccess(Integer.valueOf(n));
					}, r);
					return r;
				} 
			}
			int len = buffer.remaining();
			if (len >= b.remaining()) {
				done += b.remaining();
				buffer.put(b);
			} else {
				int limit = b.limit();
				b.limit(b.position() + len);
				buffer.put(b);
				b.limit(limit);
				done += len;
			}
			if (b.remaining() == 0) {
				Async<NoException> sp = null;
				synchronized (this) {
					buffers.removeFirst();
					if (maxPendingBuffers > 0)
						sp = lockMaxBuffers.pollFirst();
				}
				if (sp != null) sp.unblock();
			}
			if (!buffer.hasRemaining()) {
				Integer r = Integer.valueOf(done);
				if (ondone != null) ondone.accept(new Pair<>(r, null));
				return new AsyncSupplier<>(r, null);
			}
		} while (true);
	}
	
	@Override
	public int readAsync() throws IOException {
		ByteBuffer b = null;
		synchronized (this) {
			if (isClosing() || isClosed()) throw new IOException("IO closed");
			if (!buffers.isEmpty())
				b = buffers.get(0);
			else if (eof)
				return -1;
			else if (!lock.isDone())
				return -2;
			else if (lock.hasError())
				throw new OutputToInputTransferException(lock.getError());
			else
				return -2;
		}
		int res = b.get() & 0xFF;
		if (b.remaining() == 0) {
			Async<NoException> sp = null;
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
	public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		Task<Integer, IOException> task = new Task.Cpu<Integer, IOException>("OutputToInput.read", getPriority(), ondone) {
			@Override
			public Integer run() throws IOException {
				return Integer.valueOf(readSync(buffer));
			}
		};
		operation(task.start());
		return task.getOutput();
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsync(this, buffer, ondone));
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
						throw new OutputToInputTransferException(lock.getError());
				}
				lock.lock();
			} while (true);
			
			int nb = b.remaining();
			if (nb > n) {
				b.position(b.position() + (int)n);
				return done + n;
			}
			Async<NoException> sp = null;
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
	public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		return operation(IOUtil.skipAsyncUsingSync(this, n, ondone));
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
					throw new OutputToInputTransferException(lock.getError());
			}
			lock.lock();
		} while (true);
		int res = b.get() & 0xFF;
		if (b.remaining() == 0) {
			Async<NoException> sp = null;
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
					throw new OutputToInputTransferException(lock.getError());
			}
			lock.lock();
		} while (true);
		
		int nb = b.remaining();
		if (nb <= len) {
			b.get(buffer, offset, nb);
			len = nb;
		} else {
			b.get(buffer, offset, len);
		}
		if (b.remaining() == 0) {
			Async<NoException> sp = null;
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
					throw new OutputToInputTransferException(lock.getError());
			}
			lock.lock();
		} while (true);
		int nb = b.remaining();
		if (nb <= skip) {
			Async<NoException> sp = null;
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
	public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		Task.Cpu<ByteBuffer, IOException> task = new Task.Cpu.FromSupplierThrows<>(
			"Peek next buffer from OutputToInputBuffers", getPriority(), ondone, this::readNextBuffer);
		operation(task).startOn(canStartReading(), true);
		return task.getOutput();
	}
	
	@Override
	public ByteBuffer readNextBuffer() throws IOException {
		Async<NoException> sp = null;
		ByteBuffer b = null;
		do {
			synchronized (this) {
				if (isClosing() || isClosed()) throw new IOException("IO closed");
				if (!buffers.isEmpty()) {
					b = buffers.removeFirst();
					if (maxPendingBuffers > 0)
						sp = lockMaxBuffers.pollFirst();
					break;
				}
				if (eof) break;
				if (lock.hasError())
					throw new OutputToInputTransferException(lock.getError());
			}
			lock.lock();
		} while (true);
		if (sp != null) sp.unblock();
		return b;
	}
}
