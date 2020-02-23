package net.lecousin.framework.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.event.ListenableLongProperty;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Add the capability to known on which position we are on an IO.
 * @param <IOType> type of IO
 */
public abstract class PositionKnownWrapper<IOType extends IO> extends ConcurrentCloseable<IOException> implements IO.PositionKnown {

	/** Constructor with initial position. */
	public PositionKnownWrapper(IOType io, long position) {
		this.io = io;
		this.position = new ListenableLongProperty(position);
	}
	
	protected IOType io;
	protected ListenableLongProperty position;
	
	/** Register a listener to be called when position is changing. */
	@SuppressWarnings("squid:S4276")
	public void addPositionChangedListener(Consumer<Long> listener) {
		position.addListener(listener);
	}

	/** Register a listener to be called when position is changing. */
	public void addPositionChangedListener(Runnable listener) {
		position.addListener(listener);
	}

	/** Remove the given listener. */
	@SuppressWarnings("squid:S4276")
	public void removePositionChangedListener(Consumer<Long> listener) {
		position.removeListener(listener);
	}

	/** Remove the given listener. */
	public void removePositionChangedListener(Runnable listener) {
		position.removeListener(listener);
	}
	
	/**
	 * Add the capability to known on which position we are on a Readable.
	 */
	public static class Readable extends PositionKnownWrapper<IO.Readable> implements IO.Readable, IO.PositionKnown {
		
		/** Constructor with initial position. */
		public Readable(IO.Readable io, long position) {
			super(io, position);
		}
		
		/** Constructor. */
		public Readable(IO.Readable io) {
			this(io, 0);
		}

		@Override
		public IAsync<IOException> canStartReading() {
			return super.canStartReading();
		}
		
		@Override
		public int readSync(ByteBuffer buffer) throws IOException {
			return super.readSync(buffer);
		}

