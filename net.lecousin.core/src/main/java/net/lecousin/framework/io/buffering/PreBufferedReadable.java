package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.function.Consumer;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.memory.ByteArrayCache;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Often the best implementation when reading sequentially (streaming, without seek): it fills a first buffer
 * as soon as instantiated, once the first buffer is filled new buffers are pre-filled.
 * <br/>
 * We can specify the maximum number of buffers to be pre-filled to limit memory usage.
 * Once a buffer is completely read, it becomes available to be pre-filled again with new data.
 * <br/>
 * The goal of this implementation is to fill buffers while data is computed, reducing the time waiting for
 * read operations (ideally not blocking if buffers are filled fats enough compare to the data computation).
 */
public class PreBufferedReadable extends ConcurrentCloseable<IOException> implements IO.Readable.Buffered {

	/** Constructor. */
	public PreBufferedReadable(
		IO.Readable src, int firstBuffer, Priority firstBufferPriority, int nextBuffer, Priority nextBufferPriority, int maxNbNextBuffersReady
	) {
		this.src = src;
		this.priority = firstBufferPriority;
		if (src instanceof IO.KnownSize) {
			AsyncSupplier<Long,IOException> getSize = ((IO.KnownSize)src).getSizeAsync();
			Task<Void,NoException> start = Task.cpu(
				"Start PreBufferedReadable after size is known", firstBufferPriority, task -> {
				if (getSize.hasError()) {
					PreBufferedReadable.this.error = getSize.getError();
					synchronized (PreBufferedReadable.this) {
						if (dataReady != null) {
							dataReady.unblock();
							dataReady = null;
						}
					}
					return null;
				}
				if (getSize.isCancelled()) return null;
				size = getSize.getResult().longValue();
				startWithKnownSize(firstBuffer, firstBufferPriority, nextBuffer, nextBufferPriority, maxNbNextBuffersReady);
				return null;
			});
			operation(start).startOn(getSize, true);
		} else {
			start(firstBuffer, firstBufferPriority, nextBuffer, nextBufferPriority, maxNbNextBuffersReady);
		}
	}
	
	/** Constructor. */
	public <T extends IO.Readable.Seekable & IO.KnownSize> PreBufferedReadable(
		T src, int firstBuffer, Priority firstBufferPriority, int nextBuffer, Priority nextBufferPriority, int maxNbNextBuffersReady
	) throws IOException {
		this.src = src;
		this.priority = firstBufferPriority;
		this.read = src.getPosition();
		this.size = src.getSizeSync();
		startWithKnownSize(firstBuffer, firstBufferPriority, nextBuffer, nextBufferPriority, maxNbNextBuffersReady);
	}
	
	/** Constructor. */
	public PreBufferedReadable(
		IO.Readable src, long size, int firstBuffer, Priority firstBufferPriority, int nextBuffer, Priority nextBufferPriority,
		int maxNbNextBuffersReady
	) {
		this.src = src;
		this.priority = firstBufferPriority;
		this.size = size;
		startWithKnownSize(firstBuffer, firstBufferPriority, nextBuffer, nextBufferPriority, maxNbNextBuffersReady);
	}
	
	/** Constructor. */
	public <T extends IO.Readable.Seekable & IO.KnownSize> PreBufferedReadable(
		T src, long size, int firstBuffer, Priority firstBufferPriority,
		int nextBuffer, Priority nextBufferPriority, int maxNbNextBuffersReady
	) throws IOException {
		this.src = src;
		this.priority = firstBufferPriority;
		this.read = src.getPosition();
		this.size = size;
		startWithKnownSize(firstBuffer, firstBufferPriority, nextBuffer, nextBufferPriority, maxNbNextBuffersReady);
	}
	
