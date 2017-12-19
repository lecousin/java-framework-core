package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Read an IO.Readable into 2 buffers, then those buffers can be read when ready.
 */
public class TwoBuffersIO extends ConcurrentCloseable implements IO.Readable.Buffered, IO.Readable.Seekable, IO.KnownSize {

	/** Constructor. */
	public TwoBuffersIO(IO.Readable io, int firstBuffer, int secondBuffer) {
		this.io = io;
		buf1 = new byte[firstBuffer];
		read1 = io.readFullyAsync(ByteBuffer.wrap(buf1));
		if (secondBuffer > 0) {
			buf2 = new byte[secondBuffer];
			read1.listenInline(new Runnable() {
				@Override
				public void run() {
					if (read1.isSuccessful()) {
						int nb = read1.getResult().intValue();
						nb1 = nb > 0 ? nb : 0;
						if (nb < buf1.length) {
							buf2 = null;
							nb2 = 0;
						} else
							read2 = io.readFullyAsync(ByteBuffer.wrap(buf2));
						synchronized (buf1) { buf1.notifyAll(); }
					}
				}
			});
		}
	}
	
	/** Add DeterminedSize capability. */
	public static class DeterminedSize extends TwoBuffersIO implements IO.KnownSize {
		/** Constructor. */
		public DeterminedSize(IO.Readable io, int firstBuffer, int secondBuffer) {
			super(io, firstBuffer, secondBuffer);
		}
		
		@Override
		public AsyncWork<Long, IOException> skipAsync(long n) {
			return new AsyncWork<>(Long.valueOf(skip((int)n)),null);
		}

		@Override
		public int skip(int skip) {
			if (skip == 0) return 0;
			if (skip < 0) {
				if (-skip > pos) skip = -pos;
				pos += skip;
				return skip;
			}
			int max = buf1.length + (buf2 == null ? 0 : buf2.length);
			if (pos + skip > max) skip = max - pos;
			pos += skip;
			return skip;
		}

		@Override
		public long getSizeSync() {
			return buf1.length + (buf2 == null ? 0 : buf2.length);
		}

		@Override
		public AsyncWork<Long, IOException> getSizeAsync() {
			return new AsyncWork<>(Long.valueOf(buf1.length + (buf2 == null ? 0 : buf2.length)),null);
		}

