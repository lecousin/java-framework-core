package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;
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
import net.lecousin.framework.memory.ByteArrayCache;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * This is the less efficient buffered readable implementation: it uses a single buffer, which is filled only when first byte
 * is read (and so is blocking).
 * <br/>
 * It should not be used except in very specific situations, when we don't know the size of the data, and cannot predict
 * if more bytes are available until we actually need to read more data.
 * <br/>
 * An example of usage is in case of data split over several files. When reaching the end of a file, we need to open the next
 * file, but the next file may require to ask the user to insert a new disc. In that case, if we try to fill a buffer in
 * advance we may ask the user to insert a disc that does not exist, because we don't know in advance the number of discs.
 */
public class SingleBufferReadable extends ConcurrentCloseable<IOException> implements IO.Readable.Buffered {

	/** Constructor. */
	public SingleBufferReadable(IO.Readable io, int bufferSize, boolean useReadFully) {
		this.io = io;
		this.buffer = new byte[bufferSize];
		this.useReadFully = useReadFully;
		this.state = new AtomicState();
		state.pos = state.len = 0;
		state.eof = false;
		fillNextBuffer();
	}
	
	private IO.Readable io;
	private boolean useReadFully;
	private byte[] buffer;
	private AtomicState state;
	private AsyncSupplier<Integer, IOException> reading;
	
	private static class AtomicState {
		private int len;
		private int pos;
		private boolean eof;
	}
	
	@Override
	public IAsync<IOException> canStartReading() {
		return new Async<>(true);
	}

	private void fillNextBuffer() {
		if (useReadFully)
			reading = io.readFullyAsync(ByteBuffer.wrap(buffer), result -> {
				if (result.getValue1() == null) return;
				AtomicState ns = new AtomicState();
				ns.len = result.getValue1().intValue();
				if (ns.len <= 0) {
					ns.len = 0;
					ns.eof = true;
				} else if (ns.len < buffer.length) {
					ns.eof = true;
				} else {
					ns.eof = false;
				}
				ns.pos = 0;
				state = ns;
			});
		else
			reading = io.readAsync(ByteBuffer.wrap(buffer), result -> {
				if (result.getValue1() == null) return;
				AtomicState ns = new AtomicState();
				ns.len = result.getValue1().intValue();
				if (ns.len <= 0) {
					ns.len = 0;
					ns.eof = true;
				} else {
					ns.eof = false;
				}
				ns.pos = 0;
				state = ns;
			});
		operation(reading);
	}
	
	private void waitBufferSync() throws IOException {
		reading.blockException(0);
		if (reading.isCancelled()) throw new IOException("Read cancelled", reading.getCancelEvent());
	}

	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (s.eof) return 0;
			waitBufferSync();
			return readSync(buffer);
		}
		int l = buffer.remaining();
		if (l > s.len - s.pos) l = s.len - s.pos;
		buffer.put(this.buffer, s.pos, l);
		s.pos += l;
		if (s.pos == s.len)
			fillNextBuffer();
		return l;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (s.eof) return IOUtil.success(Integer.valueOf(-1), ondone);
			return readFullyAsync(buffer, ondone);
		}
		int l = buffer.remaining();
		if (l > s.len - s.pos) l = s.len - s.pos;
		buffer.put(this.buffer, s.pos, l);
		s.pos += l;
		if (s.pos == s.len)
			fillNextBuffer();
		if (!buffer.hasRemaining()) return IOUtil.success(Integer.valueOf(l), ondone);
		return operation(IOUtil.readFullyAsync(this, buffer, l, ondone));
	}

	@Override
	public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readAsyncUsingSync(this, buffer, ondone));
	}
	
	@Override
	public int readAsync() throws IOException {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (reading.hasError()) throw reading.getError();
			if (s.eof) return -1;
			return -2;
		}
		int c = buffer[s.pos++] & 0xFF;
		if (s.pos == s.len) fillNextBuffer();
		return c;
	}

	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}

	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsync(this, buffer, ondone));
	}

	@Override
	public long skipSync(long n) throws IOException {
		if (n <= 0)
			return 0;
		long nb = 0;
		while (n > 0) {
			AtomicState s = state;
			if (s.pos == s.len) {
				if (s.eof) return nb;
				waitBufferSync();
			}
			int l = state.len - state.pos;
			if (l > n) l = (int)n;
			state.pos += l;
			nb += l;
			n -= l;
			if (state.pos == state.len)
				fillNextBuffer();
		}
		return nb;
	}

	@Override
	public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		return operation(IOUtil.skipAsyncByReading(this, n, ondone));
	}

	@Override
	public String getSourceDescription() {
		return io.getSourceDescription();
	}

	@Override
	public IO getWrappedIO() {
		return io;
	}

	@Override
	public Priority getPriority() {
		return io != null ? io.getPriority() : Priority.NORMAL;
	}

	@Override
	public void setPriority(Priority priority) {
		io.setPriority(priority);
	}

	@Override
	public TaskManager getTaskManager() {
		return io.getTaskManager();
	}

	@Override
	public int read() throws IOException {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (s.eof) return -1;
			waitBufferSync();
			return read();
		}
		int c = buffer[s.pos++] & 0xFF;
		if (s.pos == s.len) fillNextBuffer();
		return c;
	}

	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		AtomicState s = state;
		if (s.pos == s.len) {
			if (s.eof) return 0;
			waitBufferSync();
			return read(buffer, offset, len);
		}
		int l = len;
		if (l > s.len - s.pos) l = s.len - s.pos;
		System.arraycopy(this.buffer, s.pos, buffer, offset, l);
		s.pos += l;
		if (s.pos == s.len) fillNextBuffer();
		return l;
	}

	@Override
	public int readFully(byte[] buffer) throws IOException {
		return IOUtil.readFully(this, ByteBuffer.wrap(buffer));
	}

	@Override
	public int skip(int skip) throws IOException {
		return (int)skipSync(skip);
	}

	@Override
	public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		AtomicState s = state;
		if (s.pos == s.len && s.eof) return IOUtil.success(null, ondone);
		Task<ByteBuffer, IOException> task = Task.cpu("Read next buffer", getPriority(),
			new Executable.FromSupplierThrows<>(this::readNextBuffer), ondone);
		task.startOn(reading, true);
		return operation(task).getOutput();
	}
	
	@Override
	public ByteBuffer readNextBuffer() throws IOException {
		do {
			if (reading.hasError()) throw reading.getError();
			if (reading.isCancelled()) throw IO.errorCancelled(reading.getCancelEvent());
			AtomicState s = state;
			if (s.pos == s.len) {
				if (s.eof) return null;
				waitBufferSync();
				continue;
			}
			ByteBuffer buf = ByteBuffer.wrap(ByteArrayCache.getInstance().get(s.len - s.pos, true));
			buf.put(buffer, s.pos, s.len - s.pos);
			s.pos = s.len;
			fillNextBuffer();
			buf.flip();
			return buf;
		} while (true);
	}
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		if (!reading.isDone()) reading.cancel(new CancelException("IO closed"));
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		buffer = null;
		io = null;
		ondone.unblock();
	}
	
}
