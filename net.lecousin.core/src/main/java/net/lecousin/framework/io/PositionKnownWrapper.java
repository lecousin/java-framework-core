package net.lecousin.framework.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Add the capability to known on which position we are on an IO.
 * @param <IOType> type of IO
 */
public abstract class PositionKnownWrapper<IOType extends IO> extends IO.AbstractIO implements IO.PositionKnown {

	/** Constructor with initial position. */
	public PositionKnownWrapper(IOType io, long position) {
		this.io = io;
		this.position = position;
	}
	
	protected IOType io;
	protected long position;
	
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
		public ISynchronizationPoint<IOException> canStartReading() {
			return super.canStartReading();
		}
		
		@Override
		public int readSync(ByteBuffer buffer) throws IOException {
			return super.readSync(buffer);
		}

		@Override
		public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
			return super.readAsync(buffer, ondone);
		}
		
		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			return super.readFullySync(buffer);
		}
		
		@Override
		public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
			return super.readFullyAsync(buffer, ondone);
		}
		
		@Override
		public long skipSync(long n) throws IOException {
			return super.skipSync(n);
		}

		@Override
		public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long, IOException>> ondone) {
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
			public ISynchronizationPoint<IOException> canStartReading() {
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
			public AsyncWork<Integer, IOException> readAsync(
				ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone
			) {
				return super.readAsync(buffer, ondone);
			}
			
			@Override
			public int readFullySync(ByteBuffer buffer) throws IOException {
				return super.readFullySync(buffer);
			}
			
			@Override
			public AsyncWork<Integer, IOException> readFullyAsync(
				ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone
			) {
				return super.readFullyAsync(buffer, ondone);
			}
			
			@Override
			public long skipSync(long n) throws IOException {
				return super.skipSync(n);
			}

			@Override
			public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long, IOException>> ondone) {
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
			public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
				return super.readNextBufferAsync(ondone);
			}

		}

	}
	
	// --- Common ---
	
	@Override
	public String getSourceDescription() { return io.getSourceDescription(); }
	
	@Override
	public IO getWrappedIO() { return io; }
	
	@Override
	public byte getPriority() { return io.getPriority(); }
	
	@Override
	public void setPriority(byte priority) { io.setPriority(priority); }
	
	@Override
	public TaskManager getTaskManager() { return io.getTaskManager(); }
	
	@Override
	public long getPosition() { return position; }
	
	@Override
	protected ISynchronizationPoint<IOException> closeIO() { return io.closeAsync(); }

	// --- IO.Readable ---
	
	protected ISynchronizationPoint<IOException> canStartReading() {
		return ((IO.Readable)io).canStartReading();
	}

	protected int readSync(ByteBuffer buffer) throws IOException {
		int nb = ((IO.Readable)io).readSync(buffer);
		if (nb > 0)
			position += nb;
		return nb;
	}

	protected int readAsync() throws IOException {
		int c = ((IO.Readable.Buffered)io).readAsync();
		if (c >= 0) position++;
		return c;
	}
	
	protected AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
		return ((IO.Readable)io).readAsync(buffer, (result) -> {
			Integer nb = result.getValue1();
			if (nb != null && nb.intValue() > 0)
				position += nb.intValue();
			if (ondone != null)
				ondone.run(result);
		});
	}
	
	protected int readFullySync(ByteBuffer buffer) throws IOException {
		int nb = ((IO.Readable)io).readFullySync(buffer);
		if (nb > 0)
			position += nb;
		return nb;
	}
	
	protected AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
		return ((IO.Readable)io).readFullyAsync(buffer, (result) -> {
			Integer nb = result.getValue1();
			if (nb != null && nb.intValue() > 0)
				position += nb.intValue();
			if (ondone != null)
				ondone.run(result);
		});
	}
	
	protected long skipSync(long n) throws IOException {
		long skipped = ((IO.Readable)io).skipSync(n);
		position += skipped;
		return skipped;
	}

	protected AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long, IOException>> ondone) {
		return ((IO.Readable)io).skipAsync(n, (result) -> {
			Long nb = result.getValue1();
			if (nb != null)
				position += nb.longValue();
			if (ondone != null)
				ondone.run(result);
		});
	}

	// --- IO.Readable.Buffered
	
	protected int read() throws IOException {
		int c = ((IO.Readable.Buffered)io).read();
		if (c >= 0) position++;
		return c;
	}

	protected int read(byte[] buffer, int offset, int len) throws IOException {
		int nb = ((IO.Readable.Buffered)io).read(buffer, offset, len);
		if (nb > 0)
			position += nb;
		return nb;
	}

	protected int readFully(byte[] buffer) throws IOException {
		int nb = ((IO.Readable.Buffered)io).readFully(buffer);
		if (nb > 0)
			position += nb;
		return nb;
	}

	protected int skip(int skip) throws IOException {
		int skipped = ((IO.Readable.Buffered)io).skip(skip);
		position += skipped;
		return skipped;
	}

	protected AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		return ((IO.Readable.Buffered)io).readNextBufferAsync((result) -> {
			ByteBuffer buf = result.getValue1();
			if (buf != null)
				position += buf.remaining();
			if (ondone != null)
				ondone.run(result);
		});
	}
	
}