		@Override
		public AsyncWork<Long, IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
			// seek does not need any task, we can do it sync
			try {
				Long r = Long.valueOf(seekSync(type, move));
				if (ondone != null) ondone.run(new Pair<>(r, null));
				return new AsyncWork<>(r, null);
			} catch (IOException e) {
				if (ondone != null) ondone.run(new Pair<>(null, e));
				return new AsyncWork<>(null,e);
			}
		}

	}

	private IO.Readable io;
	protected byte[] buf1;
	protected byte[] buf2;
	private int nb1 = -1;
	private int nb2 = -1;
	private AsyncWork<Integer,IOException> read1;
	private AsyncWork<Integer,IOException>  read2;
	protected int pos = 0;

	@Override
	protected ISynchronizationPoint<IOException> closeIO() {
		return io.closeAsync();
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		if (nb1 < 0 || pos < nb1 || buf2 == null)
			return read1;
		if (read2 == null) {
			synchronized (buf1) {
				while (read2 == null) {
					try { buf1.wait(); }
					catch (InterruptedException e) { /* ignore */ }
				}
			}
		}
		return read2;
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
	public byte getPriority() {
		return io.getPriority();
	}

	@Override
	public void setPriority(byte priority) {
		io.setPriority(priority);
	}

	@Override
	public TaskManager getTaskManager() {
		return Threading.getCPUTaskManager();
	}
	
	@Override
	public long getPosition() {
		return pos;
	}
	
	@Override
	public int read() throws IOException {
		if (nb1 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0)
				return -1;
		}
		if (pos < nb1)
			return buf1[pos++] & 0xFF;
		if (buf2 == null) return -1;
		if (nb2 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			if (read2 == null) {
				synchronized (buf1) {
					while (read2 == null) {
						if (buf2 == null) return -1;
						try { buf1.wait(); }
						catch (InterruptedException e) { /* ignore */ }
					}
				}
			}
			if (!read2.isUnblocked()) read2.block(0);
			if (!read2.isSuccessful()) {
				if (read2.isCancelled()) throw new IOException("Cancelled", read2.getCancelEvent());
				throw read2.getError();
			}
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0)
				return -1;
		}
		if (pos >= nb1 + nb2) return -1;
		return buf2[(pos++) - nb1] & 0xFF;
	}
	
	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		if (nb1 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0)
				return -1;
		}
		if (pos < nb1) {
			int l = nb1 - pos;
			if (l > len) l = len;
			System.arraycopy(buf1, pos, buffer, offset, l);
			pos += l;
			return l;
		}
		if (buf2 == null) return -1;
		if (nb2 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			if (read2 == null) {
				synchronized (buf1) {
					while (read2 == null) {
						if (buf2 == null) return -1;
						try { buf1.wait(); }
						catch (InterruptedException e) { /* ignore */ }
					}
				}
			}
			if (!read2.isUnblocked()) read2.block(0);
			if (!read2.isSuccessful()) {
				if (read2.isCancelled()) throw new IOException("Cancelled", read2.getCancelEvent());
				throw read2.getError();
			}
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0)
				return -1;
		}
		if (pos >= nb1 + nb2) return -1;
		int l = (nb1 + nb2) - pos;
		if (l > len) l = len;
		System.arraycopy(buf2, pos - nb1, buffer, offset, l);
		pos += l;
		return l;
	}

	@Override
	public int readFully(byte[] buffer) throws IOException {
		if (nb1 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0)
				return -1;
		}
		int len = buffer.length;
		int offset = 0;
		int done = 0;
		if (pos < nb1) {
			int l = nb1 - pos;
			if (l > len) l = len;
			System.arraycopy(buf1, pos, buffer, 0, l);
			pos += l;
			if (l == len) return l;
			offset += l;
			len -= l;
			done = l;
		}
		if (buf2 == null) return done > 0 ? done : -1;
		if (nb2 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			if (read2 == null) {
				synchronized (buf1) {
					while (read2 == null) {
						if (buf2 == null) return done > 0 ? done : -1;
						try { buf1.wait(); }
						catch (InterruptedException e) { /* ignore */ }
					}
				}
			}
			if (!read2.isUnblocked()) read2.block(0);
			if (!read2.isSuccessful()) {
				if (read2.isCancelled()) throw new IOException("Cancelled", read2.getCancelEvent());
				throw read2.getError();
			}
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0)
				return done > 0 ? done : -1;
		}
		if (pos >= nb1 + nb2) return done > 0 ? done : -1;
		int l = (nb1 + nb2) - pos;
		if (l > len) l = len;
		System.arraycopy(buf2, pos - nb1, buffer, offset, l);
		pos += l;
		return l + done;
	}

	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		return readSync(pos, buffer);
	}
	
	@Override
	public int readSync(long pos, ByteBuffer buffer) throws IOException {
		if (nb1 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0)
				return -1;
		}
		int len = buffer.remaining();
		if (pos < nb1) {
			int l = nb1 - (int)pos;
			if (l > len) l = len;
			buffer.put(buf1, (int)pos, l);
			this.pos = (int)pos + l;
			return l;
		}
		if (buf2 == null) return -1;
		if (nb2 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			if (read2 == null) {
				synchronized (buf1) {
					while (read2 == null) {
						if (buf2 == null) return -1;
						try { buf1.wait(); }
						catch (InterruptedException e) { /* ignore */ }
					}
				}
			}
			if (!read2.isUnblocked()) read2.block(0);
			if (!read2.isSuccessful()) {
				if (read2.isCancelled()) throw new IOException("Cancelled", read2.getCancelEvent());
				throw read2.getError();
			}
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0)
				return -1;
		}
		if (pos >= nb1 + nb2) return -1;
		int l = (nb1 + nb2) - (int)pos;
		if (l > len) l = len;
		buffer.put(buf2, (int)pos - nb1, l);
		this.pos = (int)pos + l;
		return l;
	}

	@Override
	public int readAsync() throws IOException {
		if (nb1 < 0) {
			if (!read1.isUnblocked()) return -2;
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0)
				return -1;
		}
		if (pos < nb1)
			return buf1[pos++] & 0xFF;
		if (buf2 == null) return -1;
		if (nb2 < 0) {
			if (!read1.isUnblocked()) return -2;
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			if (read2 == null) {
				synchronized (buf1) {
					while (read2 == null) {
						if (buf2 == null) return -1;
						try { buf1.wait(); }
						catch (InterruptedException e) { /* ignore */ }
					}
				}
			}
			if (!read2.isUnblocked()) return -2;
			if (!read2.isSuccessful()) {
				if (read2.isCancelled()) throw new IOException("Cancelled", read2.getCancelEvent());
				throw read2.getError();
			}
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0)
				return -1;
		}
		if (pos >= nb1 + nb2) return -1;
		return buf2[(pos++) - nb1] & 0xFF;
	}
	
	@Override
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return readAsync(pos, buffer, ondone);
	}
	
	@Override
	public AsyncWork<Integer, IOException> readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		if (nb1 < 0) {
			if (!read1.isUnblocked()) {
				AsyncWork<Integer, IOException> sp = new AsyncWork<>();
				read1.listenAsync(new Task.Cpu<Void,NoException>("FullyBufferedIO.readAsync", io.getPriority()) {
					@Override
					public Void run() {
						if (read1.isSuccessful()) {
							try {
								Integer r = Integer.valueOf(readSync(pos, buffer));
								if (ondone != null) ondone.run(new Pair<>(r, null));
								sp.unblockSuccess(r);
							} catch (IOException e) {
								if (ondone != null) ondone.run(new Pair<>(null, e));
								sp.unblockError(e);
							}
						} else if (read1.isCancelled())
							sp.unblockCancel(read1.getCancelEvent());
						else {
							if (ondone != null) ondone.run(new Pair<>(null, read1.getError()));
							sp.unblockError(read1.getError());
						}
						return null;
					}
				}, true);
				return sp;
			}
			if (!read1.isSuccessful()) {
				IOException e;
				if (read1.isCancelled())
					e = new IOException("Cancelled", read1.getCancelEvent());
				else
					e = read1.getError();
				if (ondone != null) ondone.run(new Pair<>(null, e));
				return new AsyncWork<>(null, e);
			}
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0) {
				if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
				return new AsyncWork<>(Integer.valueOf(-1),null);
			}
		}
		int len = buffer.remaining();
		if (pos < nb1) {
			Task<Integer,IOException> task = new Task.Cpu<Integer, IOException>(
				"readAsync on FullyBufferedIO", this.getPriority(), ondone
			) {
				@Override
				public Integer run() {
					int l = nb1 - (int)pos;
					if (l > len) l = len;
					buffer.put(buf1, (int)pos, l);
					TwoBuffersIO.this.pos = (int)pos + l;
					return Integer.valueOf(l);
				}
			};
			task.start();
			return task.getOutput();
		}
		if (buf2 == null) {
			if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
			return new AsyncWork<>(Integer.valueOf(-1), null);
		}
		if (nb2 < 0) {
			if (!read1.isUnblocked() || read2 == null) {
				AsyncWork<Integer, IOException> sp = new AsyncWork<>();
				read1.listenAsync(new Task.Cpu<Void,NoException>("FullyBufferedIO.readAsync", io.getPriority()) {
					@Override
					public Void run() {
						if (read1.isSuccessful()) {
							try {
								Integer r = Integer.valueOf(readSync(pos, buffer));
								if (ondone != null) ondone.run(new Pair<>(r, null));
								sp.unblockSuccess(r);
							} catch (IOException e) {
								if (ondone != null) ondone.run(new Pair<>(null, e));
								sp.unblockError(e);
							}
						} else if (read1.isCancelled())
							sp.unblockCancel(read1.getCancelEvent());
						else {
							if (ondone != null) ondone.run(new Pair<>(null, read1.getError()));
							sp.unblockError(read1.getError());
						}
						return null;
					}
				}, true);
				return sp;
			}
			if (!read2.isUnblocked()) {
				AsyncWork<Integer, IOException> sp = new AsyncWork<>();
				read2.listenAsync(new Task.Cpu<Void,NoException>("FullyBufferedIO.readAsync", io.getPriority()) {
					@Override
					public Void run() {
						if (read2.isSuccessful()) {
							try {
								Integer r = Integer.valueOf(readSync(pos, buffer));
								if (ondone != null) ondone.run(new Pair<>(r, null));
								sp.unblockSuccess(r);
							} catch (IOException e) {
								if (ondone != null) ondone.run(new Pair<>(null, e));
								sp.unblockError(e);
							}
						} else if (read2.isCancelled())
							sp.unblockCancel(read2.getCancelEvent());
						else {
							if (ondone != null) ondone.run(new Pair<>(null, read2.getError()));
							sp.unblockError(read2.getError());
						}
						return null;
					}
				}, true);
				return sp;
			}
			if (!read2.isSuccessful()) {
				IOException e;
				if (read2.isCancelled()) e = new IOException("Cancelled", read2.getCancelEvent());
				else e = read2.getError();
				if (ondone != null) ondone.run(new Pair<>(null, e));
				return new AsyncWork<>(null, e);
			}
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0) {
				if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
				return new AsyncWork<>(Integer.valueOf(-1), null);
			}
		}
		if (pos >= nb1 + nb2) {
			if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
			return new AsyncWork<>(Integer.valueOf(-1), null);
		}
		Task<Integer,IOException> task = new Task.Cpu<Integer, IOException>("readAsync on FullyBufferedIO", this.getPriority(), ondone) {
			@Override
			public Integer run() {
				int l = (nb1 + nb2) - (int)pos;
				if (l > len) l = len;
				buffer.put(buf2, (int)pos - nb1, l);
				TwoBuffersIO.this.pos = (int)pos + l;
				return Integer.valueOf(l);
			}
		};
		task.start();
		return task.getOutput();
	}

	@Override
	public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		Task.Cpu<ByteBuffer, IOException> task = new Task.Cpu<ByteBuffer, IOException>("Read next buffer", getPriority(), ondone) {
			@Override
			public ByteBuffer run() throws IOException {
				if (nb1 < 0) {
					if (read1.hasError()) throw read1.getError();
					if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
					nb1 = read1.getResult().intValue();
					if (nb1 < 0) nb1 = 0;
					if (nb1 == 0)
						return null;
				}
				if (pos < nb1) {
					ByteBuffer buf = ByteBuffer.allocate(nb1 - pos);
					buf.put(buf1, pos, nb1 - pos);
					buf.flip();
					pos = nb1;
					return buf;
				}
				if (buf2 == null) return null;
				if (nb2 < 0) {
					if (read1.hasError()) throw read1.getError();
					if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
					if (read2 == null) {
						synchronized (buf1) {
							while (read2 == null) {
								if (buf2 == null) return null;
								try { buf1.wait(); }
								catch (InterruptedException e) { /* ignore */ }
							}
						}
					}
					if (read2.hasError()) throw read2.getError();
					if (read2.isCancelled()) throw new IOException("Cancelled", read2.getCancelEvent());
					nb2 = read2.getResult().intValue();
					if (nb2 < 0) nb2 = 0;
					if (nb2 == 0)
						return null;
				}
				if (pos >= nb1 + nb2) return null;
				ByteBuffer buf = ByteBuffer.allocate(nb1 + nb2 - pos);
				if (pos < nb1) buf.put(buf1, pos, nb1 - pos);
				buf.put(buf2, pos - nb1, nb2 - (pos - nb1));
				buf.flip();
				pos = nb1 + nb2;
				return buf;
			}
		};
		
		if (nb1 < 0) {
			if (!read1.isUnblocked()) {
				read1.listenAsync(task, true);
				return task.getOutput();
			}
			if (read1.hasError()) {
				if (ondone != null) ondone.run(new Pair<>(null, read1.getError()));
				return new AsyncWork<>(null, read1.getError());
			}
			if (read1.isCancelled())
				return new AsyncWork<>(null, null, read1.getCancelEvent());
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0) {
				if (ondone != null) ondone.run(new Pair<>(null, null));
				return new AsyncWork<>(null, null);
			}
		}
		if (pos < nb1) {
			task.start();
			return task.getOutput();
		}
		if (buf2 == null) {
			if (ondone != null) ondone.run(new Pair<>(null, null));
			return new AsyncWork<>(null, null);
		}
		if (nb2 < 0) {
			if (!read1.isUnblocked()) {
				read1.listenAsync(task, true);
				return task.getOutput();
			}
			if (read1.hasError()) {
				if (ondone != null) ondone.run(new Pair<>(null, read1.getError()));
				return new AsyncWork<>(null, read1.getError());
			}
			if (read1.isCancelled())
				return new AsyncWork<>(null, null, read1.getCancelEvent());
			if (read2 == null) {
				task.start();
				return task.getOutput();
			}
			if (!read2.isUnblocked()) {
				read2.listenAsync(task, true);
				return task.getOutput();
			}
			if (read2.hasError()) {
				if (ondone != null) ondone.run(new Pair<>(null, read2.getError()));
				return new AsyncWork<>(null, read2.getError());
			}
			if (read2.isCancelled())
				return new AsyncWork<>(null, null, read2.getCancelEvent());
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0) {
				if (ondone != null) ondone.run(new Pair<>(null, null));
				return new AsyncWork<>(null, null);
			}
		}
		if (pos >= nb1 + nb2) {
			if (ondone != null) ondone.run(new Pair<>(null, null));
			return new AsyncWork<>(null, null);
		}
		task.start();
		return task.getOutput();
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return readFullySync(pos, buffer);
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		if (nb1 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0)
				return -1;
		}
		int len = buffer.remaining();
		int done = 0;
		if (pos < nb1) {
			int l = nb1 - (int)pos;
			if (l > len) l = len;
			buffer.put(buf1, (int)pos, l);
			this.pos = (int)pos + l;
			if (l == len) return l;
			pos += l;
			len -= l;
			done = l;
		}
		if (buf2 == null) return done > 0 ? done : -1;
		if (nb2 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			if (read2 == null) {
				synchronized (buf1) {
					while (read2 == null) {
						if (buf2 == null) return done > 0 ? done : -1;
						try { buf1.wait(); }
						catch (InterruptedException e) { /* ignore */ }
					}
				}
			}
			if (!read2.isUnblocked()) read2.block(0);
			if (!read2.isSuccessful()) {
				if (read2.isCancelled()) throw new IOException("Cancelled", read2.getCancelEvent());
				throw read2.getError();
			}
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0)
				return done > 0 ? done : -1;
		}
		if (pos >= nb1 + nb2) return done > 0 ? done : -1;
		int l = (nb1 + nb2) - (int)pos;
		if (l > len) l = len;
		buffer.put(buf2, (int)pos - nb1, l);
		this.pos = (int)pos + l;
		return l + done;
	}

	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsync(this, buffer, 0, ondone);
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsync(this, pos, buffer, ondone);
	}

	@Override
	public long skipSync(long n) throws IOException {
		return skip((int)n);
	}

	@Override
	public int skip(int skip) throws IOException {
		if (skip == 0) return 0;
		if (skip < 0) {
			if (-skip > pos) skip = -pos;
			pos += skip;
			return skip;
		}
		if (nb1 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0)
				return 0;
		}
		int done = 0;
		if (pos < nb1) {
			if (pos + skip <= nb1) {
				pos += skip;
				return skip;
			}
			done = nb1 - pos;
			pos = nb1;
			skip -= done;
		}
		if (buf2 == null) return done;
		if (nb2 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			if (read2 == null) {
				synchronized (buf1) {
					while (read2 == null) {
						if (buf2 == null) return done;
						try { buf1.wait(); }
						catch (InterruptedException e) { /* ignore */ }
					}
				}
			}
			if (!read2.isUnblocked()) read2.block(0);
			if (!read2.isSuccessful()) {
				if (read2.isCancelled()) throw new IOException("Cancelled", read2.getCancelEvent());
				throw read2.getError();
			}
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0)
				return done;
		}
		if (pos >= nb1 + nb2) return done;
		if (pos + skip <= nb1 + nb2) {
			pos += skip;
			return skip + done;
		}
		done += nb1 + nb2 - pos;
		pos = nb2;
		return done;
	}

	@Override
	public AsyncWork<Long, IOException> skipAsync(long skip, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		if (skip < 0) {
			if (-skip > pos) skip = -pos;
			pos += skip;
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(skip), null));
			return new AsyncWork<>(Long.valueOf(skip),null);
		}
		if (skip == 0) {
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(0), null));
			return new AsyncWork<>(Long.valueOf(0),null);
		}
		if (nb1 < 0) {
			if (!read1.isUnblocked()) {
				AsyncWork<Long, IOException> sp = new AsyncWork<>();
				long s = skip;
				read1.listenAsync(new Task.Cpu<Void,NoException>("FullyBufferedIO.skipAsync", io.getPriority()) {
					@Override
					public Void run() {
						if (read1.isSuccessful()) {
							try {
								Long r = Long.valueOf(skipSync(s));
								if (ondone != null) ondone.run(new Pair<>(r, null));
								sp.unblockSuccess(r);
							} catch (IOException e) {
								if (ondone != null) ondone.run(new Pair<>(null, e));
								sp.unblockError(e);
							}
						} else if (read1.isCancelled())
							sp.unblockCancel(read1.getCancelEvent());
						else {
							if (ondone != null) ondone.run(new Pair<>(null, read1.getError()));
							sp.unblockError(read1.getError());
						}
						return null;
					}
				}, true);
				return sp;
			}
			if (!read1.isSuccessful()) {
				IOException e;
				if (read1.isCancelled()) e = new IOException("Cancelled", read1.getCancelEvent());
				else e = read1.getError();
				if (ondone != null) ondone.run(new Pair<>(null, e));
				return new AsyncWork<>(null, e);
			}
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0) {
				if (ondone != null) ondone.run(new Pair<>(Long.valueOf(0), null));
				return new AsyncWork<>(Long.valueOf(0),null);
			}
		}
		int done = 0;
		if (pos < nb1) {
			if (pos + skip <= nb1) {
				pos += skip;
				if (ondone != null) ondone.run(new Pair<>(Long.valueOf(skip), null));
				return new AsyncWork<>(Long.valueOf(skip),null);
			}
			done = nb1 - pos;
			pos = nb1;
			skip -= done;
		}
		if (buf2 == null) {
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(done), null));
			return new AsyncWork<>(Long.valueOf(done),null);
		}
		if (nb2 < 0) {
			if (!read1.isUnblocked() || read2 == null) {
				AsyncWork<Long, IOException> sp = new AsyncWork<>();
				long s = skip;
				read1.listenAsync(new Task.Cpu<Void,NoException>("FullyBufferedIO.skipAsync", io.getPriority()) {
					@Override
					public Void run() {
						if (read1.isSuccessful()) {
							try {
								Long r = Long.valueOf(skipSync(s));
								if (ondone != null) ondone.run(new Pair<>(r, null));
								sp.unblockSuccess(r);
							} catch (IOException e) {
								if (ondone != null) ondone.run(new Pair<>(null, e));
								sp.unblockError(e);
							}
						} else if (read1.isCancelled())
							sp.unblockCancel(read1.getCancelEvent());
						else {
							if (ondone != null) ondone.run(new Pair<>(null, read1.getError()));
							sp.unblockError(read1.getError());
						}
						return null;
					}
				}, true);
				return sp;
			}
			if (!read1.isSuccessful()) {
				IOException e;
				if (read1.isCancelled()) e = new IOException("Cancelled", read1.getCancelEvent());
				else e = read1.getError();
				if (ondone != null) ondone.run(new Pair<>(null, e));
				return new AsyncWork<>(null, e);
			}
			if (!read2.isUnblocked()) {
				AsyncWork<Long, IOException> sp = new AsyncWork<>();
				long s = skip;
				int d = done;
				read2.listenAsync(new Task.Cpu<Void,NoException>("FullyBufferedIO.skipAsync", io.getPriority()) {
					@Override
					public Void run() {
						if (read2.isSuccessful()) {
							try {
								Long r = Long.valueOf(skipSync(s) + d);
								if (ondone != null) ondone.run(new Pair<>(r, null));
								sp.unblockSuccess(r);
							} catch (IOException e) {
								if (ondone != null) ondone.run(new Pair<>(null, e));
								sp.unblockError(e);
							}
						} else if (read2.isCancelled())
							sp.unblockCancel(read2.getCancelEvent());
						else {
							if (ondone != null) ondone.run(new Pair<>(null, read2.getError()));
							sp.unblockError(read2.getError());
						}
						return null;
					}
				}, true);
				return sp;
			}
			if (!read2.isSuccessful()) {
				IOException e;
				if (read2.isCancelled()) e = new IOException("Cancelled", read2.getCancelEvent());
				else e = read2.getError();
				if (ondone != null) ondone.run(new Pair<>(null, e));
				return new AsyncWork<>(null, e);
			}
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0) {
				if (ondone != null) ondone.run(new Pair<>(Long.valueOf(done), null));
				return new AsyncWork<>(Long.valueOf(done),null);
			}
		}
		if (pos >= nb1 + nb2) {
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(done), null));
			return new AsyncWork<>(Long.valueOf(done),null);
		}
		if (pos + skip <= nb1 + nb2) {
			pos += skip;
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(skip + done), null));
			return new AsyncWork<>(Long.valueOf(skip + done),null);
		}
		done += nb1 + nb2 - pos;
		pos = nb2;
		if (ondone != null) ondone.run(new Pair<>(Long.valueOf(done), null));
		return new AsyncWork<>(Long.valueOf(done),null);
	}

	@Override
	public long getSizeSync() throws IOException {
		if (nb1 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0)
				return 0;
		}
		if (buf2 == null) return nb1;
		if (nb2 < 0) {
			if (!read1.isUnblocked()) read1.block(0);
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
				throw read1.getError();
			}
			if (read2 == null) {
				synchronized (buf1) {
					while (read2 == null) {
						if (buf2 == null) return nb1;
						try { buf1.wait(); }
						catch (InterruptedException e) { /* ignore */ }
					}
				}
			}
			if (!read2.isUnblocked()) read2.block(0);
			if (!read2.isSuccessful()) {
				if (read2.isCancelled()) throw new IOException("Cancelled", read2.getCancelEvent());
				throw read2.getError();
			}
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0)
				return nb1;
		}
		return nb1 + nb2;
	}

	@Override
	public AsyncWork<Long, IOException> getSizeAsync() {
		if (nb1 < 0) {
			if (!read1.isUnblocked()) {
				AsyncWork<Long, IOException> sp = new AsyncWork<>();
				read1.listenAsync(new Task.Cpu<Void,NoException>("FullyBufferedIO.getSizeAsync", io.getPriority()) {
					@Override
					public Void run() {
						if (read1.isSuccessful()) {
							try { sp.unblockSuccess(Long.valueOf(getSizeSync())); }
							catch (IOException e) { sp.unblockError(e); }
						} else if (read1.isCancelled())
							sp.unblockCancel(read1.getCancelEvent());
						else
							sp.unblockError(read1.getError());
						return null;
					}
				}, true);
				return sp;
			}
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) return new AsyncWork<>(null, new IOException("Cancelled", read1.getCancelEvent()));
				return new AsyncWork<>(null, read1.getError());
			}
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0)
				return new AsyncWork<>(Long.valueOf(0),null);
		}
		if (buf2 == null) return new AsyncWork<>(Long.valueOf(nb1),null);
		if (nb2 < 0) {
			if (!read1.isUnblocked() || read2 == null) {
				AsyncWork<Long, IOException> sp = new AsyncWork<>();
				read1.listenAsync(new Task.Cpu<Void,NoException>("FullyBufferedIO.getSizeAsync", io.getPriority()) {
					@Override
					public Void run() {
						if (read1.isSuccessful()) {
							try { sp.unblockSuccess(Long.valueOf(getSizeSync())); }
							catch (IOException e) { sp.unblockError(e); }
						} else if (read1.isCancelled())
							sp.unblockCancel(read1.getCancelEvent());
						else
							sp.unblockError(read1.getError());
						return null;
					}
				}, true);
				return sp;
			}
			if (!read1.isSuccessful()) {
				if (read1.isCancelled()) return new AsyncWork<>(null, new IOException("Cancelled", read1.getCancelEvent()));
				return new AsyncWork<>(null, read1.getError());
			}
			if (!read2.isUnblocked()) {
				AsyncWork<Long, IOException> sp = new AsyncWork<>();
				read2.listenAsync(new Task.Cpu<Void,NoException>("FullyBufferedIO.getSizeAsync", io.getPriority()) {
					@Override
					public Void run() {
						if (read2.isSuccessful()) {
							try { sp.unblockSuccess(Long.valueOf(getSizeSync())); }
							catch (IOException e) { sp.unblockError(e); }
						} else if (read2.isCancelled())
							sp.unblockCancel(read2.getCancelEvent());
						else
							sp.unblockError(read2.getError());
						return null;
					}
				}, true);
				return sp;
			}
			if (!read2.isSuccessful()) {
				if (read2.isCancelled()) return new AsyncWork<>(null, new IOException("Cancelled", read2.getCancelEvent()));
				return new AsyncWork<>(null, read2.getError());
			}
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0)
				return new AsyncWork<>(Long.valueOf(nb1),null);
		}
		return new AsyncWork<>(Long.valueOf(nb1 + nb2),null);
	}

	@Override
	public long seekSync(SeekType type, long move) throws IOException {
		switch (type) {
		case FROM_CURRENT:
			skipSync(move);
			return pos;
		case FROM_BEGINNING:
			pos = 0;
			skipSync(move);
			return pos;
		case FROM_END:
			pos = (int)getSizeSync();
			skipSync(-move);
			return pos;
		default: return 0;
		}
	}

	@Override
	public AsyncWork<Long, IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		return IOUtil.seekAsyncUsingSync(this, type, move, ondone).getOutput();
	}
	
}