	private void startWithKnownSize(
		int firstBuffer, Priority firstBufferPriority, int nextBuffer, Priority nextBufferPriority, int maxNbNextBuffersReady
	) {
		if (size == read) {
			synchronized (PreBufferedReadable.this) {
				endReached = true;
				if (dataReady != null) {
					dataReady.unblock();
					dataReady = null;
				}
			}
		} else if (size - read <= firstBuffer) {
			start((int)(size - read), firstBufferPriority, 0, null, 0);
		} else if (size - read <= firstBuffer + nextBuffer) {
			start(firstBuffer, firstBufferPriority, (int)(size - read - firstBuffer), nextBufferPriority, 1);
		} else {
			start(firstBuffer, firstBufferPriority, nextBuffer, nextBufferPriority, maxNbNextBuffersReady);
		}
	}
	
	private IO.Readable src;
	private Priority priority;
	private IOException error = null;
	private long size = -1;
	private long read = 0;
	private boolean endReached = false;
	private boolean stopReading = false;
	private ByteBuffer current = null;
	private int nextBufferSize;
	private Async<NoException> dataReady = null;
	private TurnArray<ByteBuffer> buffersReady;
	private AsyncSupplier<?,?> nextReadTask = null;
	
	private static class UnexpectedEnd extends IOException {
		private static final long serialVersionUID = 1L;

		public UnexpectedEnd(PreBufferedReadable io) {
			super("Unexpected end after " + io.read + " bytes read, known size is " + io.size);
		}
	}
	
	@Override
	public String getSourceDescription() { return src != null ? src.getSourceDescription() : "closed"; }
	
	/** Return the next synchronization point that will be unblocked once data is ready to be read. */
	public Async<IOException> getDataReadySynchronization() {
		synchronized (this) {
			Async<IOException> sp = new Async<>();
			if (error != null) {
				sp.error(error);
				return sp;
			} else if (current != null || endReached) {
				sp.unblock();
				return sp;
			}
			if (isClosing() || isClosed()) {
				sp.cancel(IO.cancelClosed());
				return sp;
			}
			if (dataReady == null)
				dataReady = new Async<>();
			dataReady.onDone(() -> {
				if (error != null)
					sp.error(error);
				else
					sp.unblock();
			});
			return sp;
		}
	}
	
	@Override
	public IAsync<IOException> canStartReading() {
		return getDataReadySynchronization();
	}
	
	@Override
	public IO getWrappedIO() {
		return src;
	}
	
