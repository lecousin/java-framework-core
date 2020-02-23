package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * This implementation of buffered readable IO use a simple buffering strategy, with 2 buffers:
 * while the first one is used, the second is filled in background.
 * <br/>
 * Note that buffers are not necessarily fully filled, as the operation used to fill a buffer is readAsync and not readFullyAsync.
 * <br/>
 * The first read operation is started by the constructor.
 * <br/>
 * When a new byte is requested, if the position in the current buffer didn't reach its size, the byte is taken from the current buffer.
 * When reaching the end of the current buffer, a new buffer is requested:<ul>
 * 	<li>If the second buffer is ready, it becomes the current buffer, and a new read operation is started in background
 * 	to fill the next buffer</li>
 *  <li>If the second buffer is not yet ready, the operation blocks until the second buffer is ready.</li>
 * </ul>
 */
public class SimpleBufferedReadable extends ConcurrentCloseable<IOException> implements IO.Readable.Buffered {
	
	/** Constructor. */
	public SimpleBufferedReadable(IO.Readable io, int bufferSize) {
		this.io = io;
		readBuffer = ByteBuffer.allocate(bufferSize);
		readTask = io.readAsync(readBuffer);
		bb = ByteBuffer.allocate(bufferSize);
		state = new AtomicState();
		state.pos = state.len = 0;
		state.buffer = bb.array();
	}
	
	private IO.Readable io;
	private ByteBuffer readBuffer;
	private AtomicState state;
	private ByteBuffer bb;
	private AsyncSupplier<Integer,IOException> readTask;
	
	private static class AtomicState {
		private byte[] buffer;
		private int pos;
		private int len;
	}
	
	@Override
	public IAsync<IOException> canStartReading() {
		IAsync<IOException> sp = readTask;
		if (sp == null) sp = new Async<>(true);
		return sp;
	}
	
	@Override
	public TaskManager getTaskManager() { return io.getTaskManager(); }
	
	@Override
	public IO getWrappedIO() { return io; }
	
	@Override
	public String getSourceDescription() { return io.getSourceDescription(); }
	
	@Override
	public Priority getPriority() { return io.getPriority(); }
	
