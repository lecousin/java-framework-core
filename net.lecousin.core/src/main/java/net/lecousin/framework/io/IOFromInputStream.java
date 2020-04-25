package net.lecousin.framework.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.util.Pair;

/** Implements Readable from an InputStream. */
public class IOFromInputStream extends AbstractIO implements IO.Readable {

	/** Constructor. */
	public IOFromInputStream(InputStream stream, String sourceDescription, TaskManager manager, Priority priority) {
		super(sourceDescription, priority);
		this.stream = stream;
		this.manager = manager;
	}
	
	private InputStream stream;
	private TaskManager manager;
	
	/** Add the capability to get the size to IOFromInputStream. */
	public static class KnownSize extends IOFromInputStream implements IO.KnownSize {
		/** Constructor. */
		public KnownSize(InputStream stream, long size, String sourceDescription, TaskManager manager, Priority priority) {
			super(stream, sourceDescription, manager, priority);
			this.size = size;
		}
		
		private long size;
		
		@Override
		public long getSizeSync() {
			return size;
		}
		
		@Override
		public AsyncSupplier<Long, IOException> getSizeAsync() {
			return new AsyncSupplier<>(Long.valueOf(size), null);
		}
	}
	
	@Override
	public IAsync<IOException> canStartReading() {
		return new Async<>(true);
	}

	public InputStream getInputStream() { return stream; }
	
	@Override
	public IO getWrappedIO() { return null; }
	
	@Override
	public TaskManager getTaskManager() {
		return manager;
	}
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return IOUtil.closeAsync(stream);
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		stream = null;
		ondone.unblock();
	}
	
	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		int nb = stream.read(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
		if (nb >= 0)
			buffer.position(buffer.position() + nb);
		return nb;
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		int total = 0;
		do {
			int nb = stream.read(buffer.array(), buffer.arrayOffset() + buffer.position() + total, buffer.remaining() - total);
			if (nb <= 0) break;
			total += nb;
		} while (total < buffer.remaining());
		buffer.position(buffer.position() + total);
		return total;
	}

	@Override
	public long skipSync(long n) throws IOException {
		if (n <= 0) return 0;
		// InputStream does not comply to our restrictions, and may end up after the end of the stream, so we cannot use the skip method
		long total = 0;
		byte[] b = new byte[n > 65536 ? 65536 : (int)n];
		do {
			int l = n - total > 65536 ? 65536 : (int)(n - total);
			int nb = stream.read(b, 0, l);
			if (nb <= 0) break;
			total += nb;
		} while (total < n);
		return total;
	}
	
	@Override
	public AsyncSupplier<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(new Task<Integer,IOException>(manager, "Read from InputStream", priority, null, t -> {
			try {
				int nb = stream.read(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
				if (nb >= 0)
					buffer.position(buffer.position() + nb);
				return Integer.valueOf(nb);
			} catch (IOException e) {
				if (isClosing() || isClosed()) throw IO.cancelClosed();
				throw e;
			}
		}, ondone).start()).getOutput();
	}
	
	@Override
	public AsyncSupplier<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(new Task<Integer,IOException>(manager, "Read from InputStream", priority, null, t -> {
			int total = 0;
			do {
				if (isClosing() || isClosed()) throw IO.cancelClosed();
				try {
					int nb = stream.read(
						buffer.array(),
						buffer.arrayOffset() + buffer.position() + total,
						buffer.remaining() - total);
					if (nb <= 0) break;
					total += nb;
				} catch (IOException e) {
					if (isClosing() || isClosed()) throw IO.cancelClosed();
					throw e;
				}
			} while (total < buffer.remaining());
			buffer.position(buffer.position() + total);
			return Integer.valueOf(total);
		}, ondone).start()).getOutput();
	}
	
	
	@Override
	public AsyncSupplier<Long,IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		if (n <= 0) {
			if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(0), null));
			return new AsyncSupplier<>(Long.valueOf(0), null);
		}
		// InputStream does not comply to our restrictions, and may end up after the end of the stream, so we cannot use the skip method
		return operation(new Task<Long,IOException>(manager, "Skip from InputStream", priority, null, t -> {
			long total = 0;
			byte[] b = new byte[n > 65536 ? 65536 : (int)n];
			do {
				int l = n - total > 65536 ? 65536 : (int)(n - total);
				if (isClosing() || isClosed()) throw IO.cancelClosed();
				int nb = stream.read(b, 0, l);
				if (nb <= 0) break;
				total += nb;
			} while (total < n);
			return Long.valueOf(total);
		}, ondone).start()).getOutput();
	}
	
}
