package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.collections.map.LongMap;
import net.lecousin.framework.collections.map.LongMapRBT;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * BufferedIO is often the best buffering implementation when we need both bufferization and seek operations.
 * <br/>
 * Basically, it keeps an ordered list of buffers corresponding to the different parts of the IO.
 * A buffer may loaded into memory, and so read and write operations can be done, or null if not loaded.
 * A buffer loaded into memory and which has not been used after some time will be freed automatically
 * by a background task.
 * <br/>
 * Write operations are delayed: the data is kept in memory, so can be modified again or read, then a background
 * task will perform the real write operation.
 * <br/>
 * BufferedIOs are managed by a singleton BufferingManager.
 * The BufferingManager is responsible for the background tasks: write operations and free buffer no longer used.
 * All BufferedIOs are managed together, so the BufferingManager can decide of operations based on memory usage.
 * It checks regularly what is used and not used and which write operations need to be done. In case a maximum memory
 * is reached, and new memory is needed, it will free memory first. But it will try to do operations in background
 * as much as possible, and will decide to free memory when it detects we are close to reach the maximum memory usage.
 * Using a single BufferingManager for all BufferedIOs allows to manage memory globally for the application, so
 * many BufferedIOs can be used together while controlling the memory usage, and giving priority to buffers recently used. 
 */
public abstract class BufferedIO extends BufferingManaged {

	protected BufferedIO(IO.Readable.Seekable io, int bufferSize, long size) throws IOException {
		this(io, bufferSize, bufferSize, size, true);
	}
	
	protected BufferedIO(
		IO.Readable.Seekable io,
		int firstBufferSize,
		int bufferSize,
		long size,
		boolean startReadSecondBufferWhenFirstBufferFullyRead)
	throws IOException {
		this.manager = BufferingManager.get();
		this.pos = io.getPosition();
		if (this.pos >= firstBufferSize)
			firstBufferSize = bufferSize; // if we are not starting from the first buffer, there is no need for a different size
		this.io = io;
		this.firstBufferSize = firstBufferSize;
		this.bufferSize = bufferSize;
		this.size = size;
		this.startReadSecondBufferWhenFirstBufferFullyRead = startReadSecondBufferWhenFirstBufferFullyRead;
		int nbBuffers = size <= firstBufferSize ? 1 : (int)((size - firstBufferSize) / bufferSize) + 2;
		if (nbBuffers < 10000)
			bufferTable = new BufferTableArray(nbBuffers);
		else
			bufferTable = new BufferTableMapping();
		loadBuffer(getBufferIndex(this.pos));
	}
	
	protected BufferingManager manager;
	protected IO.Readable.Seekable io;
	private BufferTable bufferTable;
	private int firstBufferSize;
	private int bufferSize;
	protected long size;
	protected long pos;
	protected boolean startReadSecondBufferWhenFirstBufferFullyRead = true;
	
	/** Buffer. */
	protected static class Buffer extends BufferingManager.Buffer {
		private Buffer(BufferedIO owner, long index) {
			this.owner = owner;
			this.index = index;
		}
		
		private long index;
		private SynchronizationPoint<NoException> loading;
		private IOException error;
	}
	
	private interface BufferTable {
		Buffer getOrAllocate(long index);
		
		Buffer getIfAllocated(long index);
		
		Buffer getForWrite(long index);
		
		void cancelAll();
		
		boolean removing(Buffer buffer);
		
		ISynchronizationPoint<NoException> decreaseSize(long newSize);
	}
	
	private class BufferTableArray implements BufferTable {
		public BufferTableArray(int nbBuffers) {
			if (nbBuffers <= 10)
				buffers = new ArrayList<>(nbBuffers);
			else {
				if (nbBuffers <= 100) // up to 10 arrays of 10 elements
					buffers = new LinkedArrayList<>(10);
				else if (nbBuffers <= 200) // up to 10 arrays of 20 elements
					buffers = new LinkedArrayList<>(20);
				else if (nbBuffers <= 450) // up to 15 arrays of 30 elements
					buffers = new LinkedArrayList<>(30);
				else if (nbBuffers <= 1000) // up to 20 arrays of 50 elements
					buffers = new LinkedArrayList<>(50);
				else
					buffers = new LinkedArrayList<>(100); // arrays of 100
			}

		}
		
		private List<Buffer> buffers;
		
		@Override
		public Buffer getOrAllocate(long index) {
			int l = buffers.size();
			while (l <= index)
				buffers.add(new Buffer(BufferedIO.this, l++));
			return buffers.get((int)index);
		}
		
		@Override
		public Buffer getIfAllocated(long index) {
			return index < buffers.size() ? buffers.get((int)index) : null;
		}
		
		@Override
		public Buffer getForWrite(long index) {
			boolean isNew = false;
			Buffer buffer;
			synchronized (this) {
				if (index == buffers.size()) {
					buffer = new Buffer(BufferedIO.this, index);
					buffer.buffer = new byte[index == 0 ? firstBufferSize : bufferSize];
					buffer.len = 0;
					buffer.inUse = 1;
					buffers.add(buffer);
					isNew = true;
				} else {
					buffer = buffers.get((int)index);
					if (buffer.buffer == null && (buffer.loading == null || buffer.loading.isUnblocked())) {
						buffer.buffer = new byte[index == 0 ? firstBufferSize : bufferSize];
						buffer.len = 0;
						isNew = true;
					}
					buffer.inUse++;
				}
			}
			if (isNew) manager.newBuffer(buffer);
			return buffer;
		}
		
		@Override
		public void cancelAll() {
			for (Buffer b : buffers) {
				manager.removeBuffer(b);
				synchronized (b) {
					if (b.loading != null) b.loading.cancel(new CancelException("BufferedIO.cancelAll"));
					b.loading = null;
					if (b.flushing != null)
						while (!b.flushing.isEmpty())
							b.flushing.removeFirst().cancel(new CancelException("BufferedIO.cancelAll"));
					b.flushing = null;
				}
			}
		}
		
		@Override
		public boolean removing(Buffer buffer) {
			if (buffer.loading == null) return true;
			if (!buffer.loading.isUnblocked()) return false;
			buffer.loading = null;
			return true;
		}
		