		@Override
		public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return super.readAsync(buffer, ondone);
		}
		
		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			return super.readFullySync(buffer);
		}
		
		@Override
		public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return super.readFullyAsync(buffer, ondone);
		}
		
		@Override
		public long skipSync(long n) throws IOException {
			return super.skipSync(n);
		}

		@Override
		public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
			return super.skipAsync(n, ondone);
		}
		
		/**
		 * Add the capability to known on which position we are on a Buffered Readable.
		 */
		public static class Buffered extends PositionKnownWrapper<IO.Readable.Buffered> implements IO.Readable.Buffered, IO.PositionKnown {

			/** Constructor with initial position. */
			public Buffered(IO.Readable.Buffered io, long position) {
				super(io, position);
			}
			
			/** Constructor. */
			public Buffered(IO.Readable.Buffered io) {
				this(io, 0);
			}

			@Override
			public IAsync<IOException> canStartReading() {
				return super.canStartReading();
			}
			
			@Override
			public int readSync(ByteBuffer buffer) throws IOException {
				return super.readSync(buffer);
			}
			
			@Override
			public int readAsync() throws IOException {
				return super.readAsync();
			}

			@Override
			public AsyncSupplier<Integer, IOException> readAsync(
				ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
			) {
				return super.readAsync(buffer, ondone);
			}
			
			@Override
			public int readFullySync(ByteBuffer buffer) throws IOException {
				return super.readFullySync(buffer);
			}
			
			@Override
			public AsyncSupplier<Integer, IOException> readFullyAsync(
				ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
			) {
				return super.readFullyAsync(buffer, ondone);
			}
			
			@Override
			public long skipSync(long n) throws IOException {
				return super.skipSync(n);
			}

			@Override
			public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
				return super.skipAsync(n, ondone);
			}
			
			@Override
			public int read() throws IOException {
				return super.read();
			}

			@Override
			public int read(byte[] buffer, int offset, int len) throws IOException {
				return super.read(buffer, offset, len);
			}

			@Override
			public int readFully(byte[] buffer) throws IOException {
				return super.readFully(buffer);
			}

			@Override
			public int skip(int skip) throws IOException {
				return super.skip(skip);
			}

			@Override
			public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
				return super.readNextBufferAsync(ondone);
			}
			
			@Override
			public ByteBuffer readNextBuffer() throws IOException {
				return super.readNextBuffer();
			}

			@Override
			public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(
				ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
			) {
				return super.readFullySyncIfPossible(buffer, ondone);
			}
		}

	}
	
	// --- Common ---
	
	@Override
	public String getSourceDescription() { return io.getSourceDescription(); }
	
	@Override
	public IO getWrappedIO() { return io; }
	
	@Override
	public Priority getPriority() { return io.getPriority(); }
	
	@Override
	public void setPriority(Priority priority) { io.setPriority(priority); }
	
	@Override
	public TaskManager getTaskManager() { return io.getTaskManager(); }
	
	@Override
	public long getPosition() { return position.get(); }
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		io = null;
		ondone.unblock();
	}

	// --- IO.Readable ---
	
	protected IAsync<IOException> canStartReading() {
		return ((IO.Readable)io).canStartReading();
	}

	protected int readSync(ByteBuffer buffer) throws IOException {
		int nb = ((IO.Readable)io).readSync(buffer);
		if (nb > 0)
			position.add(nb);
		return nb;
	}

	protected int readAsync() throws IOException {
		int c = ((IO.Readable.Buffered)io).readAsync();
		if (c >= 0) position.inc();
		return c;
	}
	
	protected AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return ((IO.Readable)io).readAsync(buffer, result -> {
			Integer nb = result.getValue1();
			if (nb != null && nb.intValue() > 0)
				position.add(nb.longValue());
			if (ondone != null)
				ondone.accept(result);
		});
	}
	
	protected int readFullySync(ByteBuffer buffer) throws IOException {
		int nb = ((IO.Readable)io).readFullySync(buffer);
		if (nb > 0)
			position.add(nb);
		return nb;
	}
	
	protected AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return ((IO.Readable)io).readFullyAsync(buffer, result -> {
			Integer nb = result.getValue1();
			if (nb != null && nb.intValue() > 0)
				position.add(nb.intValue());
			if (ondone != null)
				ondone.accept(result);
		});
	}
	
	protected long skipSync(long n) throws IOException {
		if (n <= 0) return 0;
		long skipped = ((IO.Readable)io).skipSync(n);
		position.add(skipped);
		return skipped;
	}

	protected AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
		if (n <= 0) return IOUtil.success(Long.valueOf(0), ondone);
		return ((IO.Readable)io).skipAsync(n, result -> {
			Long nb = result.getValue1();
			if (nb != null)
				position.add(nb.longValue());
			if (ondone != null)
				ondone.accept(result);
		});
	}

	// --- IO.Readable.Buffered
	
	protected int read() throws IOException {
		int c = ((IO.Readable.Buffered)io).read();
		if (c >= 0) position.inc();
		return c;
	}

	protected int read(byte[] buffer, int offset, int len) throws IOException {
		int nb = ((IO.Readable.Buffered)io).read(buffer, offset, len);
		if (nb > 0)
			position.add(nb);
		return nb;
	}

	protected int readFully(byte[] buffer) throws IOException {
		int nb = ((IO.Readable.Buffered)io).readFully(buffer);
		if (nb > 0)
			position.add(nb);
		return nb;
	}

	protected int skip(int skip) throws IOException {
		int skipped = ((IO.Readable.Buffered)io).skip(skip);
		position.add(skipped);
		return skipped;
	}

	protected AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		return ((IO.Readable.Buffered)io).readNextBufferAsync(result -> {
			ByteBuffer buf = result.getValue1();
			if (buf != null)
				position.add(buf.remaining());
			if (ondone != null)
				ondone.accept(result);
		});
	}
	
	protected ByteBuffer readNextBuffer() throws IOException {
		ByteBuffer buf = ((IO.Readable.Buffered)io).readNextBuffer();
		if (buf != null)
			position.add(buf.remaining());
		return buf;
	}
	
	protected AsyncSupplier<Integer, IOException> readFullySyncIfPossible(
		ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
	) {
		AsyncSupplier<Integer, IOException> res = new AsyncSupplier<>();
		((IO.Readable.Buffered)io).readFullySyncIfPossible(buffer, r -> {
			if (r.getValue1() != null)
				position.add(r.getValue1().intValue());
			if (ondone == null) return;
			ondone.accept(r);
		}).onDone(res::unblockSuccess, res);
		return res;
	}
	
}
