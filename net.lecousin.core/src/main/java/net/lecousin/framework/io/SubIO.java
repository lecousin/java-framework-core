package net.lecousin.framework.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Sub-part of an IO.
 */
public abstract class SubIO extends IO.AbstractIO {
	
	/**
	 * Sub-part of a Readable IO.
	 */
	public static class Readable extends SubIO implements IO.Readable, IO.KnownSize {
		
		/** Constructor. */
		public Readable(IO.Readable src, long size, String description, boolean closeSrcOnClose) {
			io = src;
			pos = 0;
			this.size = size;
			this.description = description;
			closeContainer = closeSrcOnClose;
		}
		
		protected IO.Readable io;
		protected long pos;
		protected long size;
		protected String description;
		protected boolean closeContainer;
		
		@Override
		public IO getWrappedIO() { return null; }
		
		@Override
		public String getSourceDescription() { return description; }
		
		@Override
		public TaskManager getTaskManager() { return io.getTaskManager(); }
		
		@Override
		public byte getPriority() { return io.getPriority(); }
		
		@Override
		public void setPriority(byte priority) { io.setPriority(priority); }
		
		@Override
		protected ISynchronizationPoint<IOException> closeIO() {
			if (closeContainer)
				return io.closeAsync();
			return new SynchronizationPoint<>(true);
		}
		
		@Override
		public ISynchronizationPoint<IOException> canStartReading() {
			return io.canStartReading();
		}
		
		@Override
		public long getSizeSync() {
			return size;
		}
		
		@Override
		public AsyncWork<Long, IOException> getSizeAsync() {
			return new AsyncWork<Long, IOException>(Long.valueOf(size), null);
		}
		
		@Override
		public int readSync(ByteBuffer buffer) throws IOException {
			int limit = -1;
			if (pos + buffer.remaining() > size) {
				limit = buffer.limit();
				buffer.limit((int)(buffer.position() + size - pos));
			}
			int nb = io.readSync(buffer);
			pos += nb;
			if (limit != -1) buffer.limit(limit);
			return nb;
		}
		
