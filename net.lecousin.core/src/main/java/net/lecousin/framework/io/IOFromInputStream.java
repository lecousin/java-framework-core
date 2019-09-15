package net.lecousin.framework.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/** Implements Readable from an InputStream. */
public class IOFromInputStream extends ConcurrentCloseable implements IO.Readable {

	/** Constructor. */
	public IOFromInputStream(InputStream stream, String sourceDescription, TaskManager manager, byte priority) {
		this.stream = stream;
		this.sourceDescription = sourceDescription;
		this.manager = manager;
		this.priority = priority;
	}
	
	private InputStream stream;
	private String sourceDescription;
	private TaskManager manager;
	private byte priority;
	
	/** Add the capability to get the size to IOFromInputStream. */
	public static class KnownSize extends IOFromInputStream implements IO.KnownSize {
		/** Constructor. */
		public KnownSize(InputStream stream, long size, String sourceDescription, TaskManager manager, byte priority) {
			super(stream, sourceDescription, manager, priority);
			this.size = size;
		}
		
		private long size;
		
		@Override
		public long getSizeSync() {
			return size;
		}
		
		@Override
		public AsyncWork<Long, IOException> getSizeAsync() {
			return new AsyncWork<>(Long.valueOf(size), null);
		}
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		return new SynchronizationPoint<>(true);
	}

	public InputStream getInputStream() { return stream; }
	
	@Override
	public String getSourceDescription() { return sourceDescription; }
	
	@Override
	public IO getWrappedIO() { return null; }
	
	@Override
	public byte getPriority() { return priority; }
	
	@Override
	public void setPriority(byte priority) { this.priority = priority; }
	
	@Override
	public TaskManager getTaskManager() {
		return manager;
	}
	
	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		return IOUtil.closeAsync(stream);
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
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
	public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		Task<Integer,IOException> t = new Task<Integer,IOException>(manager, "Read from InputStream", priority, ondone) {
			@Override
			public Integer run() throws IOException, CancelException {
				try {
					int nb = stream.read(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
					if (nb >= 0)
						buffer.position(buffer.position() + nb);
					return Integer.valueOf(nb);
				} catch (IOException e) {
					if (isClosing() || isClosed()) throw new CancelException("InputStream closed");
					throw e;
				}
			}
		};
		operation(t.start());
		return t.getOutput();
	}
	
	@Override
	public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		Task<Integer,IOException> t = new Task<Integer,IOException>(manager, "Read from InputStream", priority, ondone) {
			@Override
			public Integer run() throws IOException, CancelException {
				int total = 0;
				do {
					try {
						int nb = stream.read(
							buffer.array(),
							buffer.arrayOffset() + buffer.position() + total,
							buffer.remaining() - total);
						if (nb <= 0) break;
						total += nb;
					} catch (IOException e) {
						if (isClosing() || isClosed()) throw new CancelException("InputStream closed");
						throw e;
					}
				} while (total < buffer.remaining());
				buffer.position(buffer.position() + total);
				return Integer.valueOf(total);
			}
		};
		operation(t.start());
		return t.getOutput();
	}
	
	
	@Override
	public AsyncWork<Long,IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		if (n <= 0) {
			if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(0), null));
			return new AsyncWork<>(Long.valueOf(0), null);
		}
		// InputStream does not comply to our restrictions, and may end up after the end of the stream, so we cannot use the skip method
		Task<Long,IOException> t = new Task<Long,IOException>(manager, "Skip from InputStream", priority, ondone) {
			@Override
			public Long run() throws IOException, CancelException {
				long total = 0;
				byte[] b = new byte[n > 65536 ? 65536 : (int)n];
				do {
					int l = n - total > 65536 ? 65536 : (int)(n - total);
					if (isClosing() || isClosed()) throw new CancelException("InputStream closed");
					int nb = stream.read(b, 0, l);
					if (nb <= 0) break;
					total += nb;
				} while (total < n);
				return Long.valueOf(total);
			}
		};
		operation(t.start());
		return t.getOutput();
	}
	
}