	@Override
	public TaskManager getTaskManager() {
		// should be buffered...
		return Threading.getCPUTaskManager();
	}
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		AsyncSupplier<?,?> nextRead;
		synchronized (this) {
			nextRead = nextReadTask;
		}
		if (nextRead != null && !nextRead.isDone())
			nextRead.cancel(IO.cancelClosed());
		while (dataReady != null) {
			Async<NoException> dr;
			synchronized (this) {
				dr = dataReady;
				dataReady = null;
			}
			if (dr != null)
				dr.unblock();
		}
		return src.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		Async<NoException> dr = null;
		synchronized (this) {
			endReached = true;
			if (dataReady != null) {
				dr = dataReady;
				dataReady = null;
			}
			buffersReady = null;
			nextReadTask = null;
		}
		if (dr != null)
			dr.unblock();
		src = null;
		ondone.unblock();
	}
	
	@Override
	public Priority getPriority() { return priority; }
	
	@Override
	public void setPriority(Priority priority) { this.priority = priority; }
	
	@SuppressWarnings({
		"squid:S2259", // false positive: firstNextReadTask cannot be null if nextBuffer > 0
		"squid:S3776" // complexity
	})
	private void start(int firstBuffer, Priority firstBufferPriority, int nextBuffer, Priority nextBufferPriority, int maxNbNextBuffersReady) {
		if (nextBuffer < 0)
			throw new IllegalArgumentException("next buffer size must be positive, or zero to disable it, given: " + nextBuffer);
		if (maxNbNextBuffersReady < 0)
			throw new IllegalArgumentException(
				"maximum number of next buffers must be positive, or zero to disable it, given: " + maxNbNextBuffersReady);
		if (nextBuffer == 0) maxNbNextBuffersReady = 0;
		if (maxNbNextBuffersReady == 0) nextBuffer = 0;
		nextBufferSize = nextBuffer;
		buffersReady = new TurnArray<>(maxNbNextBuffersReady + 1);
		// first read
		ByteBuffer buffer = ByteBuffer.wrap(ByteArrayCache.getInstance().get(firstBuffer, true));
		src.setPriority(firstBufferPriority);
		JoinPoint<NoException> jpNextRead = new JoinPoint<>();
		jpNextRead.addToJoin(1);
		AsyncSupplier<Integer,IOException> firstReadTask;
		if (nextBuffer > 0)
			firstReadTask = operation(src.readAsync(buffer));
		else
			firstReadTask = operation(src.readFullyAsync(buffer));
		
		Task<Void,NoException> firstNextReadTask = null;
		if (nextBuffer > 0) {
			firstNextReadTask = Task.cpu("First next read of pre-buffered IO " + getSourceDescription(), nextBufferPriority, task -> {
				Async<NoException> dr = null;
				synchronized (PreBufferedReadable.this) {
					nextReadTask = null;
					if (error == null && !endReached && !stopReading && !isClosing() && !isClosed())
						nextRead();
					else if (dataReady != null) {
						dr = dataReady;
						dataReady = null;
					}
				}
				if (dr != null) dr.unblock();
				return null;
			});
			nextReadTask = firstNextReadTask.getOutput();
		}
		
		boolean singleRead = nextBuffer <= 0;
		firstReadTask.onDone(() -> {
			if (buffersReady == null) {
				jpNextRead.joined();
				return;
			}
			buffer.flip();
			synchronized (PreBufferedReadable.this) {
				if (firstReadTask.isCancelled()) {
					if (dataReady != null) {
						Async<NoException> dr = dataReady;
						dataReady = null;
						dr.unblock();
					}
					jpNextRead.joined();
					return;
				}
				Throwable e = firstReadTask.getError();
				if (singleRead && e == null && firstReadTask.getResult().intValue() < size)
					e = new IOException("Only " + firstReadTask.getResult().intValue() + " bytes read, expected is " + size);
				if (e != null) {
					if (e instanceof IOException) error = (IOException)e;
					else error = new IOException("Read failed", e);
					if (dataReady != null) {
						Async<NoException> dr = dataReady;
						dataReady = null;
						dr.unblock();
					}
				} else {
					if (buffer.remaining() == 0)
						endReached = true;
					else
						current = buffer;
					read += buffer.remaining();
					if (size > 0 && read == size) 
						endReached = true;
					if (endReached && size > 0 && read < size) {
						error = new UnexpectedEnd(this);
					}
					if (dataReady != null) {
						Async<NoException> dr = dataReady;
						dataReady = null;
						dr.unblock();
					}
				}
			}
			if (buffersReady == null) {
				jpNextRead.joined();
				return;
			}
			src.setPriority(nextBufferPriority);
			jpNextRead.joined();
		});
		if (nextBuffer > 0) {
			operation(firstNextReadTask.getOutput());
			jpNextRead.start();
			jpNextRead.thenStart(firstNextReadTask, true);
		}
	}
	
	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		do {
			Async<NoException> sp;
			synchronized (this) {
				if (error != null) throw error;
				if (current != null) break;
				if (endReached) return -1;
				if (isClosing() || isClosed()) return -1;
				if (dataReady == null) dataReady = new Async<>();
				sp = dataReady;
			}
			sp.block(0);
		} while (true);
		if (buffersReady == null) throw new ClosedChannelException();
		int nb = buffer.remaining();
		if (current.remaining() > nb) {
			int limit = current.limit();
			current.limit(limit - (current.remaining() - nb));
			buffer.put(current);
			current.limit(limit);
			return nb;
		}
		nb = current.remaining();
		buffer.put(current);
		moveNextBuffer(true);
		return nb;
	}
	
	@Override
	public int readAsync() throws IOException {
		synchronized (this) {
			if (error != null) throw error;
			if (current != null) {
				if (!current.hasRemaining() && endReached)
					return -1;
			} else {
				if (endReached) return -1;
				if (isClosing() || isClosed()) return -1;
				if (dataReady == null) dataReady = new Async<>();
				if (!dataReady.isDone())
					return -2;
			}
		}
		int res = current.get() & 0xFF;
		if (!current.hasRemaining())
			moveNextBuffer(true);
		return res;
	}

	@Override
	public AsyncSupplier<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		Async<NoException> sp = null;
		synchronized (this) {
			if (error != null) return IOUtil.error(error, ondone);
			if (current == null) {
				if (endReached) return IOUtil.success(Integer.valueOf(-1), ondone);
				if (isClosing() || isClosed()) return new AsyncSupplier<>(null, null, IO.cancelClosed());
				if (dataReady == null) dataReady = new Async<>();
				sp = dataReady;
			}
		}
		Task<Integer,IOException> t = Task.cpu("Async read on pre-buffered IO " + getSourceDescription(), getPriority(),
			task -> {
				synchronized (PreBufferedReadable.this) {
					if (error != null) throw error;
					if (buffersReady == null) {
						if (endReached) return Integer.valueOf(-1); // case of empty readable
						throw IO.cancelClosed();
					}
					if (current == null) {
						if (endReached) return Integer.valueOf(-1);
						if (isClosing() || isClosed()) throw IO.cancelClosed();
						throw new IOException("Unexpected error: current buffer is null but end is not reached");
					}
				}
				int nb = buffer.remaining();
				if (current.remaining() > nb) {
					int limit = current.limit();
					current.limit(limit - (current.remaining() - nb));
					buffer.put(current);
					current.limit(limit);
					return Integer.valueOf(nb);
				}
				nb = current.remaining();
				buffer.put(current);
				moveNextBuffer(true);
				return Integer.valueOf(nb);
			}, ondone);
		operation(t);
		if (sp == null) {
			t.start();
			return t.getOutput();
		}
		t.startOn(sp, false);
		return t.getOutput();
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}
	
	@Override
	public AsyncSupplier<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsync(this, buffer, ondone));
	}
	
	@Override
	@SuppressWarnings("squid:S3776") // complexity
	public long skipSync(long n) throws IOException {
		long skipped = 0;
		do {
			Async<NoException> sp = null;
			long remaining = -1;
			synchronized (this) {
				if (error != null) throw error;
				if (n <= 0) {
					stopReading = false;
					return 0;
				}
				if (current != null) {
					int nb = current.remaining();
					if (nb > n) {
						stopReading = false;
						current.position(current.position() + ((int)n));
						return n + skipped;
					}
					if (nb == n) {
						stopReading = false;
						moveNextBuffer(true);
						return n + skipped;
					}
					skipped += nb;
					if (buffersReady == null || nextBufferSize == 0) {
						// we reach the end
						endReached = true;
						if (size > 0 && read < size)
							error = new UnexpectedEnd(this);
						if (dataReady != null) dataReady.unblock();
						return skipped;
					}
					stopReading = true;
					moveNextBuffer(false);
					remaining = n - nb;
				} else {
					if (endReached) return skipped;
					if (nextReadTask == null && nextBufferSize > 0) {
						// we cancelled all buffers, let's do a move
						long n2 = src.skipSync(n);
						skipped += n2;
						read += n2;
						// restart reading
						stopReading = false;
						moveNextBuffer(true);
						return skipped;
					}
					if (isClosing() || isClosed()) return skipped;
					if (dataReady == null) dataReady = new Async<>();
					sp = dataReady;
				}
			}
			if (remaining > 0)
				n = remaining;
			else if (sp != null)
				sp.block(0);
		} while (true);		
	}
	
	@Override
	public int skip(int skip) throws IOException {
		return (int)skipSync(skip);
	}
	
	@Override
	public AsyncSupplier<Long,IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		synchronized (this) {
			if (error != null) return IOUtil.error(error, ondone);
			if (n <= 0) return IOUtil.success(Long.valueOf(0), ondone);
			return operation(Task.cpu("Skipping data from pre-buffered IO " + getSourceDescription(), priority,
				t -> Long.valueOf(skipSync(n)), ondone).start()).getOutput();
		}
	}
	
	private void moveNextBuffer(boolean startNextRead) {
		synchronized (this) {
			current = buffersReady.pollFirst();
			if (!endReached &&
				error == null &&
				nextReadTask == null &&
				startNextRead &&
				!stopReading)
				nextRead();
		}
	}
	
	@SuppressWarnings("squid:S1696") // NullPointerException
	private void nextRead() {
		if (isClosing() || isClosed()) {
			Async<NoException> dr;
			synchronized (this) {
				dr = dataReady;
				dataReady = null;
			}
			if (dr != null)
				dr.unblock();
			return;
		}
		ByteBuffer buffer = ByteBuffer.wrap(ByteArrayCache.getInstance().get(nextBufferSize, true));
		nextReadTask = operation(src.readFullyAsync(buffer));
		nextReadTask.onDone(() -> {
			if (handleNextReadError())
				return;
			int nb;
			try { nb = ((Integer)nextReadTask.getResult()).intValue(); }
			catch (NullPointerException ex) { nb = 0; /* we are closed */ }
			if (handleNextReadResult(nb, buffer))
				return;
		});
	}
	
	@SuppressWarnings("squid:S1696") // NullPointerException
	private boolean handleNextReadError() {
		if (nextReadTask == null) return true; // case we have been closed
		Throwable e;
		try { e = nextReadTask.getError(); }
		catch (NullPointerException ex) { return true; /* we are closed */ }
		if (e == null)
			return false;
		if (e instanceof IOException) error = (IOException)e;
		else error = new IOException("Read failed", e);
		nextReadTask = null;
		synchronized (PreBufferedReadable.this) {
			if (dataReady != null) {
				Async<NoException> dr = dataReady;
				dataReady = null;
				dr.unblock();
			}
		}
		return true;
	}
	
	private boolean handleNextReadResult(int nb, ByteBuffer buffer) {
		Async<NoException> sp = null;
		synchronized (PreBufferedReadable.this) {
			nextReadTask = null;
			if (buffersReady == null) return true; // closed
			if (nb <= 0) {
				endReached = true;
			} else {
				read += nb;
				if (nb < buffer.limit() || (size > 0 && read == size)) 
					endReached = true;
				buffer.flip();
				if (current == null) current = buffer;
				else buffersReady.addLast(buffer);
				if (!endReached && !buffersReady.isFull() && !stopReading)
					nextRead();
			}
			if (endReached && size > 0 && read < size && buffersReady != null && !isClosing() && !isClosed())
				error = new UnexpectedEnd(this);
			if (dataReady != null) {
				sp = dataReady;
				dataReady = null;
			}
		}
		if (sp != null)
			sp.unblock();
		return false;
	}
	
	@Override
	public int read() throws IOException {
		do {
			Async<NoException> sp;
			synchronized (this) {
				if (error != null) throw error;
				if (current != null) {
					if (!current.hasRemaining() && endReached)
						return -1;
					break;
				}
				if (endReached) return -1;
				if (isClosing() || isClosed()) return -1;
				if (dataReady == null) dataReady = new Async<>();
				sp = dataReady;
			}
			sp.block(0);
		} while (true);
		int res = current.get() & 0xFF;
		if (!current.hasRemaining())
			moveNextBuffer(true);
		return res;
	}
	
	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		do {
			Async<NoException> sp;
			synchronized (this) {
				if (error != null) throw error;
				if (current != null) {
					if (!current.hasRemaining() && endReached)
						return -1;
					break;
				}
				if (endReached) return -1;
				if (isClosing() || isClosed()) return -1;
				if (dataReady == null) dataReady = new Async<>();
				sp = dataReady;
			}
			sp.block(0);
		} while (true);
		if (current.remaining() > len) {
			current.get(buffer, offset, len);
			return len;
		}
		len = current.remaining();
		current.get(buffer, offset, len);
		moveNextBuffer(true);
		return len;
	}
	
	@Override
	public int readFully(byte[] buffer) throws IOException {
		do {
			Async<NoException> sp;
			synchronized (this) {
				if (error != null) throw error;
				if (current != null) {
					if (!current.hasRemaining() && endReached)
						return -1;
					break;
				}
				if (endReached) return -1;
				if (isClosing() || isClosed()) return -1;
				if (dataReady == null) dataReady = new Async<>();
				sp = dataReady;
			}
			sp.block(0);
		} while (true);
		if (current.remaining() > buffer.length) {
			current.get(buffer);
			return buffer.length;
		}
		int len = current.remaining();
		current.get(buffer, 0, len);
		moveNextBuffer(true);
		int pos = len;
		while (pos < buffer.length) {
			len = read(buffer, pos, buffer.length - pos);
			if (len < 0) break;
			pos += len;
		}
		return pos;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return readFullySyncIfPossible(buffer, 0, ondone);
	}
	
	private AsyncSupplier<Integer, IOException> readFullySyncIfPossible(
		ByteBuffer buffer, int alreadyDone, Consumer<Pair<Integer, IOException>> ondone
	) {
		boolean ok = true;
		synchronized (this) {
			if (error != null) return IOUtil.error(error, ondone);
			if (current != null) {
				if (!current.hasRemaining() && endReached)
					return IOUtil.success(Integer.valueOf(alreadyDone > 0 ? alreadyDone : -1), ondone);
			} else {
				if (endReached || isClosing() || isClosed())
					return IOUtil.success(Integer.valueOf(alreadyDone > 0 ? alreadyDone : -1), ondone);
				ok = false;
			}
		}
		if (!ok) {
			if (alreadyDone == 0)
				return readFullyAsync(buffer, ondone);
			AsyncSupplier<Integer, IOException> res = new AsyncSupplier<>();
			readFullyAsync(buffer, r -> {
				if (ondone != null) {
					if (r.getValue1() != null) ondone.accept(
						new Pair<>(Integer.valueOf(r.getValue1().intValue() + alreadyDone), null));
					else ondone.accept(r);
				}
			}).onDone(nb -> res.unblockSuccess(Integer.valueOf(alreadyDone + nb.intValue())), res);
			return res;
		}
		int len = buffer.remaining();
		if (current.remaining() > len) {
			int l = current.limit();
			current.limit(current.position() + len);
			buffer.put(current);
			current.limit(l);
			Integer r = Integer.valueOf(alreadyDone + len);
			if (ondone != null) ondone.accept(new Pair<>(r, null));
			return new AsyncSupplier<>(r, null);
		}
		len = current.remaining();
		buffer.put(current);
		moveNextBuffer(true);
		return readFullySyncIfPossible(buffer, len + alreadyDone, ondone);
	}

	@Override
	public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		Async<NoException> sp;
		synchronized (this) {
			if (error != null) return IOUtil.error(error, ondone);
			if (current != null) {
				if (!current.hasRemaining() && endReached)
					return IOUtil.success(null, ondone);
				return operation(Task.cpu("Read next buffer", getPriority(), t -> {
					ByteBuffer buf = current.asReadOnlyBuffer();
					current.position(current.limit());
					moveNextBuffer(true);
					return buf;
				}, ondone).start()).getOutput();
			}
			if (endReached) return IOUtil.success(null, ondone);
			if (isClosing() || isClosed()) return new AsyncSupplier<>(null, null, IO.cancelClosed());
			if (dataReady == null) dataReady = new Async<>();
			sp = dataReady;
		}
		AsyncSupplier<ByteBuffer, IOException> result = new AsyncSupplier<>();
		sp.onDone(() -> readNextBufferAsync(ondone).forward(result));
		return operation(result);
	}
	
	@Override
	public ByteBuffer readNextBuffer() throws IOException {
		do {
			Async<NoException> sp;
			synchronized (this) {
				if (error != null) throw error;
				if (current != null) {
					if (!current.hasRemaining() && endReached)
						return null;
					ByteBuffer buf = current.asReadOnlyBuffer();
					current.position(current.limit());
					moveNextBuffer(true);
					return buf;
				}
				if (endReached) return null;
				if (isClosing() || isClosed()) throw new ClosedChannelException();
				if (dataReady == null) dataReady = new Async<>();
				sp = dataReady;
			}
			sp.block(0);
		} while (true);
	}

}