		@Override
		public ISynchronizationPoint<NoException> decreaseSize(long newSize) {
			JoinPoint<Exception> jp = new JoinPoint<>();
			long lastBufferIndex = newSize == 0 ? -1 : getBufferIndex(newSize - 1);
			synchronized (this) {
				for (int i = buffers.size() - 1; i >= lastBufferIndex && i >= 0; --i) {
					Buffer buffer = buffers.get(i);
					synchronized (buffer) {
						if (buffer.loading != null && !buffer.loading.isUnblocked())
							jp.addToJoin(buffer.loading);
						if (buffer.flushing != null)
							for (AsyncWork<?,?> f : buffer.flushing)
								jp.addToJoin(f);
						if (i != lastBufferIndex) {
							buffer.lastRead = System.currentTimeMillis();
							buffer.lastWrite = 0;
						}
					}
				}
			}
			jp.start();
			SynchronizationPoint<NoException> sp = new SynchronizationPoint<>();
			jp.listenAsync(new Task.Cpu<Void,NoException>("Decrease size of BufferedIO", getPriority()) {
				@Override
				public Void run() {
					Buffer lastBuffer = null;
					LinkedList<Buffer> removed = new LinkedList<>();
					synchronized (BufferTableArray.this) {
						while (buffers.size() > lastBufferIndex + 1) {
							Buffer buffer = buffers.remove(buffers.size() - 1);
							if (buffer.buffer != null)
								removed.add(buffer);
						}
						if (newSize > 0) lastBuffer = buffers.get((int)lastBufferIndex);
					}
					while (!removed.isEmpty())
						manager.removeBuffer(removed.removeFirst());
					// decrease size of last buffer if any
					if (lastBuffer != null) {
						int lastBufferSize;
						if (newSize <= firstBufferSize)
							lastBufferSize = (int)newSize;
						else
							lastBufferSize = (int)((newSize - firstBufferSize) % bufferSize);
						lastBuffer.len = lastBufferSize;
						lastBuffer.lastRead = System.currentTimeMillis();
					}
					sp.unblock();
					return null;
				}
			}, true);
			return sp;
		}
	}
	
	private class BufferTableMapping implements BufferTable {
		private LongMap<Buffer> map = new LongMapRBT<>(10000);

		@Override
		public Buffer getOrAllocate(long index) {
			Buffer b = map.get(index);
			if (b == null) {
				b = new Buffer(BufferedIO.this, index);
				map.put(index, b);
			}
			return b;
		}
		
		@Override
		public Buffer getIfAllocated(long index) {
			return map.get(index);
		}
		
		@Override
		public Buffer getForWrite(long index) {
			boolean isNew = false;
			Buffer buffer;
			synchronized (this) {
				buffer = map.get(index);
				if (buffer == null) {
					buffer = new Buffer(BufferedIO.this, index);
					buffer.buffer = new byte[index == 0 ? firstBufferSize : bufferSize];
					buffer.len = 0;
					buffer.inUse = 1;
					map.put(index, buffer);
					isNew = true;
				} else {
					if (buffer.buffer == null && (buffer.loading == null || buffer.loading.isUnblocked())) {
						buffer.buffer = new byte[index == 0 ? firstBufferSize : bufferSize];
						buffer.len = 0;
						isNew = true;
					}
					buffer.inUse++;
				}
			}
			if (isNew) manager.newBuffer(buffer);
			return buffer;
		}
		
		@Override
		public void cancelAll() {
			for (Iterator<Buffer> it = map.values(); it.hasNext(); ) {
				Buffer b = it.next();
				manager.removeBuffer(b);
				synchronized (b) {
					if (b.loading != null) b.loading.cancel(new CancelException("BufferedIO.cancelAll"));
					b.loading = null;
					if (b.flushing != null)
						while (!b.flushing.isEmpty())
							b.flushing.removeFirst().cancel(new CancelException("BufferedIO.cancelAll"));
					b.flushing = null;
				}
			}
			map.clear();
		}
		
		@Override
		public boolean removing(Buffer buffer) {
			if (buffer.loading != null) {
				if (!buffer.loading.isUnblocked()) return false;
				buffer.loading = null;
			}
			map.remove(buffer.index);
			return true;
		}
		
		@Override
		public ISynchronizationPoint<NoException> decreaseSize(long newSize) {
			JoinPoint<Exception> jp = new JoinPoint<>();
			long lastBufferIndex = newSize == 0 ? -1 : getBufferIndex(newSize - 1);
			synchronized (this) {
				for (Iterator<Buffer> it = map.values(); it.hasNext(); ) {
					Buffer buffer = it.next();
					if (buffer.index < lastBufferIndex) continue;
					synchronized (buffer) {
						if (buffer.loading != null && !buffer.loading.isUnblocked())
							jp.addToJoin(buffer.loading);
						if (buffer.flushing != null)
							for (AsyncWork<?,?> f : buffer.flushing)
								jp.addToJoin(f);
						if (buffer.index != lastBufferIndex) {
							buffer.lastRead = System.currentTimeMillis();
							buffer.lastWrite = 0;
						}
					}
				}
			}
			jp.start();
			SynchronizationPoint<NoException> sp = new SynchronizationPoint<>();
			jp.listenAsync(new Task.Cpu<Void,NoException>("Decrease size of BufferedIO", getPriority()) {
				@Override
				public Void run() {
					Buffer lastBuffer = null;
					LinkedList<Buffer> removed = new LinkedList<>();
					synchronized (BufferTableMapping.this) {
						for (Iterator<Buffer> it = map.values(); it.hasNext(); ) {
							Buffer buffer = it.next();
							if (buffer.index <= lastBufferIndex) continue;
							it.remove();
							if (buffer.buffer != null)
								removed.add(buffer);
						}
						if (newSize > 0) lastBuffer = map.get(lastBufferIndex);
					}
					while (!removed.isEmpty())
						manager.removeBuffer(removed.removeFirst());
					// decrease size of last buffer if any
					if (lastBuffer != null) {
						int lastBufferSize;
						if (newSize <= firstBufferSize)
							lastBufferSize = (int)newSize;
						else
							lastBufferSize = (int)((newSize - firstBufferSize) % bufferSize);
						lastBuffer.len = lastBufferSize;
						lastBuffer.lastRead = System.currentTimeMillis();
					}
					sp.unblock();
					return null;
				}
			}, true);
			return sp;
		}
	}
	