	@Override
	public void setPriority(Priority priority) { io.setPriority(priority); }
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		AsyncSupplier<Integer,IOException> currentRead = readTask;
		if (currentRead != null && !currentRead.isDone()) {
			currentRead.cancel(new CancelException("IO closed"));
			Async<IOException> sp = new Async<>();
			currentRead.onDone(() -> io.closeAsync().onDone(sp));
			return sp;
		}
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		readTask = null;
		state.buffer = null;
		bb = null;
		readBuffer = null;
		ondone.unblock();
	}
	
	/** Stop any pending read, and block until they are cancelled or done. */
	public void stop() {
		AsyncSupplier<Integer,IOException> currentRead = readTask;
		if (currentRead != null && !currentRead.isDone()) {
			currentRead.cancel(new CancelException("SimpleBufferedReadable.stop"));
			currentRead.block(0);
		}
	}
	
	@SuppressWarnings("squid:S2583") // false positive because concurrent operations
	private void fill() throws IOException, CancelException {
		AsyncSupplier<Integer,IOException> currentRead = readTask;
		if (currentRead == null)
			return;
		currentRead.block(0);
		if (!currentRead.isSuccessful()) {
			if (currentRead.isCancelled()) throw currentRead.getCancelEvent();
			Exception e = currentRead.getError();
			if (e instanceof IOException) throw (IOException)e;
			throw new IOException(e);
		}
		int nb = currentRead.getResult().intValue();
		if (nb <= 0) {
			state.pos = state.len = 0;
			state.buffer = null;
			bb = null;
			readBuffer = null;
			readTask = null;
			return;
		}
		if (readTask == null) return;
		AtomicState s = new AtomicState();
		s.buffer = readBuffer.array();
		s.pos = 0;
		s.len = readBuffer.position();
		state = s;
		ByteBuffer b = readBuffer;
		readBuffer = bb;
		bb = b;
		readBuffer.clear();
		readTask = io.readAsync(readBuffer);
	}

	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (s.buffer == null) return -1;
			try { fill(); }
			catch (CancelException e) { return -1; }
			if (state.pos == state.len) return -1;
		}
		int l = buffer.remaining();
		if (l > state.len - state.pos) l = state.len - state.pos;
		buffer.put(state.buffer, state.pos, l);
		state.pos += l;
		return l;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (s.buffer == null) return IOUtil.success(Integer.valueOf(-1), ondone);
			return readFullyAsync(buffer, ondone);
		}
		int l = buffer.remaining();
		if (l > state.len - state.pos) l = state.len - state.pos;
		buffer.put(state.buffer, state.pos, l);
		state.pos += l;
		if (!buffer.hasRemaining()) return IOUtil.success(Integer.valueOf(l), ondone);
		return IOUtil.readFullyAsync(this, buffer, l, ondone);
	}
	
	@Override
	public int readAsync() throws IOException {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (s.buffer == null) return -1;
			AsyncSupplier<Integer,IOException> currentRead = readTask;
			if (currentRead != null && currentRead.isDone())
				try { fill(); }
				catch (IOException e) { throw e; }
				catch (Exception t) { return -1; }
			return -2;
		}
		return s.buffer[s.pos++] & 0xFF;
	}

	@Override
	public AsyncSupplier<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readAsyncUsingSync(this, buffer, ondone));
	}

	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}

	@Override
	public AsyncSupplier<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsync(this, buffer, ondone);
	}

	@Override
	@SuppressWarnings("java:S1696") // catch NPE because of concurrency
	public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		AtomicState s = state;
		if (s.pos == s.len && s.buffer == null) return IOUtil.success(null, ondone);
		return operation(Task.cpu("Read next buffer", getPriority(), new Executable<ByteBuffer, IOException>() {
			@Override
			public ByteBuffer execute() throws CancelException, IOException {
				AtomicState st = state;
				if (st.pos == st.len) {
					if (st.buffer == null) return null;
					fill();
					if (state.pos == state.len) return null;
				}
				ByteBuffer buf;
				try {
					// wrap on our buffer
					buf = ByteBuffer.wrap(state.buffer, state.pos, state.len - state.pos).asReadOnlyBuffer();
					state.pos = state.len;
					// allocate a new buffer for the next read
					bb = ByteBuffer.allocate(state.buffer.length);
				} catch (NullPointerException e) {
					throw new CancelException("IO closed");
				}
				return buf;
			}
		}, ondone).start()).getOutput();
	}
	
	@Override
	@SuppressWarnings("squid:S1696") // NPE
	public ByteBuffer readNextBuffer() throws IOException {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (s.buffer == null) return null;
			try { fill(); }
			catch (CancelException e) {
				throw new ClosedChannelException();
			}
			if (state.pos == state.len) return null;
		}
		ByteBuffer buf;
		try {
			// wrap on our buffer
			buf = ByteBuffer.wrap(state.buffer, state.pos, state.len - state.pos).asReadOnlyBuffer();
			state.pos = state.len;
			// allocate a new buffer for the next read
			bb = ByteBuffer.allocate(state.buffer.length);
		} catch (NullPointerException e) {
			throw new ClosedChannelException();
		}
		return buf;
	}
	
	@Override
	public int skip(int skip) throws IOException {
		return (int)skipSync(skip);
	}

	@Override
	public long skipSync(long n) throws IOException {
		if (state.buffer == null || n <= 0) return 0;
		if (n <= state.len - state.pos) {
			state.pos += (int)n;
			return n;
		}
		AsyncSupplier<Integer,IOException> currentRead = readTask;
		if (currentRead == null)
			return 0;
		currentRead.block(0);
		if (!currentRead.isSuccessful()) {
			if (currentRead.isCancelled()) return 0;
			Exception e = currentRead.getError();
			if (e instanceof IOException) throw (IOException)e;
			throw new IOException(e);
		}
		int avail = currentRead.getResult().intValue();
		if (avail < 0) avail = 0;
		if (n <= state.len - state.pos + avail) {
			int i = state.len - state.pos;
			try { fill(); }
			catch (CancelException e) { return 0; }
			return skipSync(n - i) + i;
		}
		n = io.skipSync(n - ((state.len - state.pos) + avail));
		n += state.len - state.pos;
		n += avail;
		state.len = state.pos = 0;
		readBuffer.clear();
		readTask = io.readAsync(readBuffer);
		return n;
	}

	@Override
	public AsyncSupplier<Long,IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		if (state.buffer == null || n <= 0) return IOUtil.success(Long.valueOf(0), ondone);
		if (n <= state.len - state.pos) {
			state.pos += (int)n;
			if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(n), null));
			return new AsyncSupplier<>(Long.valueOf(n),null);
		}
		AsyncSupplier<Integer,IOException> currentRead = readTask;
		if (currentRead == null) return IOUtil.success(Long.valueOf(0), ondone);
		Task<Long, IOException> task = Task.cpu("Skipping bytes", io.getPriority(), new Executable<Long, IOException>() {
			@Override
			public Long execute() throws CancelException, IOException {
				if (currentRead.isCancelled()) return Long.valueOf(0);
				if (!currentRead.isSuccessful()) {
					if (currentRead.isCancelled()) throw currentRead.getCancelEvent();
					throw currentRead.getError();
				}
				int avail = currentRead.getResult().intValue();
				if (avail < 0) avail = 0;
				if (n <= state.len - state.pos + avail) {
					int i = state.len - state.pos;
					fill();
					return Long.valueOf(skipSync(n - i) + i);
				}
				long res = io.skipSync(n - ((state.len - state.pos) + avail));
				res += state.len - state.pos;
				res += avail;
				state.len = state.pos = 0;
				readBuffer.clear();
				readTask = io.readAsync(readBuffer);
				return Long.valueOf(res);
			}
		}, ondone);
		operation(task).startOn(readTask, true);
		return task.getOutput();
	}

	@Override
	public int read() throws IOException {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (s.buffer == null) return -1;
			try { fill(); }
			catch (CancelException e) { return -1; }
			if (state.pos == state.len) return -1;
		}
		return state.buffer[state.pos++] & 0xFF;
	}

	@Override
	public int read(byte[] buffer, int offset, int l) throws IOException {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (s.buffer == null) return -1;
			try { fill(); }
			catch (CancelException e) { return -1; }
			if (state.pos == state.len) return -1;
		}
		if (l > state.len - state.pos) l = state.len - state.pos;
		System.arraycopy(state.buffer, state.pos, buffer, offset, l);
		state.pos += l;
		return l;
	}

	@Override
	public int readFully(byte[] buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}

}
