package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.memory.ByteArrayCache;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Utility class to implement the Buffered interface even in case buffers cannot be used.
 * An example it cannot be used is in case of a non-seekable IO, and we do not know in advance the size
 * of the data to read. Because we cannot seek, we may not exceed a certain position in the stream.
 * This must be used only in case a Buffered implementation cannot be used, and the number of read
 * operations are very limited.
 */
public class NonBufferedReadableIOAsBuffered extends ConcurrentCloseable<IOException> implements IO.Readable.Buffered {

	/** Constructor. */
	public NonBufferedReadableIOAsBuffered(IO.Readable io) {
		this.io = io;
	}
	
	private IO.Readable io;
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		io = null;
		ondone.unblock();
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
		return io.getPriority();
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
	public int readSync(ByteBuffer buffer) throws IOException {
		return io.readSync(buffer);
	}

	@Override
	public int readAsync() throws IOException {
		return read();
	}

	@Override
	public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return io.readAsync(buffer, ondone);
	}

	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return io.readFullySync(buffer);
	}

	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return io.readFullyAsync(buffer, ondone);
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return io.readAsync(buffer, ondone);
	}

	@Override
	public long skipSync(long n) throws IOException {
		return io.skipSync(n);
	}

	@Override
	public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		return io.skipAsync(n, ondone);
	}

	private byte[] b1 = new byte[1];
	private ByteBuffer bb1 = ByteBuffer.wrap(b1);
	
	@Override
	public int read() throws IOException {
		bb1.clear();
		int nb = io.readSync(bb1);
		if (nb <= 0) return -1;
		return b1[0] & 0xFF;
	}

	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		return io.readSync(ByteBuffer.wrap(buffer, offset, len));
	}

	@Override
	public IAsync<IOException> canStartReading() {
		return new Async<>(true);
	}

	@Override
	public int readFully(byte[] buffer) throws IOException {
		return io.readFullySync(ByteBuffer.wrap(buffer));
	}

	@Override
	public int skip(int skip) throws IOException {
		return (int)io.skipSync(skip);
	}

	@Override
	public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		AsyncSupplier<ByteBuffer, IOException> result = new AsyncSupplier<>();
		operation(Task.cpu("Read next buffer", getPriority(), t -> {
			ByteBuffer buf = ByteBuffer.allocate(4096);
			AsyncSupplier<Integer, IOException> read = readAsync(buf);
			read.onDone(() -> {
				if (read.hasError()) {
					if (ondone != null) ondone.accept(new Pair<>(null, read.getError()));
					result.unblockError(read.getError());
					return;
				}
				int nb = read.getResult().intValue();
				if (nb <= 0) {
					if (ondone != null) ondone.accept(new Pair<>(null, null));
					result.unblockSuccess(null);
					return;
				}
				buf.flip();
				if (ondone != null) ondone.accept(new Pair<>(buf, null));
				result.unblockSuccess(buf);
			});
			return null;
		}).start());
		return result;
	}
	
	@Override
	public ByteBuffer readNextBuffer() throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(ByteArrayCache.getInstance().get(4096, true));
		int nb = readSync(buf);
		if (nb <= 0) return null;
		buf.flip();
		return buf;
	}
	
}