	protected void loadBuffer(long index) {
		Buffer buffer;
		synchronized (bufferTable) {
			buffer = bufferTable.getOrAllocate(index);
			if (buffer.buffer != null) return;
			if (buffer.loading != null) return;
			buffer.loading = new SynchronizationPoint<>();
		}
		AsyncWork<Integer,IOException> loading;
		synchronized (buffer) {
			buffer.buffer = new byte[index == 0 ? firstBufferSize : bufferSize];
			loading = io.readFullyAsync(
				index == 0
				? 0L
				: (firstBufferSize + (index - 1) * bufferSize),
				ByteBuffer.wrap(buffer.buffer));
		}
		operation(loading).listenInline(new Runnable() {
			@Override
			public void run() {
				SynchronizationPoint<NoException> sp;
				synchronized (buffer) {
					sp = buffer.loading;
					if (sp == null) return;
				}
				if (!loading.isSuccessful()) {
					if (loading.isCancelled()) sp.cancel(loading.getCancelEvent());
					else {
						buffer.error = loading.getError();
						sp.unblock();
					}
					buffer.buffer = null;
					return;
				}
				buffer.len = loading.getResult().intValue();
				if (buffer.len < 0) buffer.len = 0;
				buffer.lastRead = System.currentTimeMillis();
				manager.newBuffer(buffer);
				sp.unblock();
			}
		});
	}
	
	/** Load the buffer if needed. */
	protected Buffer useBufferAsync(long index) {
		do {
			Buffer b = null;
			try {
				synchronized (bufferTable) {
					b = bufferTable.getIfAllocated(index);
				}
				if (b != null) {
					synchronized (b) {
						b.inUse++;
						if (b.buffer == null && (b.loading == null || b.loading.isUnblocked()) && b.error == null)
							loadBuffer(index);
					}
					return b;
				}
				loadBuffer(index);
			} catch (Throwable t) {
				if (closing) {
					b = new Buffer(this, index);
					b.loading = new SynchronizationPoint<>(new CancelException("IO closed"));
					return b;
				}
				if (b == null) b = new Buffer(this, index);
				b.error = IO.error(t);
				b.loading = new SynchronizationPoint<>(true);
				return b;
			}
		} while (true);
	}
	
	/** Load the buffer if needed and wait for it. */
	protected Buffer useBufferSync(long index) throws IOException {
		Buffer b = useBufferAsync(index);
		SynchronizationPoint<NoException> sp = b.loading;
		if (sp != null) sp.block(0);
		if (b.error != null) throw b.error;
		return b;
	}
	
	protected long getBufferIndex(long pos) {
		if (pos < firstBufferSize) return 0;
		return (((pos - firstBufferSize) / bufferSize)) + 1;
	}
	
	protected int getBufferOffset(long pos) {
		if (pos < firstBufferSize) return (int)pos;
		pos -= firstBufferSize;
		return (int)(pos % bufferSize);
	}
	
	protected long getBufferStart(long index) {
		if (index == 0) return 0;
		if (index == 1) return firstBufferSize;
		return firstBufferSize + ((index - 1) * bufferSize);
	}
	
