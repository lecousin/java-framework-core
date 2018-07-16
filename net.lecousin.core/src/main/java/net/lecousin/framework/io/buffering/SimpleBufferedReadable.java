package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

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
public class SimpleBufferedReadable extends ConcurrentCloseable implements IO.Readable.Buffered {
	
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
	private AsyncWork<Integer,IOException> readTask;
	
	private static class AtomicState {
		private byte[] buffer;
		private int pos;
		private int len;
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		ISynchronizationPoint<IOException> sp = readTask;
		if (sp == null) sp = new SynchronizationPoint<>(true);
		return sp;
	}
	
	@Override
	public TaskManager getTaskManager() { return io.getTaskManager(); }
	
	@Override
	public IO getWrappedIO() { return io; }
	
	@Override
	public String getSourceDescription() { return io.getSourceDescription(); }
	
	@Override
	public byte getPriority() { return io.getPriority(); }
	
	@Override
	public void setPriority(byte priority) { io.setPriority(priority); }
	
	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		AsyncWork<Integer,IOException> currentRead = readTask;
		if (currentRead != null && !currentRead.isUnblocked()) {
			currentRead.cancel(new CancelException("IO closed"));
			SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
			currentRead.listenInline(new Runnable() {
				@Override
				public void run() {
					io.closeAsync().listenInline(sp);
				}
			});
			return sp;
		}
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		readTask = null;
		state.buffer = null;
		bb = null;
		readBuffer = null;
		ondone.unblock();
	}
	
	/** Stop any pending read, and block until they are cancelled or done. */
	public void stop() {
		AsyncWork<Integer,IOException> currentRead = readTask;
		if (currentRead != null && !currentRead.isUnblocked()) {
			currentRead.cancel(new CancelException("SimpleBufferedReadable.stop"));
			currentRead.block(0);
		}
	}
	
	private void fill() throws IOException, CancelException {
		AsyncWork<Integer,IOException> currentRead = readTask;
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
	public AsyncWork<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
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
	public int readAsync() {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (s.buffer == null) return -1;
			AsyncWork<Integer,IOException> currentRead = readTask;
			if (currentRead != null && currentRead.isUnblocked())
				try { fill(); }
				catch (Throwable t) { return -1; }
			return -2;
		}
		return s.buffer[s.pos++] & 0xFF;
	}

	@Override
	public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readAsyncUsingSync(this, buffer, ondone)).getOutput();
	}

	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}

	@Override
	public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsync(this, buffer, ondone);
	}

	@Override
	public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		AtomicState s = state;
		if (s.pos == s.len && s.buffer == null) return IOUtil.success(null, ondone);
		Task.Cpu<ByteBuffer, IOException> task = new Task.Cpu<ByteBuffer, IOException>("Read next buffer", getPriority(), ondone) {
			@Override
			public ByteBuffer run() throws IOException, CancelException {
				AtomicState s = state;
				if (s.pos == s.len) {
					if (s.buffer == null) return null;
					fill();
					if (state.pos == state.len) return null;
				}
				ByteBuffer buf = ByteBuffer.allocate(state.len - state.pos);
				try { buf.put(state.buffer, state.pos, state.len - state.pos); }
				catch (NullPointerException e) {
					throw new CancelException("IO closed");
				}
				state.pos = state.len;
				buf.flip();
				return buf;
			}
		};
		operation(task.start());
		return task.getOutput();
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
		AsyncWork<Integer,IOException> currentRead = readTask;
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
	public AsyncWork<Long,IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		if (state.buffer == null || n <= 0) return IOUtil.success(Long.valueOf(0), ondone);
		if (n <= state.len - state.pos) {
			state.pos += (int)n;
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(n), null));
			return new AsyncWork<Long,IOException>(Long.valueOf(n),null);
		}
		AsyncWork<Integer,IOException> currentRead = readTask;
		if (currentRead == null) return IOUtil.success(Long.valueOf(0), ondone);
		Task<Long,IOException> task = new Task.Cpu<Long,IOException>("Skipping bytes", io.getPriority(), ondone) {
			@Override
			public Long run() throws IOException, CancelException {
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
		};
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
