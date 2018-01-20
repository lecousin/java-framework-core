package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.collections.LinkedArrayList;
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
		loadBuffer(getBufferIndex(this.pos));
	}
	
	protected BufferingManager manager;
	protected IO.Readable.Seekable io;
	protected List<Buffer> buffers;
	private int firstBufferSize;
	private int bufferSize;
	protected long size;
	protected long pos;
	protected boolean startReadSecondBufferWhenFirstBufferFullyRead = true;
	
	/** Buffer. */
	protected static class Buffer extends BufferingManager.Buffer {
		private Buffer(BufferedIO owner) {
			this.owner = owner;
		}
		
		private SynchronizationPoint<NoException> loading;
		private IOException error;
	}
	
	protected void loadBuffer(int index) {
		Buffer buffer;
		synchronized (buffers) {
			while (index >= buffers.size())
				buffers.add(new Buffer(this));
			buffer = buffers.get(index);
			if (buffer.buffer != null) return;
			if (buffer.loading != null) return;
			buffer.loading = new SynchronizationPoint<>();
		}
		synchronized (buffer) {
			buffer.buffer = new byte[index == 0 ? firstBufferSize : bufferSize];
			AsyncWork<Integer,IOException> loading =
				io.readFullyAsync(index == 0 ? 0 : (firstBufferSize + (index - 1) * bufferSize), ByteBuffer.wrap(buffer.buffer));
			operation(loading).listenInline(new Runnable() {
				@Override
				public void run() {
					synchronized (buffer) {
						if (buffer.loading == null) return;
						if (!loading.isSuccessful()) {
							if (loading.isCancelled()) buffer.loading.cancel(loading.getCancelEvent());
							else {
								buffer.error = loading.getError();
								buffer.loading.unblock();
							}
							buffer.buffer = null;
							return;
						}
						buffer.len = loading.getResult().intValue();
						if (buffer.len < 0) buffer.len = 0;
						buffer.lastRead = System.currentTimeMillis();
						manager.newBuffer(buffer);
						buffer.loading.unblock();
					}
				}
			});
		}
	}
	
	/** Load the buffer if needed. */
	protected Buffer useBufferAsync(int index) {
		do {
			Buffer b = null;
			try {
				synchronized (buffers) {
					if (index < buffers.size())
						b = buffers.get(index);
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
					b = new Buffer(this);
					b.loading = new SynchronizationPoint<>(new CancelException("IO closed"));
					return b;
				}
				if (b == null) b = new Buffer(this);
				b.error = IO.error(t);
				b.loading = new SynchronizationPoint<>(true);
				return b;
			}
		} while (true);
	}
	
	/** Load the buffer if needed and wait for it. */
	protected Buffer useBufferSync(int index) throws IOException {
		Buffer b = useBufferAsync(index);
		SynchronizationPoint<NoException> sp = b.loading;
		if (sp != null) sp.block(0);
		if (b.error != null) throw b.error;
		return b;
	}
	
	protected int getBufferIndex(long pos) {
		if (pos < firstBufferSize) return 0;
		return (int)(((pos - firstBufferSize) / bufferSize)) + 1;
	}
	
	protected int getBufferOffset(long pos) {
		if (pos < firstBufferSize) return (int)pos;
		pos -= firstBufferSize;
		return (int)(pos % bufferSize);
	}
	
	protected long getBufferStart(int index) {
		if (index == 0) return 0;
		if (index == 1) return firstBufferSize;
		return firstBufferSize + ((index - 1) * ((long)bufferSize));
	}
	
	/** Ask to cancel all buffers. This can be used before to close if the data is not needed anymore
	 * so there is no need to write pending buffers when closing.
	 */
	public void cancelAll() {
		synchronized (buffers) {
			for (Buffer b : buffers) {
				manager.removeBuffer(b);
				synchronized (b) {
					b.buffer = null;
					if (b.loading != null) b.loading.cancel(new CancelException("BufferedIO.cancelAll"));
					b.loading = null;
					if (b.flushing != null)
						while (!b.flushing.isEmpty())
							b.flushing.removeFirst().cancel(new CancelException("BufferedIO.cancelAll"));
					b.flushing = null;
				}
			}
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
		buffers = null;
		ondone.unblock();
	}
	
	@Override
	boolean removing(BufferingManager.Buffer buffer) {
		Buffer b = (Buffer)buffer;
		if (b.loading == null) return true;
		if (!b.loading.isUnblocked()) return false;
		b.loading = null;
		return true;
	}
	
	@Override
	AsyncWork<?,IOException> flushWrite(BufferingManager.Buffer buffer) {
		long pos;
		synchronized (buffers) {
			int i = buffers.indexOf(buffer);
			if (i == 0) pos = 0;
			else if (i == 1) pos = firstBufferSize;
			else pos = firstBufferSize + ((i - 1) * ((long)bufferSize));
		}
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
					operation(increaseSize(newSize)).blockResult(0);
				else
					operation(decreaseSize(newSize)).blockResult(0);
			} catch (CancelException e) {
				throw new IOException("Cancelled", e);
			}
		}
		
		@Override
		public AsyncWork<Void, IOException> setSizeAsync(long newSize) {
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
		int i = getBufferIndex(pos);
		if (i >= buffers.size()) i = 0;
		Buffer b = buffers.get(i);
		SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
		SynchronizationPoint<NoException> l = b.loading;
		if (l != null && !l.isUnblocked()) {
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
		if (b.error != null)
			sp.error(b.error);
		else
			sp.unblock();
		return sp;
	}
	
	protected int read() throws IOException {
		if (pos == size) return -1;
		int bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferSync(bufferIndex);
		buffer.lastRead = System.currentTimeMillis();
		byte b = buffer.buffer[getBufferOffset(pos)];
		pos++;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			int nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		}
		buffer.inUse--;
		return b & 0xFF;
	}

	protected int read(byte[] buf, int offset, int len) throws IOException {
		if (pos == size) return -1;
		int bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferSync(bufferIndex);
		int start = getBufferOffset(pos);
		if (len > buffer.len - start) len = buffer.len - start;
		System.arraycopy(buffer.buffer, start, buf, offset, len);
		buffer.lastRead = System.currentTimeMillis();
		buffer.inUse--;
		pos += len;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			int nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		}
		return len;
	}
	
	protected int readSync(long pos, ByteBuffer buf) throws IOException {
		if (pos >= size)
			return -1;
		int bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferSync(bufferIndex);
		int start = getBufferOffset(pos);
		int len = buf.remaining();
		if (len > buffer.len - start) len = buffer.len - start;
		if (len <= 0)
			return 0;
		buf.put(buffer.buffer, start, len);
		buffer.lastRead = System.currentTimeMillis();
		buffer.inUse--;
		pos += len;
		this.pos = pos;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			int nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		}
		return len;
	}
	
	protected int readAsync() throws IOException {
		if (pos == size) return -1;
		int bufferIndex = getBufferIndex(pos);
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
			int nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		}
		return b & 0xFF;
	}
	
	protected AsyncWork<Integer,IOException> readAsync(long pos, ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		if (pos >= size) {
			if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(0), null));
			return new AsyncWork<Integer,IOException>(Integer.valueOf(0), null);
		}
		int bufferIndex = getBufferIndex(pos);
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
					int nextIndex = getBufferIndex(p);
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
		int bufferIndex = getBufferIndex(pos);
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
				int nextIndex = getBufferIndex(p);
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
					int nextIndex = getBufferIndex(p);
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
		int bufferIndex = getBufferIndex(pos);
		Buffer buffer = useBufferSync(bufferIndex);
		int start = getBufferOffset(pos);
		int len = buf.length;
		if (len > buffer.len - start) len = buffer.len - start;
		System.arraycopy(buffer.buffer, start, buf, 0, len);
		buffer.lastRead = System.currentTimeMillis();
		buffer.inUse--;
		pos += len;
		if (pos < size && (bufferIndex != 0 || startReadSecondBufferWhenFirstBufferFullyRead)) {
			int nextIndex = getBufferIndex(pos);
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
		int bufferIndex = getBufferIndex(pos);
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
			int nextIndex = getBufferIndex(pos);
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
		int bufferIndex = getBufferIndex(pos);
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
					int nextIndex = getBufferIndex(p);
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
	
	protected AsyncWork<Void,IOException> increaseSize(long newSize) {
		AsyncWork<Void,IOException> resize = ((IO.Resizable)io).setSizeAsync(newSize);
		SynchronizationPoint<NoException> lastBuffer = null;
		
		int prevIndex = size > 0 ? getBufferIndex(size - 1) : 0;
		int firstIndex = getBufferIndex(size);
		int lastIndex = getBufferIndex(newSize - 1);
		
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
		synchronized (buffers) {
			while (firstIndex > buffers.size())
				buffers.add(new Buffer(this));
			while (firstIndex <= lastIndex) {
				Buffer b;
				if (firstIndex == buffers.size()) {
					b = new Buffer(this);
					buffers.add(b);
				} else
					b = buffers.get(firstIndex);
				if (b.buffer == null && (b.loading == null || b.loading.isUnblocked())) {
					b.buffer = new byte[firstIndex > 0 ? bufferSize : firstBufferSize];
					newBuffers.add(b);
				}
				if (b.loading != null && !b.loading.isUnblocked()) {
					int i = firstIndex;
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
		AsyncWork<Void, IOException> sp = new AsyncWork<>();
		if (lastBuffer != null)
			jp.addToJoin(lastBuffer);
		jp.start();
		resize.listenInline(() -> {
			if (jp.isUnblocked()) {
				size = newSize;
				sp.unblockSuccess(null);
			} else jp.listenInline(new Runnable() {
				@Override
				public void run() {
					size = newSize;
					sp.unblockSuccess(null);
				}
			});
		}, sp);
		return sp;
	}
	
	protected AsyncWork<Void,IOException> decreaseSize(long newSize) {
		AsyncWork<Void,IOException> result = new AsyncWork<>();
		if (newSize < 0) newSize = 0;
		// we need to remove the buffers, and ensure there is no waiting operation on them
		int lastBufferIndex = newSize == 0 ? -1 : getBufferIndex(newSize - 1);
		JoinPoint<Exception> jp = new JoinPoint<>();
		synchronized (buffers) {
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
		long ns = newSize;
		jp.listenAsync(new Task.Cpu<Void,NoException>("Decrease size of BufferedIO", getPriority()) {
			@Override
			public Void run() {
				Buffer lastBuffer = null;
				LinkedList<Buffer> removed = new LinkedList<>();
				synchronized (buffers) {
					while (buffers.size() > lastBufferIndex + 1) {
						Buffer buffer = buffers.remove(buffers.size() - 1);
						if (buffer.buffer != null)
							removed.add(buffer);
					}
					if (ns > 0) lastBuffer = buffers.get(lastBufferIndex);
				}
				while (!removed.isEmpty())
					manager.removeBuffer(removed.removeFirst());
				// decrease size of last buffer if any
				if (lastBuffer != null) {
					int lastBufferSize;
					if (ns <= firstBufferSize)
						lastBufferSize = (int)ns;
					else
						lastBufferSize = (int)((ns - firstBufferSize) % bufferSize);
					lastBuffer.len = lastBufferSize;
					lastBuffer.lastRead = System.currentTimeMillis();
				}
				AsyncWork<Void,IOException> resize = ((IO.Resizable)io).setSizeAsync(ns);
				resize.listenInline(() -> {
					size = ns;
					if (pos > size) pos = size;
					result.unblockSuccess(null);
				}, result);
				return null;
			}
		}, true);
		return result;
	}

	protected AsyncWork<Integer, IOException> writeAsync(
		long pos, ByteBuffer buf, int alreadyDone, RunnableWithParameter<Pair<Integer,IOException>> ondone)
	{
		if (closing)
			return new AsyncWork<>(null, null, new CancelException("IO closed"));
		if (pos > size) {
			AsyncWork<Void,IOException> resize = increaseSize(pos);
			size = pos;
			AsyncWork<Integer,IOException> sp = new AsyncWork<>();
			IOUtil.listenOnDone(resize, (result) -> {
				AsyncWork<Integer, IOException> write = writeAsync(pos, buf, alreadyDone, null);
				IOUtil.listenOnDone(write, sp, ondone);
			}, sp, ondone);
			return operation(sp);
		}
		
		int bufferIndex = getBufferIndex(pos);
		Buffer buffer;
		if (pos < size || (size > 0 && getBufferIndex(size - 1) == bufferIndex))
			buffer = useBufferAsync(bufferIndex);
		else {
			List<Buffer> buffers = this.buffers;
			if (closing || buffers == null)
				return new AsyncWork<>(null, null, new CancelException("IO closed"));
			boolean isNew = false;
			synchronized (buffers) {
				if (bufferIndex == buffers.size()) {
					buffer = new Buffer(this);
					buffer.buffer = new byte[bufferIndex == 0 ? firstBufferSize : bufferSize];
					buffer.len = 0;
					buffer.inUse = 1;
					buffers.add(buffer);
					isNew = true;
				} else {
					buffer = buffers.get(bufferIndex);
					if (buffer.buffer == null && (buffer.loading == null || buffer.loading.isUnblocked())) {
						buffer.buffer = new byte[bufferIndex == 0 ? firstBufferSize : bufferSize];
						buffer.len = 0;
						isNew = true;
					}
					buffer.inUse++;
				}
			}
			if (isNew) manager.newBuffer(buffer);
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
					int nextIndex = getBufferIndex(p);
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
			AsyncWork<Void,IOException> resize = increaseSize(pos);
			size = pos;
			try { resize.blockResult(0); }
			catch (CancelException e) {
				throw new IOException("Cancelled", e);
			}
		}
		
		int bufferIndex = getBufferIndex(pos);
		Buffer buffer;
		if (pos < size || (size > 0 && getBufferIndex(size - 1) == bufferIndex))
			buffer = useBufferAsync(bufferIndex);
		else {
			boolean isNew = false;
			synchronized (buffers) {
				if (bufferIndex == buffers.size()) {
					buffer = new Buffer(this);
					buffer.buffer = new byte[bufferIndex == 0 ? firstBufferSize : bufferSize];
					buffer.len = 0;
					buffer.inUse = 1;
					buffers.add(buffer);
					isNew = true;
				} else {
					buffer = buffers.get(bufferIndex);
					if (buffer.buffer == null && (buffer.loading == null || buffer.loading.isUnblocked())) {
						buffer.buffer = new byte[bufferIndex == 0 ? firstBufferSize : bufferSize];
						buffer.len = 0;
						isNew = true;
					}
					buffer.inUse++;
				}
			}
			if (isNew) manager.newBuffer(buffer);
		}
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
			int nextIndex = getBufferIndex(p);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		} else if (p > size)
			size = p;
		if (buf.remaining() == 0) return alreadyDone + len;
		return writeSync(p, buf, alreadyDone + len);
	}
	
	protected void write(byte[] buf, int offset, int length) throws IOException {
		do {
			int bufferIndex = getBufferIndex(pos);
			Buffer buffer;
			if (pos < size || (size > 0 && getBufferIndex(size - 1) == bufferIndex))
				buffer = useBufferAsync(bufferIndex);
			else {
				boolean isNew = false;
				synchronized (buffers) {
					if (bufferIndex == buffers.size()) {
						buffer = new Buffer(this);
						buffer.buffer = new byte[bufferIndex == 0 ? firstBufferSize : bufferSize];
						buffer.len = 0;
						buffer.inUse = 1;
						buffers.add(buffer);
						isNew = true;
					} else {
						buffer = buffers.get(bufferIndex);
						if (buffer.buffer == null && (buffer.loading == null || buffer.loading.isUnblocked())) {
							buffer.buffer = new byte[bufferIndex == 0 ? firstBufferSize : bufferSize];
							buffer.len = 0;
							isNew = true;
						}
						buffer.inUse++;
					}
				}
				if (isNew) manager.newBuffer(buffer);
			}
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
				int nextIndex = getBufferIndex(pos);
				if (nextIndex != bufferIndex) loadBuffer(nextIndex);
			} else if (pos > size)
				size = pos;
		} while (length > 0);
	}
	
	protected void write(byte b) throws IOException {
		int bufferIndex = getBufferIndex(pos);
		Buffer buffer;
		if (pos < size || (size > 0 && getBufferIndex(size - 1) == bufferIndex))
			buffer = useBufferAsync(bufferIndex);
		else {
			boolean isNew = false;
			synchronized (buffers) {
				if (bufferIndex == buffers.size()) {
					buffer = new Buffer(this);
					buffer.buffer = new byte[bufferIndex == 0 ? firstBufferSize : bufferSize];
					buffer.len = 0;
					buffer.inUse = 1;
					buffers.add(buffer);
					isNew = true;
				} else {
					buffer = buffers.get(bufferIndex);
					if (buffer.buffer == null && (buffer.loading == null || buffer.loading.isUnblocked())) {
						buffer.buffer = new byte[bufferIndex == 0 ? firstBufferSize : bufferSize];
						buffer.len = 0;
						isNew = true;
					}
					buffer.inUse++;
				}
			}
			if (isNew) manager.newBuffer(buffer);
		}
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
			int nextIndex = getBufferIndex(pos);
			if (nextIndex != bufferIndex) loadBuffer(nextIndex);
		} else if (pos > size)
			size = pos;
	}
}