	/** Ask to cancel all buffers. This can be used before to close if the data is not needed anymore
	 * so there is no need to write pending buffers when closing.
	 */
	public void cancelAll() {
		synchronized (bufferTable) {
			bufferTable.cancelAll();
		}
	}
	
	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		return manager.close(this);
	}
	
	@Override
	ISynchronizationPoint<Exception> closed() {
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		io = null;
		bufferTable = null;
		ondone.unblock();
	}
	
	@Override
	boolean removing(BufferingManager.Buffer buffer) {
		Buffer b = (Buffer)buffer;
		synchronized (bufferTable) {
			return bufferTable.removing(b);
		}
	}
	
	@Override
	AsyncWork<?,IOException> flushWrite(BufferingManager.Buffer buffer) {
		long pos;
		if (((Buffer)buffer).index == 0) pos = 0;
		else if (((Buffer)buffer).index == 1) pos = firstBufferSize;
		else pos = firstBufferSize + ((((Buffer)buffer).index - 1) * bufferSize);
		return ((IO.Writable.Seekable)io).writeAsync(pos, ByteBuffer.wrap(buffer.buffer, 0, buffer.len));
	}
	
	protected ISynchronizationPoint<IOException> flush() {
		return manager.fullFlush(this);
	}
	
	/** Read-only implementation of BufferedIO. */
	public static class ReadOnly extends BufferedIO implements IO.Readable.Seekable, IO.Readable.Buffered, IO.KnownSize {
		
		/** Constructor. */
		public ReadOnly(IO.Readable.Seekable io, int bufferSize, long size) throws IOException {
			super(io, bufferSize, size);
		}
		
		/** Constructor. */
		public ReadOnly(
			IO.Readable.Seekable io, 
			int firstBufferSize, 
			int bufferSize, 
			long size, 
			boolean startReadSecondBufferWhenFirstBufferFullyRead)
		throws IOException {
			super(io, firstBufferSize, bufferSize, size, startReadSecondBufferWhenFirstBufferFullyRead);
		}
		
		@Override
		public ISynchronizationPoint<IOException> canStartReading() {
			return super.canStartReading();
		}
		
		@Override
		public IO getWrappedIO() {
			return io;
		}
		
		@Override
		public String getSourceDescription() {
			if (io == null) return "";
			return io.getSourceDescription();
		}
		
		@Override
		public byte getPriority() {
			if (io == null) return Task.PRIORITY_NORMAL;
			return io.getPriority();
		}
		
		@Override
		public void setPriority(byte priority) {
			io.setPriority(priority);
		}
		
		@Override
		public TaskManager getTaskManager() {
			return Threading.getCPUTaskManager();
		}
		
		@Override
		public long getPosition() {
			return pos;
		}
		
		@Override
		public long getSizeSync() {
			return size;
		}
		
		@Override
		public AsyncWork<Long, IOException> getSizeAsync() {
			AsyncWork<Long, IOException> sp = new AsyncWork<>();
			sp.unblockSuccess(Long.valueOf(size));
			return sp;
		}
		
		@Override
		public int skip(int skip) {
			return (int)skipSync(skip);
		}
		
		@Override
		public long skipSync(long skip) {
			if (pos + skip > size) skip = size - pos;
			if (pos + skip < 0) skip = -pos;
			pos += skip;
			if (pos < size)
				loadBuffer(getBufferIndex(pos));
			return skip;
		}
		
		@Override
		public long seekSync(SeekType type, long move) {
			switch (type) {
			case FROM_BEGINNING:
				pos = move;
				break;
			case FROM_END:
				pos = size - move;
				break;
			case FROM_CURRENT:
				pos += move;
				break;
			default: break;
			}
			if (pos < 0) pos = 0;
			if (pos > size) pos = size;
			return pos;
		}
		
		@Override
		public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
			seekSync(type, move);
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(pos), null));
			return new AsyncWork<Long,IOException>(Long.valueOf(pos), null);
		}
		
		@Override
		public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long, IOException>> ondone) {
			long skipped = skipSync(n);
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(skipped), null));
			return new AsyncWork<Long,IOException>(Long.valueOf(skipped), null);
		}
		
		@Override
		public int read() throws IOException {
			return super.read();
		}
		
		@Override
		public int read(byte[] buf, int offset, int len) throws IOException {
			return super.read(buf, offset, len);
		}
		
		@Override
		public int readFully(byte[] buf) throws IOException {
			return super.readFully(buf);
		}
		
		@Override
		public int readSync(long pos, ByteBuffer buf) throws IOException {
			return super.readSync(pos, buf);
		}
		
		@Override
		public int readSync(ByteBuffer buf) throws IOException {
			return super.readSync(pos, buf);
		}
		
		@Override
		public int readAsync() throws IOException {
			return super.readAsync();
		}
		
		@Override
		public AsyncWork<Integer,IOException> readAsync(long pos, ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readAsync(pos, buf, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException> readAsync(ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readAsync(pos, buf, ondone);
		}
		
		@Override
		public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
			return super.readNextBufferAsync(ondone);
		}
		
		@Override
		public int readFullySync(long pos, ByteBuffer buf) throws IOException {
			return super.readFullySync(pos, buf);
		}
		
		@Override
		public int readFullySync(ByteBuffer buf) throws IOException {
			return super.readFullySync(pos, buf);
		}
		
		@Override
		public AsyncWork<Integer,IOException> readFullyAsync(
			long pos, ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone)
		{
			return super.readFullyAsync(pos, buf, 0, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readFullyAsync(pos, buf, 0, ondone);
		}
		
		@Override
		public AsyncWork<Integer, IOException> readFullySyncIfPossible(
			ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone
		) {
			return super.readFullySyncIfPossible(pos, buffer, 0, ondone);
		}
		
		/** While readAsync methods are supposed to do the job in a separate thread, this method
		 * fills the given buffer synchronously if enough data is already buffered, else it finishes asynchronously.
		 * The caller can check the returned AsyncWork by calling its method isUnblocked to know if the
		 * read has been performed synchronously.
		 * This method may be useful for processes that hope to work synchronously because this IO is buffered,
		 * but support also to work asynchronously without blocking a thread.
		 */
		public AsyncWork<Integer, IOException> readFullySyncIfPossible(
			long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone
		) {
			return super.readFullySyncIfPossible(pos, buffer, 0, ondone);
		}
	}

	/** Read and write implementation of BufferedIO. */
	public static class ReadWrite
		extends BufferedIO
		implements IO.Readable.Seekable, IO.Readable.Buffered, IO.Writable.Seekable, IO.Writable.Buffered,
			IO.KnownSize, IO.Resizable {
		
		/** Constructor. */
		public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.Resizable>
		ReadWrite(T io, int bufferSize, long size)throws IOException {
			super(io, bufferSize, size);
		}
		
		/** Constructor. */
		public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.Resizable>
		ReadWrite(
			T io, int firstBufferSize, int bufferSize, long size, boolean startReadSecondBufferWhenFirstBufferFullyRead
		) throws IOException {
			super(io, firstBufferSize, bufferSize, size, startReadSecondBufferWhenFirstBufferFullyRead);
		}
		
		@Override
		public ISynchronizationPoint<IOException> canStartReading() {
			return super.canStartReading();
		}
		
		@Override
		public ISynchronizationPoint<IOException> canStartWriting() {
			return canStartReading();
		}
		
		@Override
		public IO getWrappedIO() {
			return io;
		}
		
		@Override
		public String getSourceDescription() {
			if (io == null) return "";
			return io.getSourceDescription();
		}
		
		@Override
		public byte getPriority() {
			if (io == null) return Task.PRIORITY_NORMAL;
			return io.getPriority();
		}
		
		@Override
		public void setPriority(byte priority) {
			io.setPriority(priority);
		}
		
		@Override
		public TaskManager getTaskManager() {
			return Threading.getCPUTaskManager();
		}
		
		@Override
		public long getPosition() {
			return pos;
		}
		
		@Override
		public long getSizeSync() {
			return size;
		}
		
		@Override
		public AsyncWork<Long, IOException> getSizeAsync() {
			AsyncWork<Long, IOException> sp = new AsyncWork<>();
			sp.unblockSuccess(Long.valueOf(size));
			return sp;
		}
		
		@Override
		public int skip(int skip) {
			return (int)skipSync(skip);
		}
		
		@Override
		public long skipSync(long skip) {
			if (pos + skip > size) skip = size - pos;
			if (pos + skip < 0) skip = -pos;
			pos += skip;
			if (pos < size)
				loadBuffer(getBufferIndex(pos));
			return skip;
		}
		
		@Override
		public long seekSync(SeekType type, long move) {
			switch (type) {
			case FROM_BEGINNING:
				pos = move;
				break;
			case FROM_END:
				pos = size - move;
				break;
			case FROM_CURRENT:
				pos += move;
				break;
			default: break;
			}
			if (pos < 0) pos = 0;
			if (pos > size) pos = size;
			return pos;
		}
		
		@Override
		public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
			seekSync(type, move);
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(pos), null));
			return new AsyncWork<Long,IOException>(Long.valueOf(pos), null);
		}
		
		@Override
		public AsyncWork<Long, IOException> seekAsync(SeekType type, long move) {
			return net.lecousin.framework.io.IO.Writable.Seekable.super.seekAsync(type, move);
		}
		
		@Override
		public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long, IOException>> ondone) {
			long skipped = skipSync(n);
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(skipped), null));
			return new AsyncWork<Long,IOException>(Long.valueOf(skipped), null);
		}
		
		@Override
		public int read() throws IOException {
			return super.read();
		}
		
		@Override
		public int read(byte[] buf, int offset, int len) throws IOException {
			return super.read(buf, offset, len);
		}
		
		@Override
		public int readFully(byte[] buf) throws IOException {
			return super.readFully(buf);
		}
		
		@Override
		public int readSync(long pos, ByteBuffer buf) throws IOException {
			return super.readSync(pos, buf);
		}
		
		@Override
		public int readSync(ByteBuffer buf) throws IOException {
			return super.readSync(pos, buf);
		}
		
		@Override
		public int readAsync() throws IOException {
			return super.readAsync();
		}

		@Override
		public AsyncWork<Integer,IOException> readAsync(long pos, ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readAsync(pos, buf, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException> readAsync(ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readAsync(pos, buf, ondone);
		}
		
		@Override
		public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
			return super.readNextBufferAsync(ondone);
		}
		
		@Override
		public int readFullySync(long pos, ByteBuffer buf) throws IOException {
			return super.readFullySync(pos, buf);
		}
		
		@Override
		public int readFullySync(ByteBuffer buf) throws IOException {
			return super.readFullySync(pos, buf);
		}
		
		@Override
		public AsyncWork<Integer,IOException> readFullyAsync(
			long pos, ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone)
		{
			return super.readFullyAsync(pos, buf, 0, ondone);
		}
		
		@Override
		public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readFullyAsync(pos, buf, 0, ondone);
		}
		
		@Override
		public AsyncWork<Integer, IOException> readFullySyncIfPossible(
			ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone
		) {
			return super.readFullySyncIfPossible(pos, buffer, 0, ondone);
		}
		
		/** While readAsync methods are supposed to do the job in a separate thread, this method
		 * fills the given buffer synchronously if enough data is already buffered, else it finishes asynchronously.
		 * The caller can check the returned AsyncWork by calling its method isUnblocked to know if the
		 * read has been performed synchronously.
		 * This method may be useful for processes that hope to work synchronously because this IO is buffered,
		 * but support also to work asynchronously without blocking a thread.
		 */
		public AsyncWork<Integer, IOException> readFullySyncIfPossible(
			long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone
		) {
			return super.readFullySyncIfPossible(pos, buffer, 0, ondone);
		}
		
		@Override
		public AsyncWork<Integer, IOException> writeAsync(
			long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone)
		{
			return super.writeAsync(pos, buffer, 0, ondone);
		}
		
		@Override
		public AsyncWork<Integer, IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.writeAsync(pos, buffer, 0, ondone);
		}

		@Override
		public int writeSync(long pos, ByteBuffer buffer) throws IOException {
			return super.writeSync(pos, buffer, 0);
		}
		
		@Override
		public int writeSync(ByteBuffer buffer) throws IOException {
			return super.writeSync(pos, buffer, 0);
		}
		
		@Override
		public void setSizeSync(long newSize) throws IOException {
			if (newSize == size) return;
			try {
				if (newSize > size)
					operation(increaseSize(newSize)).blockThrow(0);
				else
					operation(decreaseSize(newSize)).blockThrow(0);
			} catch (CancelException e) {
				throw new IOException("Cancelled", e);
			}
		}
		
		@Override
		public ISynchronizationPoint<IOException> setSizeAsync(long newSize) {
			if (newSize == size) return new AsyncWork<>(null, null);
			if (newSize > size) return operation(increaseSize(newSize));
			return operation(decreaseSize(newSize));
		}
		
		@Override
		public void write(byte b) throws IOException {
			super.write(b);
		}
		
		@Override
		public void write(byte[] buf, int offset, int length) throws IOException {
			super.write(buf, offset, length);
		}
		
		@Override
		public ISynchronizationPoint<IOException> flush() {
			return super.flush();
		}
	}	

	protected ISynchronizationPoint<IOException> canStartReading() {
		long i = getBufferIndex(pos);
		Buffer buffer;
		synchronized (bufferTable) {
			buffer = bufferTable.getIfAllocated(i);
			if (buffer == null) buffer = bufferTable.getIfAllocated(0);
		}
		SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
		if (buffer == null) {
			sp.unblock();
			return sp;
		}
		SynchronizationPoint<NoException> l = buffer.loading;
		if (l != null && !l.isUnblocked()) {
			Buffer b = buffer;
			l.listenInline(new Runnable() {
				@Override
				public void run() {
					if (b.error != null)
						sp.error(b.error);
					else
						sp.unblock();
				}
			});
			return sp;
		}
		if (buffer.error != null)
			sp.error(buffer.error);
		else
			sp.unblock();
		return sp;
	}
	
	protected int read() throws IOException {
		if (pos == size) return -1;
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferSync(bufferIndex);
		buffer.lastRead = System.currentTimeMillis();
		byte b = buffer.buffer[getBufferOffset(pos)];
		pos++;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			long nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		}
		buffer.inUse--;
		return b & 0xFF;
	}

	protected int read(byte[] buf, int offset, int len) throws IOException {
		if (pos == size) return -1;
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferSync(bufferIndex);
		int start = getBufferOffset(pos);
		if (len > buffer.len - start) len = buffer.len - start;
		System.arraycopy(buffer.buffer, start, buf, offset, len);
		buffer.lastRead = System.currentTimeMillis();
		buffer.inUse--;
		pos += len;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			long nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		}
		return len;
	}
	
	protected int readSync(long pos, ByteBuffer buf) throws IOException {
		if (pos >= size)
			return -1;
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferSync(bufferIndex);
		int start = getBufferOffset(pos);
		int len = buf.remaining();
		int max = buffer.len - start;
		if (len > max) len = max;
		if (len <= 0)
			return 0;
		buf.put(buffer.buffer, start, len);
		buffer.lastRead = System.currentTimeMillis();
		buffer.inUse--;
		pos += len;
		this.pos = pos;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			long nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		}
		return len;
	}
	
	protected int readAsync() throws IOException {
		if (pos == size) return -1;
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferAsync(bufferIndex);
		SynchronizationPoint<NoException> sp = buffer.loading;
		if (sp != null && !sp.isUnblocked()) {
			buffer.lastRead = System.currentTimeMillis();
			buffer.inUse--;
			return -2;
		}
		if (buffer.error != null) throw buffer.error;
		byte b = buffer.buffer[getBufferOffset(pos)];
		buffer.inUse--;
		pos++;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			long nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		}
		return b & 0xFF;
	}
	
	protected AsyncWork<Integer,IOException> readAsync(long pos, ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		if (pos >= size) {
			if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(0), null));
			return new AsyncWork<Integer,IOException>(Integer.valueOf(0), null);
		}
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferAsync(bufferIndex);
		SynchronizationPoint<NoException> sp = buffer.loading;
		if (sp == null) {
			// already loaded
			if (buffer.error != null) {
				if (ondone != null) ondone.run(new Pair<>(null, buffer.error));
				return new AsyncWork<Integer,IOException>(null, buffer.error);
			}
		}
		Task<Integer,IOException> task = new Task.Cpu<Integer,IOException>("Read in BufferedIO", getPriority(), ondone) {
			@Override
			public Integer run() throws IOException, CancelException {
				if (buffer.error != null)
					throw buffer.error;
				if (sp != null && sp.isCancelled())
					throw sp.getCancelEvent();
				int start = getBufferOffset(pos);
				if (start >= buffer.len) {
					buffer.inUse--;
					if (pos < size)
						throw new IOException("Unexpected buffer size: IO size is " + size
							+ ", length of buffer " + bufferIndex + " is " + buffer.len
							+ ", expected is " + (size - getBufferStart(bufferIndex)));
					return Integer.valueOf(0);
				}
				int len = buf.remaining();
				if (len > buffer.len - start) len = buffer.len - start;
				buf.put(buffer.buffer, start, len);
				buffer.lastRead = System.currentTimeMillis();
				buffer.inUse--;
				long p = pos + len;
				BufferedIO.this.pos = p;
				if (p < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
					long nextIndex = getBufferIndex(p);
					if (nextIndex != bufferIndex) loadBuffer(nextIndex);
				}
				return Integer.valueOf(len);
			}
		};
		if (sp == null || sp.isUnblocked()) task.start();
		else task.startOn(sp, true);
		return operation(task.getOutput());
	}
	
	protected AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		if (pos >= size) {
			if (ondone != null) ondone.run(new Pair<>(null, null));
			return new AsyncWork<>(null, null);
		}
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferAsync(bufferIndex);
		SynchronizationPoint<NoException> sp = buffer.loading;
		if (sp == null || sp.isUnblocked()) {
			// already loaded
			if (buffer.error != null) {
				if (ondone != null) ondone.run(new Pair<>(null, buffer.error));
				return new AsyncWork<>(null, buffer.error);
			}
			if (sp != null && sp.isCancelled())
				return new AsyncWork<>(null, null, sp.getCancelEvent());
			int start = getBufferOffset(pos);
			ByteBuffer buf = ByteBuffer.allocate(buffer.len - start);
			buf.put(buffer.buffer, start, buffer.len - start);
			buffer.lastRead = System.currentTimeMillis();
			buffer.inUse--;
			long p = pos + buffer.len - start;
			this.pos = p;
			if (p < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
				long nextIndex = getBufferIndex(p);
				if (nextIndex != bufferIndex) loadBuffer(nextIndex);
			}
			buf.flip();
			if (ondone != null) ondone.run(new Pair<>(buf, null));
			return new AsyncWork<>(buf, null);
		}
		Task<ByteBuffer,IOException> task = new Task.Cpu<ByteBuffer,IOException>("Read in BufferedIO", getPriority(), ondone) {
			@Override
			public ByteBuffer run() throws IOException, CancelException {
				if (buffer.error != null)
					throw buffer.error;
				if (sp != null && sp.isCancelled())
					throw sp.getCancelEvent();
				int start = getBufferOffset(pos);
				ByteBuffer buf = ByteBuffer.allocate(buffer.len - start);
				buf.put(buffer.buffer, start, buffer.len - start);
				buffer.lastRead = System.currentTimeMillis();
				buffer.inUse--;
				long p = pos + buffer.len - start;
				BufferedIO.this.pos = p;
				if (p < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
					long nextIndex = getBufferIndex(p);
					if (nextIndex != bufferIndex) loadBuffer(nextIndex);
				}
				buf.flip();
				return buf;
			}
		};
		task.startOn(sp, true);
		return operation(task.getOutput());
	}

	protected int readFully(byte[] buf) throws IOException {
		if (pos == size) return -1;
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferSync(bufferIndex);
		int start = getBufferOffset(pos);
		int len = buf.length;
		if (len > buffer.len - start) len = buffer.len - start;
		System.arraycopy(buffer.buffer, start, buf, 0, len);
		buffer.lastRead = System.currentTimeMillis();
		buffer.inUse--;
		pos += len;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			long nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		}
		if (len == buf.length) return len;
		int bufPos = len;
		int remaining = buf.length - bufPos;
		while (pos < size && remaining > 0) {
			int nb = read(buf, bufPos, remaining);
			if (nb <= 0) break;
			bufPos += nb;
			remaining -= nb;
		}
		return bufPos;
	}
	
	protected int readFullySync(long pos, ByteBuffer buf) throws IOException {
		if (pos >= size) return -1;
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferSync(bufferIndex);
		int start = getBufferOffset(pos);
		int len = buf.remaining();
		if (len > buffer.len - start) len = buffer.len - start;
		buf.put(buffer.buffer, start, len);
		buffer.lastRead = System.currentTimeMillis();
		buffer.inUse--;
		pos += len;
		this.pos = pos;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			long nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		}
		if (buf.remaining() == 0) return len;
		int bufPos = len;
		while (pos < size && buf.remaining() > 0) {
			int nb = readSync(pos, buf);
			if (nb <= 0) break;
			bufPos += nb;
			pos += nb;
		}
		return bufPos;
	}
	
	protected AsyncWork<Integer,IOException> readFullyAsync(
		long pos, ByteBuffer buf, int alreadyDone, RunnableWithParameter<Pair<Integer,IOException>> ondone)
	{
		if (pos >= size) {
			if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(alreadyDone > 0 ? alreadyDone : -1),null));
			return new AsyncWork<Integer,IOException>(Integer.valueOf(alreadyDone > 0 ? alreadyDone : -1),null);
		}
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferAsync(bufferIndex);
		SynchronizationPoint<NoException> sp = buffer.loading;
		if (sp == null) {
			// already loaded
			if (buffer.error != null) {
				if (ondone != null) ondone.run(new Pair<>(null, buffer.error));
				return new AsyncWork<Integer,IOException>(null, buffer.error);
			}
		}
		AsyncWork<Integer, IOException> done = new AsyncWork<Integer, IOException>();
		Task<Void,IOException> task = new Task.Cpu<Void,IOException>("Read in BufferedIO at " + pos, getPriority()) {
			@Override
			public Void run() throws IOException, CancelException {
				if (buffer.error != null) {
					if (ondone != null) ondone.run(new Pair<>(null, buffer.error));
					done.unblockError(buffer.error);
					throw buffer.error;
				}
				if (sp != null && sp.isCancelled())
					throw sp.getCancelEvent();
				int start = getBufferOffset(pos);
				if (start >= buffer.len) {
					buffer.inUse--;
					if (pos < size) {
						IOException err = new IOException("Unexpected buffer size: IO size is " + size
							+ ", and length of buffer " + bufferIndex + " is " + buffer.len);
						if (ondone != null) ondone.run(new Pair<>(null, err));
						done.error(err);
					} else {
						if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(alreadyDone > 0 ? alreadyDone : -1),null));
						done.unblockSuccess(Integer.valueOf(alreadyDone > 0 ? alreadyDone : -1));
					}
					return null;
				}
				int len = buf.remaining();
				if (len > buffer.len - start) len = buffer.len - start;
				buf.put(buffer.buffer, start, len);
				buffer.lastRead = System.currentTimeMillis();
				buffer.inUse--;
				long p = pos + len;
				BufferedIO.this.pos = p;
				if (p < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
					long nextIndex = getBufferIndex(p);
					if (nextIndex != bufferIndex) loadBuffer(nextIndex);
				}
				if (buf.remaining() == 0) {
					if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(alreadyDone + len), null));
					done.unblockSuccess(Integer.valueOf(alreadyDone + len));
					return null;
				}
				int previous = alreadyDone + len;
				AsyncWork<Integer,IOException> next = readFullyAsync(p, buf, 0, null);
				next.listenInline(new Runnable() {
					@Override
					public void run() {
						if (next.isSuccessful()) {
							int result = previous;
							if (next.getResult().intValue() > 0)
								result += next.getResult().intValue();
							if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(result), null));
							done.unblockSuccess(Integer.valueOf(result));
							return;
						}
						if (next.isCancelled()) {
							done.unblockCancel(next.getCancelEvent());
							return;
						}
						if (ondone != null) ondone.run(new Pair<>(null, next.getError()));
						done.unblockError(next.getError());
					}
				});
				return null;
			}
		};
		if (sp == null || sp.isUnblocked()) task.start();
		else task.startOn(sp, true);
		done.forwardCancel(task.getOutput());
		task.getOutput().forwardCancel(done);
		return operation(done);
	}
	
	protected AsyncWork<Integer, IOException> readFullySyncIfPossible(
		long pos, ByteBuffer buf, int alreadyDone, RunnableWithParameter<Pair<Integer, IOException>> ondone
	) {
		if (pos >= size) {
			Integer r = Integer.valueOf(alreadyDone > 0 ? alreadyDone : -1);
			if (ondone != null) ondone.run(new Pair<>(r, null));
			return new AsyncWork<Integer,IOException>(r, null);
		}
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferAsync(bufferIndex);
		SynchronizationPoint<NoException> sp = buffer.loading;
		if (sp != null && !sp.isUnblocked())
			return readFullyAsync(this.pos, buf, alreadyDone, ondone);
		if (buffer.error != null) {
			if (ondone != null) ondone.run(new Pair<>(null, buffer.error));
			return new AsyncWork<Integer,IOException>(null, buffer.error);
		}
		if (sp != null && sp.isCancelled())
			return new AsyncWork<>(null, null, sp.getCancelEvent());
		int start = getBufferOffset(pos);
		int len = buf.remaining();
		if (len > buffer.len - start) len = buffer.len - start;
		buf.put(buffer.buffer, start, len);
		buffer.lastRead = System.currentTimeMillis();
		buffer.inUse--;
		pos += len;
		this.pos = pos;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			long nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		}
		if (buf.remaining() == 0) {
			Integer r = Integer.valueOf(alreadyDone + len);
			if (ondone != null) ondone.run(new Pair<>(r, null));
			return new AsyncWork<>(r, null);
		}
		return readFullySyncIfPossible(pos, buf, alreadyDone + len, ondone);
	}
	
	protected ISynchronizationPoint<IOException> increaseSize(long newSize) {
		ISynchronizationPoint<IOException> resize = ((IO.Resizable)io).setSizeAsync(newSize);
		SynchronizationPoint<NoException> lastBuffer = null;
		
		long prevIndex = size > 0 ? getBufferIndex(size - 1) : 0;
		long firstIndex = getBufferIndex(size);
		long lastIndex = getBufferIndex(newSize - 1);
		
		if (prevIndex == firstIndex && size > 0) {
			// we will need to load that buffer to modify it
			Buffer buffer = useBufferAsync(prevIndex);
			lastBuffer = buffer.loading;
			if (lastBuffer == null || lastBuffer.isUnblocked()) {
				if (lastIndex > prevIndex)
					buffer.len = buffer.buffer.length;
				else
					buffer.len = (int)(newSize - getBufferStart(prevIndex));
				buffer.lastRead = System.currentTimeMillis();
				buffer.inUse--;
			} else {
				lastBuffer.listenInline(new Runnable() {
					@Override
					public void run() {
						if (lastIndex > prevIndex)
							buffer.len = buffer.buffer.length;
						else
							buffer.len = (int)(newSize - getBufferStart(prevIndex));
						buffer.lastRead = System.currentTimeMillis();
						buffer.inUse--;
					}
				});
			}
			firstIndex++;
		}
		JoinPoint<Exception> jp = new JoinPoint<>();
		LinkedList<Buffer> newBuffers = new LinkedList<>();
		synchronized (bufferTable) {
			while (firstIndex <= lastIndex) {
				Buffer b = bufferTable.getOrAllocate(firstIndex);
				if (b.buffer == null && (b.loading == null || b.loading.isUnblocked())) {
					b.buffer = new byte[firstIndex > 0 ? bufferSize : firstBufferSize];
					newBuffers.add(b);
				}
				if (b.loading != null && !b.loading.isUnblocked()) {
					long i = firstIndex;
					Buffer buf = b;
					b.loading.listenInline(() -> {
						buf.len = i < lastIndex ? buf.buffer.length : (int)(newSize - getBufferStart(i));
						buf.lastRead = System.currentTimeMillis();
					});
					jp.addToJoin(b.loading);
				} else {
					b.len = firstIndex < lastIndex ? b.buffer.length : (int)(newSize - getBufferStart(firstIndex));
					b.lastRead = System.currentTimeMillis();
				}
				firstIndex++;
			}
		}
		while (!newBuffers.isEmpty())
			manager.newBuffer(newBuffers.removeFirst());
		SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
		if (lastBuffer != null)
			jp.addToJoin(lastBuffer);
		jp.start();
		resize.listenInline(() -> {
			if (jp.isUnblocked()) {
				size = newSize;
				sp.unblock();
			} else jp.listenInline(new Runnable() {
				@Override
				public void run() {
					size = newSize;
					sp.unblock();
				}
			});
		}, sp);
		return sp;
	}
	
	protected ISynchronizationPoint<IOException> decreaseSize(long newSize) {
		SynchronizationPoint<IOException> result = new SynchronizationPoint<>();
		if (newSize < 0) newSize = 0;
		// we need to remove the buffers, and ensure there is no waiting operation on them
		ISynchronizationPoint<NoException> sp = bufferTable.decreaseSize(newSize);
		long ns = newSize;
		sp.listenInline(() -> {
			ISynchronizationPoint<IOException> resize = ((IO.Resizable)io).setSizeAsync(ns);
			resize.listenInline(() -> {
				size = ns;
				if (pos > size) pos = size;
				result.unblock();
			}, result);
		});
		return result;
	}

	protected AsyncWork<Integer, IOException> writeAsync(
		long pos, ByteBuffer buf, int alreadyDone, RunnableWithParameter<Pair<Integer,IOException>> ondone)
	{
		if (closing)
			return new AsyncWork<>(null, null, new CancelException("IO closed"));
		if (pos > size) {
			ISynchronizationPoint<IOException> resize = increaseSize(pos);
			size = pos;
			AsyncWork<Integer,IOException> sp = new AsyncWork<>();
			IOUtil.listenOnDone(resize, () -> {
				AsyncWork<Integer, IOException> write = writeAsync(pos, buf, alreadyDone, null);
				IOUtil.listenOnDone(write, sp, ondone);
			}, sp, ondone);
			return operation(sp);
		}
		
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer;
		if (pos < size || (size > 0 && getBufferIndex(size - 1) == bufferIndex))
			buffer = useBufferAsync(bufferIndex);
		else {
			BufferTable table = this.bufferTable;
			if (closing || table == null)
				return new AsyncWork<>(null, null, new CancelException("IO closed"));
			buffer = table.getForWrite(bufferIndex);
		}
		SynchronizationPoint<NoException> sp = buffer.loading;
		AsyncWork<Integer,IOException> done = new AsyncWork<>();
		Task<Void,NoException> task = new Task.Cpu<Void, NoException>("Writting to BufferedIO", getPriority()) {
			@Override
			public Void run() {
				if (buffer.error != null) {
					if (ondone != null) ondone.run(new Pair<>(null, buffer.error));
					done.unblockError(buffer.error);
					return null;
				}
				if (sp != null && sp.isCancelled()) {
					done.cancel(sp.getCancelEvent());
					return null;
				}
				if (closing) {
					done.cancel(new CancelException("IO closed"));
					return null;
				}
				int start = getBufferOffset(pos);
				int len = buf.remaining();
				if (len > buffer.buffer.length - start) len = buffer.buffer.length - start;
				buf.get(buffer.buffer, start, len);
				if (start + len > buffer.len)
					buffer.len = start + len;
				manager.toBeWritten(buffer);
				buffer.inUse--;
				long p = pos + len;
				BufferedIO.this.pos = p;
				if (p < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
					long nextIndex = getBufferIndex(p);
					if (nextIndex != bufferIndex) loadBuffer(nextIndex);
				} else if (p > size)
					size = p;
				if (buf.remaining() == 0) {
					if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(len + alreadyDone), null));
					done.unblockSuccess(Integer.valueOf(len + alreadyDone));
					return null;
				}
				AsyncWork<Integer,IOException> next = writeAsync(p, buf, len + alreadyDone, null);
				IOUtil.listenOnDone(next, done, ondone);
				return null;
			}
		};
		task.getOutput().forwardCancel(done);
		if (sp == null || sp.isUnblocked())
			task.start();
		else
			sp.listenAsync(task, true);
		return operation(done);
	}
	
	protected int writeSync(long pos, ByteBuffer buf, int alreadyDone) throws IOException {
		if (pos > size) {
			ISynchronizationPoint<IOException> resize = increaseSize(pos);
			size = pos;
			try { resize.blockThrow(0); }
			catch (CancelException e) {
				throw new IOException("Cancelled", e);
			}
		}
		
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer;
		if (pos < size || (size > 0 && getBufferIndex(size - 1) == bufferIndex))
			buffer = useBufferAsync(bufferIndex);
		else
			buffer = bufferTable.getForWrite(bufferIndex);
		SynchronizationPoint<NoException> sp = buffer.loading;
		if (sp != null && !sp.isUnblocked())
			sp.block(0);
		if (buffer.error != null)
			throw buffer.error;
		int start = getBufferOffset(pos);
		int len = buf.remaining();
		if (len > buffer.buffer.length - start) len = buffer.buffer.length - start;
		buf.get(buffer.buffer, start, len);
		if (start + len > buffer.len)
			buffer.len = start + len;
		manager.toBeWritten(buffer);
		buffer.inUse--;
		long p = pos + len;
		this.pos = p;
		if (p < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			long nextIndex = getBufferIndex(p);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		} else if (p > size)
			size = p;
		if (buf.remaining() == 0) return alreadyDone + len;
		return writeSync(p, buf, alreadyDone + len);
	}
	
	protected void write(byte[] buf, int offset, int length) throws IOException {
		do {
			long bufferIndex = getBufferIndex(pos);
			Buffer buffer;
			if (pos < size || (size > 0 && getBufferIndex(size - 1) == bufferIndex))
				buffer = useBufferAsync(bufferIndex);
			else
				buffer = bufferTable.getForWrite(bufferIndex);
			SynchronizationPoint<NoException> sp = buffer.loading;
			if (sp != null && !sp.isUnblocked())
				sp.block(0);
			if (buffer.error != null)
				throw buffer.error;
			int start = getBufferOffset(pos);
			int len = length;
			if (len > buffer.buffer.length - start) len = buffer.buffer.length - start;
			System.arraycopy(buf, offset, buffer.buffer, start, len);
			if (start + len > buffer.len)
				buffer.len = start + len;
			manager.toBeWritten(buffer);
			buffer.inUse--;
			pos += len;
			offset += len;
			length -= len;
			if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
				long nextIndex = getBufferIndex(pos);
				if (nextIndex != bufferIndex) loadBuffer(nextIndex);
			} else if (pos > size)
				size = pos;
		} while (length > 0);
	}
	
	protected void write(byte b) throws IOException {
		long bufferIndex = getBufferIndex(pos);
		Buffer buffer;
		if (pos < size || (size > 0 && getBufferIndex(size - 1) == bufferIndex))
			buffer = useBufferAsync(bufferIndex);
		else
			buffer = bufferTable.getForWrite(bufferIndex);
		SynchronizationPoint<NoException> sp = buffer.loading;
		if (sp != null && !sp.isUnblocked())
			sp.block(0);
		if (buffer.error != null)
			throw buffer.error;
		int start = getBufferOffset(pos);
		buffer.buffer[start] = b;
		if (start + 1 > buffer.len)
			buffer.len = start + 1;
		manager.toBeWritten(buffer);
		buffer.inUse--;
		pos++;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			long nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		} else if (pos > size)
			size = pos;
	}
}