		@Override
		public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			int limit = -1;
			if (pos + buffer.remaining() > size) {
				limit = buffer.limit();
				buffer.limit((int)(buffer.position() + size - pos));
			}
			int plimit = limit;
			return io.readAsync(buffer, (result) -> {
				if (result.getValue1() != null)
					pos += result.getValue1().intValue();
				if (plimit != -1)
					buffer.limit(plimit);
				if (ondone != null) ondone.run(result);
			});
		}
		
		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			int limit = -1;
			if (pos + buffer.remaining() > size) {
				limit = buffer.limit();
				buffer.limit((int)(buffer.position() + size - pos));
			}
			int nb = io.readFullySync(buffer);
			pos += nb;
			if (limit != -1) buffer.limit(limit);
			return nb;
		}
		
		@Override
		public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			int limit = -1;
			if (pos + buffer.remaining() > size) {
				limit = buffer.limit();
				buffer.limit((int)(buffer.position() + size - pos));
			}
			int plimit = limit;
			return io.readAsync(buffer, (result) -> {
				if (result.getValue1() != null)
					pos += result.getValue1().intValue();
				if (plimit != -1)
					buffer.limit(plimit);
				if (ondone != null) ondone.run(result);
			});
		}
		
		@Override
		public long skipSync(long n) throws IOException {
			if (n <= 0) return 0;
			if (pos + n < 0) n = -pos;
			if (pos + n > size) n = size - pos;
			long nb = io.skipSync(n);
			pos += nb;
			return nb;
		}
		
		@Override
		public AsyncWork<Long,IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
			if (n <= 0) {
				if (ondone != null) ondone.run(new Pair<>(Long.valueOf(0), null));
				return new AsyncWork<>(Long.valueOf(0), null);
			}
			if (pos + n < 0) n = -pos;
			if (pos + n > size) n = size - pos;
			return io.skipAsync(n, (result) -> {
				if (result.getValue1() != null)
					pos += result.getValue1().longValue();
				if (ondone != null) ondone.run(result);
			});
		}
		
		
		/**
		 * Sub-part of a Seekable Readable IO.
		 */
		public static class Seekable extends SubIO.Readable implements IO.Readable.Seekable {
			
			/** Constructor. */
			public Seekable(IO.Readable.Seekable src, long start, long size, String description, boolean closeSrcOnClose) {
				super(src, size, description, closeSrcOnClose);
				this.start = start;
				this.io = src;
			}
			
			protected long start;
			protected IO.Readable.Seekable io;
			
			@Override
			public long getPosition() { return pos; }
			
			@Override
			public int readSync(long pos, ByteBuffer buffer) throws IOException {
				if (pos >= size) return -1;
				int limit = -1;
				if (pos + buffer.remaining() > size) {
					limit = buffer.limit();
					buffer.limit((int)(buffer.position() + size - pos));
				}
				int nb = io.readSync(start + pos, buffer);
				this.pos = pos + nb;
				if (limit != -1) buffer.limit(limit);
				return nb;
			}
			
			@Override
			public int readSync(ByteBuffer buffer) throws IOException {
				return readSync(pos, buffer);
			}
			
			@Override
			public AsyncWork<Integer,IOException> readAsync(
				long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
			) {
				if (pos > size) {
					if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
					return new AsyncWork<>(Integer.valueOf(-1),null);
				}
				int limit = -1;
				if (pos + buffer.remaining() > size) {
					limit = buffer.limit();
					buffer.limit((int)(buffer.position() + size - pos));
				}
				int plimit = limit;
				return io.readAsync(start + pos, buffer, (result) -> {
					if (result.getValue1() != null)
						SubIO.Readable.Seekable.this.pos = pos + result.getValue1().intValue();
					if (plimit != -1)
						buffer.limit(plimit);
					if (ondone != null) ondone.run(result);
				});
			}
			
			@Override
			public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
				return readAsync(pos, buffer, ondone);
			}
			
			@Override
			public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
				if (pos > size) return -1;
				int limit = -1;
				if (pos + buffer.remaining() > size) {
					limit = buffer.limit();
					buffer.limit((int)(buffer.position() + size - pos));
				}
				int nb = io.readFullySync(start + pos, buffer);
				this.pos = pos + nb;
				if (limit != -1) buffer.limit(limit);
				return nb;
			}
			
			@Override
			public int readFullySync(ByteBuffer buffer) throws IOException {
				return readFullySync(pos, buffer);
			}
			
			@Override
			public AsyncWork<Integer,IOException> readFullyAsync(
				long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
			) {
				if (pos > size) return new AsyncWork<>(Integer.valueOf(-1),null);
				int limit = -1;
				if (pos + buffer.remaining() > size) {
					limit = buffer.limit();
					buffer.limit((int)(buffer.position() + size - pos));
				}
				int plimit = limit;
				return io.readFullyAsync(start + pos, buffer, (result) -> {
					if (result.getValue1() != null)
						SubIO.Readable.Seekable.this.pos = pos + result.getValue1().intValue();
					if (plimit != -1)
						buffer.limit(plimit);
					if (ondone != null) ondone.run(result);
				});
			}
			
			@Override
			public AsyncWork<Integer,IOException> readFullyAsync(
				ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
			) {
				return readFullyAsync(pos, buffer, ondone);
			}
			
			@Override
			public long seekSync(SeekType type, long move) {
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
			
			@Override
			public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
				return IOUtil.seekAsyncUsingSync(this, type, move, ondone).getOutput();
			}
			
			@Override
			public long skipSync(long n) {
				if (pos + n < 0) n = -pos;
				if (pos + n > size) n = size - pos;
				pos += n;
				return n;
			}
			
			@Override
			public AsyncWork<Long,IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
				long l = skipSync(n);
				if (ondone != null) ondone.run(new Pair<>(Long.valueOf(l), null));
				return new AsyncWork<Long,IOException>(Long.valueOf(l), null);
			}
			
			/** Sub-part of a Buffered Seekable Readable IO. */
			public static class Buffered extends SubIO.Readable.Seekable implements IO.Readable.Buffered {
				
				/** Constructor. */
				public <T extends IO.Readable.Seekable & IO.Readable.Buffered> 
				Buffered(T src, long start, long size, String description, boolean closeSrcOnClose) {
					super(src, start, size, description, closeSrcOnClose);
				}

				@Override
				public ISynchronizationPoint<IOException> canStartReading() {
					ISynchronizationPoint<IOException> sp = ((IO.Readable.Buffered)io).canStartReading();
					if (!sp.isUnblocked()) return sp;
					try {
						if (io.getPosition() == start + pos) return sp;
					} catch (IOException e) {
						return new SynchronizationPoint<>(e);
					}
					AsyncWork<Long, IOException> seek = io.seekAsync(SeekType.FROM_BEGINNING, start + pos);
					return seek;
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
					int nb = readSync(b);
					return nb;
				}

				@Override
				public int readFully(byte[] buffer) throws IOException {
					int len = buffer.length;
					if (pos + len > size)
						len = (int)(size - pos);
					ByteBuffer b = ByteBuffer.wrap(buffer, 0, len);
					int nb = readFullySync(b);
					return nb;
				}
				
				@Override
				public int readAsync() throws IOException {
					if (pos == size) return -1;
					if (io.getPosition() != start + pos) {
						AsyncWork<Long, IOException> seek = io.seekAsync(SeekType.FROM_BEGINNING, start + pos);
						if (!seek.isUnblocked())
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
				public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(
					RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone
				) {
					if (pos == size) {
						if (ondone != null) ondone.run(new Pair<>(null, null));
						return new AsyncWork<>(null, null);
					}
					AsyncWork<ByteBuffer, IOException> result = new AsyncWork<>();
					Task.Cpu<Void, NoException> task = new Task.Cpu<Void, NoException>("Read next buffer", getPriority()) {
						@Override
						public Void run() {
							int len = 16384;
							if (len > size - pos) len = (int)(size - pos);
							ByteBuffer buf = ByteBuffer.allocate(len);
							AsyncWork<Integer, IOException> read = io.readAsync(start + pos, buf);
							read.listenInline(() -> {
								if (read.hasError()) {
									if (ondone != null) ondone.run(new Pair<>(null, read.getError()));
									result.error(read.getError());
									return;
								}
								if (read.getResult().intValue() > 0)
									pos += read.getResult().intValue();
								buf.flip();
								if (ondone != null) ondone.run(new Pair<>(buf, null));
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

}
