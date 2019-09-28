package net.lecousin.framework.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

// skip checkstyle: OverloadMethodsDeclarationOrder
/**
 * Sub-part of an IO.
 */
public abstract class SubIO extends ConcurrentCloseable<IOException> implements IO, IO.KnownSize, IO.PositionKnown {

	private SubIO(IO src, long start, long size, String description, boolean closeSrcOnClose) {
		io = src;
		pos = 0;
		this.start = start;
		this.size = size;
		this.description = description;
		closeContainer = closeSrcOnClose;
	}

	protected IO io;
	protected long pos;
	protected long start;
	protected long size;
	protected String description;
	protected boolean closeContainer;
	
	/**
	 * Sub-part of a Readable IO.
	 */
	public static class Readable extends SubIO implements IO.Readable {
		
		/** Constructor. */
		public Readable(IO.Readable src, long size, String description, boolean closeSrcOnClose) {
			super(src, 0, size, description, closeSrcOnClose);
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
		public AsyncSupplier<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readAsync(buffer, ondone);
		}
		
		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			return super.readFullySync(buffer);
		}
		
		@Override
		public AsyncSupplier<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readFullyAsync(buffer, ondone);
		}
		
		@Override
		public long skipSync(long n) throws IOException {
			if (n <= 0) return 0;
			if (pos + n < 0) n = -pos;
			if (pos + n > size) n = size - pos;
			long nb = ((IO.Readable)io).skipSync(n);
			pos += nb;
			return nb;
		}
		
		@Override
		public AsyncSupplier<Long,IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
			if (n <= 0) {
				if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(0), null));
				return new AsyncSupplier<>(Long.valueOf(0), null);
			}
			if (pos + n < 0) n = -pos;
			if (pos + n > size) n = size - pos;
			return ((IO.Readable)io).skipAsync(n, result -> {
				if (result.getValue1() != null)
					pos += result.getValue1().longValue();
				if (ondone != null) ondone.accept(result);
			});
		}
		
		
		/**
		 * Sub-part of a Seekable Readable IO.
		 */
		public static class Seekable extends SubIO implements IO.Readable.Seekable {
			
			/** Constructor. */
			public Seekable(IO.Readable.Seekable src, long start, long size, String description, boolean closeSrcOnClose) {
				super(src, start, size, description, closeSrcOnClose);
			}
			
			@Override
			public IAsync<IOException> canStartReading() {
				return super.canStartReading();
			}
			
			@Override
			public int readSync(long pos, ByteBuffer buffer) throws IOException {
				return super.readSync(pos, buffer);
			}
			
			@Override
			public int readSync(ByteBuffer buffer) throws IOException {
				int nb = super.readSync(pos, buffer);
				if (nb > 0) pos += nb;
				return nb;
			}
			
			@Override
			public AsyncSupplier<Integer,IOException> readAsync(
				long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
			) {
				return super.readAsync(pos, buffer, ondone);
			}
			
			@Override
			public AsyncSupplier<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
				return super.readAsync(pos, buffer, res -> {
					if (res.getValue1() != null && res.getValue1().intValue() > 0)
						pos += res.getValue1().intValue();
					if (ondone != null) ondone.accept(res);
				});
			}
			
			@Override
			public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
				return super.readFullySync(pos, buffer);
			}
			
			@Override
			public int readFullySync(ByteBuffer buffer) throws IOException {
				int nb = super.readFullySync(pos, buffer);
				if (nb > 0) pos += nb;
				return nb;
			}
			
			@Override
			public AsyncSupplier<Integer,IOException> readFullyAsync(
				long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
			) {
				return super.readFullyAsync(pos, buffer, ondone);
			}
			
			@Override
			public AsyncSupplier<Integer,IOException> readFullyAsync(
				ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
			) {
				return super.readFullyAsync(pos, buffer, res -> {
					if (res.getValue1() != null && res.getValue1().intValue() > 0)
						pos += res.getValue1().intValue();
					if (ondone != null) ondone.accept(res);
				});
			}
			
			@Override
			public long seekSync(SeekType type, long move) {
				return super.seekSync(type, move);
			}
			
			@Override
			public AsyncSupplier<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
				return super.seekAsync(type, move, ondone);
			}
			
			@Override
			public long skipSync(long n) {
				if (pos + n < 0) n = -pos;
				if (pos + n > size) n = size - pos;
				pos += n;
				return n;
			}
			
			@Override
			public AsyncSupplier<Long,IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
				long l = skipSync(n);
				if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(l), null));
				return new AsyncSupplier<>(Long.valueOf(l), null);
			}
			
			/** Sub-part of a Buffered Seekable Readable IO. */
			public static class Buffered extends SubIO.Readable.Seekable implements IO.Readable.Buffered {
				
				/** Constructor. */
				public <T extends IO.Readable.Seekable & IO.Readable.Buffered> 
				Buffered(T src, long start, long size, String description, boolean closeSrcOnClose) {
					super(src, start, size, description, closeSrcOnClose);
				}

				@Override
				public IAsync<IOException> canStartReading() {
					IAsync<IOException> sp = ((IO.Readable.Buffered)io).canStartReading();
					if (!sp.isDone()) return sp;
					try {
						if (((IO.Readable.Seekable)io).getPosition() == start + pos) return sp;
					} catch (IOException e) {
						return new Async<>(e);
					}
					return ((IO.Readable.Seekable)io).seekAsync(SeekType.FROM_BEGINNING, start + pos);
				}
				
				@Override
				public int read() throws IOException {
					if (pos == size) return -1;
					ByteBuffer b = ByteBuffer.allocate(1);
					int nb = readSync(b);
					if (nb <= 0) return -1;
					return (b.array()[0] & 0xFF);
				}

				@Override
				public int read(byte[] buffer, int offset, int len) throws IOException {
					if (pos + len > size)
						len = (int)(size - pos);
					ByteBuffer b = ByteBuffer.wrap(buffer, offset, len);
					return readSync(b);
				}

				@Override
				public int readFully(byte[] buffer) throws IOException {
					int len = buffer.length;
					if (pos + len > size)
						len = (int)(size - pos);
					ByteBuffer b = ByteBuffer.wrap(buffer, 0, len);
					return readFullySync(b);
				}
				
				@Override
				public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(
					ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
				) {
					try {
						if (((IO.Readable.Seekable)io).getPosition() != start + pos)
							return readFullyAsync(buffer, ondone);
					} catch (IOException e) {
						if (ondone != null) ondone.accept(new Pair<>(null, e));
						return new AsyncSupplier<>(null, e);
					}
					int limit;
					if (buffer.remaining() > size - pos) {
						limit = buffer.limit();
						buffer.limit((int)(buffer.position() + size - pos));
					} else {
						limit = -1;
					}
					return ((IO.Readable.Buffered)io).readFullySyncIfPossible(buffer, res -> {
						if (res.getValue1() != null && res.getValue1().intValue() > 0)
							pos += res.getValue1().intValue();
						if (limit != -1)
							buffer.limit(limit);
						if (ondone != null) ondone.accept(res);
					});
				}
				
				@Override
				public int readAsync() throws IOException {
					if (pos == size) return -1;
					if (((IO.Readable.Seekable)io).getPosition() != start + pos) {
						AsyncSupplier<Long, IOException> seek =
							((IO.Readable.Seekable)io).seekAsync(SeekType.FROM_BEGINNING, start + pos);
						if (!seek.isDone())
							return -2;
						if (seek.hasError()) throw seek.getError();
					}
					int res = ((IO.Readable.Buffered)io).readAsync();
					if (res >= 0)
						pos++;
					return res;
				}

				@Override
				public int skip(int skip) {
					return (int)skipSync(skip);
				}

				@Override
				public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(
					Consumer<Pair<ByteBuffer, IOException>> ondone
				) {
					if (pos == size) {
						if (ondone != null) ondone.accept(new Pair<>(null, null));
						return new AsyncSupplier<>(null, null);
					}
					AsyncSupplier<ByteBuffer, IOException> result = new AsyncSupplier<>();
					Task.Cpu<Void, NoException> task = new Task.Cpu<Void, NoException>("Read next buffer", getPriority()) {
						@Override
						public Void run() {
							int len = 16384;
							if (len > size - pos) len = (int)(size - pos);
							ByteBuffer buf = ByteBuffer.allocate(len);
							AsyncSupplier<Integer, IOException> read =
								((IO.Readable.Seekable)io).readAsync(start + pos, buf);
							read.onDone(() -> {
								if (read.hasError()) {
									if (ondone != null) ondone.accept(new Pair<>(null, read.getError()));
									result.error(read.getError());
									return;
								}
								if (read.getResult().intValue() > 0)
									pos += read.getResult().intValue();
								buf.flip();
								if (ondone != null) ondone.accept(new Pair<>(buf, null));
								result.unblockSuccess(buf);
							});
							return null;
						}
					};
					task.start();
					return result;
				}
			}
		}
		
	}

	/**
	 * Sub-part of a Writable IO.
	 */
	public static class Writable extends SubIO implements IO.Writable {


		/** Constructor. */
		public Writable(IO.Writable src, long start, long size, String description, boolean closeSrcOnClose) {
			super(src, start, size, description, closeSrcOnClose);
		}
		
		@Override
		public IAsync<IOException> canStartWriting() {
			return super.canStartWriting();
		}
		
		@Override
		public int writeSync(ByteBuffer buffer) throws IOException {
			return super.writeSync(buffer);
		}
		
		@Override
		public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return super.writeAsync(buffer, ondone);
		}
		
		/**
		 * Sub-part of a Seekable Writable IO.
		 */
		public static class Seekable extends SubIO implements IO.Writable.Seekable {

			/** Constructor. */
			public Seekable(IO.Writable.Seekable src, long start, long size, String description, boolean closeSrcOnClose) {
				super(src, start, size, description, closeSrcOnClose);
			}
			
			@Override
			public IAsync<IOException> canStartWriting() {
				return super.canStartWriting();
			}
			
			@Override
			public int writeSync(long pos, ByteBuffer buffer) throws IOException {
				return super.writeSync(pos, buffer);
			}
			
			@Override
			public int writeSync(ByteBuffer buffer) throws IOException {
				int nb = super.writeSync(pos, buffer);
				if (nb > 0) pos += nb;
				return nb;
			}
			
			@Override
			public AsyncSupplier<Integer, IOException> writeAsync(
				long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
			) {
				return super.writeAsync(pos, buffer, ondone);
			}
			
			@Override
			public AsyncSupplier<Integer, IOException> writeAsync(
				ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
			) {
				return super.writeAsync(pos, buffer, res -> {
					if (res.getValue1() != null && res.getValue1().intValue() > 0)
						pos += res.getValue1().intValue();
					if (ondone != null) ondone.accept(res);
				});
			}
			
			@Override
			public long seekSync(SeekType type, long move) {
				return super.seekSync(type, move);
			}
			
			@Override
			public AsyncSupplier<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
				return super.seekAsync(type, move, ondone);
			}
		}
		
	}
	
	/**
	 * Sub-part of a Seekable Readable and Writable IO.
	 */
	public static class ReadWrite extends SubIO implements IO.Readable.Seekable, IO.Writable.Seekable {

		/** Constructor. */
		public <T extends IO.Readable.Seekable & IO.Writable.Seekable> ReadWrite(
				T src, long start, long size, String description, boolean closeSrcOnClose
		) {
			super(src, start, size, description, closeSrcOnClose);
		}

		@Override
		public IAsync<IOException> canStartReading() {
			return super.canStartReading();
		}
		
		@Override
		public int readSync(long pos, ByteBuffer buffer) throws IOException {
			return super.readSync(pos, buffer);
		}
		
		@Override
		public int readSync(ByteBuffer buffer) throws IOException {
			int nb = super.readSync(pos, buffer);
			if (nb > 0) pos += nb;
			return nb;
		}
		
		@Override
		public AsyncSupplier<Integer,IOException> readAsync(
			long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
		) {
			return super.readAsync(pos, buffer, ondone);
		}
		
		@Override
		public AsyncSupplier<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readAsync(pos, buffer, res -> {
				if (res.getValue1() != null && res.getValue1().intValue() > 0)
					pos += res.getValue1().intValue();
				if (ondone != null) ondone.accept(res);
			});
		}
		
		@Override
		public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
			return super.readFullySync(pos, buffer);
		}
		
		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			int nb = super.readFullySync(pos, buffer);
			if (nb > 0) pos += nb;
			return nb;
		}
		
		@Override
		public AsyncSupplier<Integer,IOException> readFullyAsync(
			long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
		) {
			return super.readFullyAsync(pos, buffer, ondone);
		}
		
		@Override
		public AsyncSupplier<Integer,IOException> readFullyAsync(
			ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
		) {
			return super.readFullyAsync(pos, buffer, res -> {
				if (res.getValue1() != null && res.getValue1().intValue() > 0)
					pos += res.getValue1().intValue();
				if (ondone != null) ondone.accept(res);
			});
		}
		
		@Override
		public long seekSync(SeekType type, long move) {
			return super.seekSync(type, move);
		}
		
		@Override
		public AsyncSupplier<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
			return super.seekAsync(type, move, ondone);
		}
		
		@Override
		public long skipSync(long n) {
			if (pos + n < 0) n = -pos;
			if (pos + n > size) n = size - pos;
			pos += n;
			return n;
		}
		
		@Override
		public AsyncSupplier<Long,IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
			long l = skipSync(n);
			if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(l), null));
			return new AsyncSupplier<>(Long.valueOf(l), null);
		}

		@Override
		public IAsync<IOException> canStartWriting() {
			return super.canStartWriting();
		}
		
		@Override
		public int writeSync(long pos, ByteBuffer buffer) throws IOException {
			return super.writeSync(pos, buffer);
		}
		
		@Override
		public int writeSync(ByteBuffer buffer) throws IOException {
			int nb = super.writeSync(pos, buffer);
			if (nb > 0) pos += nb;
			return nb;
		}
		
		@Override
		public AsyncSupplier<Integer, IOException> writeAsync(
			long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
		) {
			return super.writeAsync(pos, buffer, ondone);
		}
		
		@Override
		public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return super.writeAsync(pos, buffer, res -> {
				if (res.getValue1() != null && res.getValue1().intValue() > 0)
					pos += res.getValue1().intValue();
				if (ondone != null) ondone.accept(res);
			});
		}
	}

	@Override
	public IO getWrappedIO() { return null; }
	
	@Override
	public String getSourceDescription() { return description; }
	
	@Override
	public TaskManager getTaskManager() { return io.getTaskManager(); }
	
	@Override
	public byte getPriority() { return io != null ? io.getPriority() : Task.PRIORITY_NORMAL; }
	
	@Override
	public void setPriority(byte priority) { if (io != null) io.setPriority(priority); }
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		if (!closeContainer) return null;
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		io = null;
		ondone.unblock();
	}
	
	@Override
	public long getSizeSync() {
		return size;
	}
	
	@Override
	public AsyncSupplier<Long, IOException> getSizeAsync() {
		return new AsyncSupplier<>(Long.valueOf(size), null);
	}
	
	@Override
	public long getPosition() { return pos; }

	
	// ************************************
	// IO.Readable
	// ************************************

	
	protected IAsync<IOException> canStartReading() {
		return ((IO.Readable)io).canStartReading();
	}
	
	protected int readSync(ByteBuffer buffer) throws IOException {
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int nb = ((IO.Readable)io).readSync(buffer);
		pos += nb;
		if (limit != -1) buffer.limit(limit);
		return nb;
	}
	
	protected AsyncSupplier<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int plimit = limit;
		return ((IO.Readable)io).readAsync(buffer, result -> {
			if (result.getValue1() != null)
				pos += result.getValue1().intValue();
			if (plimit != -1)
				buffer.limit(plimit);
			if (ondone != null) ondone.accept(result);
		});
	}
	
	protected int readFullySync(ByteBuffer buffer) throws IOException {
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int nb = ((IO.Readable)io).readFullySync(buffer);
		pos += nb;
		if (limit != -1) buffer.limit(limit);
		return nb;
	}
	
	protected AsyncSupplier<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int plimit = limit;
		return ((IO.Readable)io).readFullyAsync(buffer, result -> {
			if (result.getValue1() != null)
				pos += result.getValue1().intValue();
			if (plimit != -1)
				buffer.limit(plimit);
			if (ondone != null) ondone.accept(result);
		});
	}
	
	
	// ************************************
	// IO.Readable.Seekable
	// ************************************

	
	protected int readSync(long pos, ByteBuffer buffer) throws IOException {
		if (pos >= size) return -1;
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int nb = ((IO.Readable.Seekable)io).readSync(start + pos, buffer);
		if (limit != -1) buffer.limit(limit);
		return nb;
	}
	
	protected AsyncSupplier<Integer,IOException> readAsync(
		long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		if (pos > size) {
			if (ondone != null) ondone.accept(new Pair<>(Integer.valueOf(-1), null));
			return new AsyncSupplier<>(Integer.valueOf(-1),null);
		}
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int plimit = limit;
		return ((IO.Readable.Seekable)io).readAsync(start + pos, buffer, result -> {
			if (plimit != -1)
				buffer.limit(plimit);
			if (ondone != null) ondone.accept(result);
		});
	}
	
	protected int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		if (pos > size) return -1;
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int nb = ((IO.Readable.Seekable)io).readFullySync(start + pos, buffer);
		if (limit != -1) buffer.limit(limit);
		return nb;
	}
	
	protected AsyncSupplier<Integer,IOException> readFullyAsync(
		long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		if (pos > size) return new AsyncSupplier<>(Integer.valueOf(-1),null);
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int plimit = limit;
		return ((IO.Readable.Seekable)io).readFullyAsync(start + pos, buffer, result -> {
			if (plimit != -1)
				buffer.limit(plimit);
			if (ondone != null) ondone.accept(result);
		});
	}
	
	protected long seekSync(SeekType type, long move) {
		switch (type) {
		default:
		case FROM_BEGINNING:
			if (move < 0) move = 0;
			if (move > size) move = size;
			pos = move;
			return pos;
		case FROM_END:
			if (move < 0) move = 0;
			if (size - move < 0) move = size;
			pos = size - move;
			return pos;
		case FROM_CURRENT:
			if (pos + move < 0) move = -pos;
			if (pos + move > size) move = size - pos;
			pos += move;
			return pos;
		}
	}
	
	protected AsyncSupplier<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		return IOUtil.seekAsyncUsingSync((IO.Seekable)this, type, move, ondone).getOutput();
	}

	
	// ************************************
	// IO.Writable
	// ************************************
	

	protected IAsync<IOException> canStartWriting() {
		return ((IO.Writable)io).canStartWriting();
	}

	protected int writeSync(ByteBuffer buffer) throws IOException {
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int nb = ((IO.Writable)io).writeSync(buffer);
		pos += nb;
		if (limit != -1) buffer.limit(limit);
		return nb;
	}
	
	protected AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int plimit = limit;
		return ((IO.Writable)io).writeAsync(buffer, result -> {
			if (result.getValue1() != null)
				pos += result.getValue1().intValue();
			if (plimit != -1)
				buffer.limit(plimit);
			if (ondone != null) ondone.accept(result);
		});
	}

	
	// ************************************
	// IO.Writable.Seekable
	// ************************************
	
	protected int writeSync(long pos, ByteBuffer buffer) throws IOException {
		if (pos >= size) return -1;
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int nb = ((IO.Writable.Seekable)io).writeSync(start + pos, buffer);
		if (limit != -1) buffer.limit(limit);
		return nb;
	}
	
	protected AsyncSupplier<Integer, IOException> writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		if (pos > size) {
			if (ondone != null) ondone.accept(new Pair<>(Integer.valueOf(-1), null));
			return new AsyncSupplier<>(Integer.valueOf(-1),null);
		}
		int limit = -1;
		if (pos + buffer.remaining() > size) {
			limit = buffer.limit();
			buffer.limit((int)(buffer.position() + size - pos));
		}
		int plimit = limit;
		return ((IO.Writable.Seekable)io).writeAsync(start + pos, buffer, result -> {
			if (plimit != -1)
				buffer.limit(plimit);
			if (ondone != null) ondone.accept(result);
		});
	}
	
}
