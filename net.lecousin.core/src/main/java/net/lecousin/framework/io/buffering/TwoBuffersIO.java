package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
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
		operation(read1 = io.readFullyAsync(ByteBuffer.wrap(buf1)));
		if (secondBuffer > 0) {
			buf2 = new byte[secondBuffer];
			read1.onSuccess(new Runnable() {
				@Override
				public void run() {
					int nb = read1.getResult().intValue();
					nb1 = nb > 0 ? nb : 0;
					if (nb < buf1.length) {
						buf2 = null;
						nb2 = 0;
						read2 = new AsyncWork<>(Integer.valueOf(0), null);
					} else
						operation(read2 = io.readFullyAsync(ByteBuffer.wrap(buf2)));
					synchronized (buf1) { buf1.notifyAll(); }
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
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		io = null;
		buf1 = null;
		buf2 = null;
		ondone.unblock();
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		if (nb1 < 0 || pos < nb1 || buf2 == null)
			return read1;
		if (read2 == null) waitRead2();
		return read2;
	}
	
	private void waitRead2() {
		synchronized (buf1) {
			while (read2 == null) {
				try { buf1.wait(); }
				catch (InterruptedException e) { /* ignore */ }
			}
		}
	}
	
	private void needRead1() throws IOException {
		if (!read1.isUnblocked()) read1.block(0);
		if (!read1.isSuccessful()) {
			if (read1.isCancelled()) throw new IOException("Cancelled", read1.getCancelEvent());
			throw read1.getError();
		}
		nb1 = read1.getResult().intValue();
		if (nb1 < 0) nb1 = 0;
	}
	
	private void needRead2() throws IOException {
		if (read2 == null) waitRead2();
		if (!read2.isUnblocked()) read2.block(0);
		if (!read2.isSuccessful()) {
			if (read2.isCancelled()) throw new IOException("Cancelled", read2.getCancelEvent());
			throw read2.getError();
		}
		nb2 = read2.getResult().intValue();
		if (nb2 < 0) nb2 = 0;
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
		if (nb1 < 0) needRead1();
		if (pos < nb1)
			return buf1[pos++] & 0xFF;
		if (buf2 == null) return -1;
		if (nb2 < 0) needRead2();
		if (pos >= nb1 + nb2) return -1;
		return buf2[(pos++) - nb1] & 0xFF;
	}
	
	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		if (nb1 < 0) needRead1();
		if (pos < nb1) {
			int l = nb1 - pos;
			if (l > len) l = len;
			System.arraycopy(buf1, pos, buffer, offset, l);
			pos += l;
			return l;
		}
		if (buf2 == null) return -1;
		if (nb2 < 0) needRead2();
		if (pos >= nb1 + nb2) return -1;
		int l = (nb1 + nb2) - pos;
		if (l > len) l = len;
		System.arraycopy(buf2, pos - nb1, buffer, offset, l);
		pos += l;
		return l;
	}

	@Override
	public int readFully(byte[] buffer) throws IOException {
		if (nb1 < 0) needRead1();
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
		if (nb2 < 0) needRead2();
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
		if (nb1 < 0) needRead1();
		int len = buffer.remaining();
		if (pos < nb1) {
			int l = nb1 - (int)pos;
			if (l > len) l = len;
			buffer.put(buf1, (int)pos, l);
			this.pos = (int)pos + l;
			return l;
		}
		if (buf2 == null) return -1;
		if (nb2 < 0) needRead2();
		if (pos >= nb1 + nb2) return -1;
		int l = (nb1 + nb2) - (int)pos;
		if (l > len) l = len;
		buffer.put(buf2, (int)pos - nb1, l);
		this.pos = (int)pos + l;
		return l;
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
		if (nb1 < 0) return readFullyAsync(buffer, ondone);
		int len = buffer.remaining();
		int done = 0;
		if (pos < nb1) {
			int l = nb1 - pos;
			if (l > len) l = len;
			buffer.put(buf1, pos, l);
			pos += l;
			if (l == len) {
				Integer r = Integer.valueOf(l);
				if (ondone != null) ondone.run(new Pair<>(r, null));
				return new AsyncWork<>(r, null);
			}
			len -= l;
			done = l;
		}
		if (buf2 == null) {
			Integer r = Integer.valueOf(done > 0 ? done : -1);
			if (ondone != null) ondone.run(new Pair<>(r, null));
			return new AsyncWork<>(r, null);
		}
		if (nb2 < 0) {
			if (done == 0)
				return readFullyAsync(buffer, ondone);
			AsyncWork<Integer, IOException> r = new AsyncWork<>();
			int d = done;
			readFullyAsync(buffer, (res) -> {
				if (ondone != null) {
					if (res.getValue1() != null) ondone.run(new Pair<>(Integer.valueOf(d + res.getValue1().intValue()), null));
					else ondone.run(res);
				}
			}).listenInline((nb) -> {
				r.unblockSuccess(Integer.valueOf(nb.intValue() + d));
			}, r);
			return r;
		}
		if (pos >= nb1 + nb2) {
			Integer r = Integer.valueOf(done > 0 ? done : -1);
			if (ondone != null) ondone.run(new Pair<>(r, null));
			return new AsyncWork<>(r, null);
		}
		int l = (nb1 + nb2) - pos;
		if (l > len) l = len;
		buffer.put(buf2, pos - nb1, l);
		pos += l;
		Integer r = Integer.valueOf(l + done);
		if (ondone != null) ondone.run(new Pair<>(r, null));
		return new AsyncWork<>(r, null);
	}

	private <T> AsyncWork<T, IOException> needRead1Async(Callable<T> onReady, RunnableWithParameter<Pair<T,IOException>> ondone) {
		if (!read1.isUnblocked()) {
			AsyncWork<T, IOException> sp = new AsyncWork<>();
			IOUtil.listenOnDone(read1, new Task.Cpu.FromRunnable("TwoBuffersIO", io.getPriority(), () -> {
				try {
					T r = onReady.call();
					if (ondone != null) ondone.run(new Pair<>(r, null));
					sp.unblockSuccess(r);
				} catch (Exception e) {
					IOException err = IO.error(e);
					if (ondone != null) ondone.run(new Pair<>(null, err));
					sp.unblockError(err);
				}
			}), sp, ondone);
			return operation(sp);
		}
		if (!read1.isSuccessful()) {
			IOException e = read1.isCancelled() ? IO.error(read1.getCancelEvent()) : read1.getError();
			if (ondone != null) ondone.run(new Pair<>(null, e));
			return new AsyncWork<>(null, e);
		}
		nb1 = read1.getResult().intValue();
		if (nb1 < 0) nb1 = 0;
		return null;
	}
	
	private <T> AsyncWork<T, IOException> needRead2Async(Callable<T> onReady, RunnableWithParameter<Pair<T,IOException>> ondone) {
		if (!read1.isUnblocked() || read2 == null) {
			AsyncWork<T, IOException> sp = new AsyncWork<>();
			IOUtil.listenOnDone(read1, new Task.Cpu.FromRunnable("TwoBuffersIO", io.getPriority(), () -> {
				try {
					T r = onReady.call();
					if (ondone != null) ondone.run(new Pair<>(r, null));
					sp.unblockSuccess(r);
				} catch (Exception e) {
					IOException err = IO.error(e);
					if (ondone != null) ondone.run(new Pair<>(null, err));
					sp.unblockError(err);
				}
			}), sp, ondone);
			return operation(sp);
		}
		if (!read2.isUnblocked()) {
			AsyncWork<T, IOException> sp = new AsyncWork<>();
			IOUtil.listenOnDone(read2, new Task.Cpu.FromRunnable("TwoBuffersIO", io.getPriority(), () -> {
				try {
					T r = onReady.call();
					if (ondone != null) ondone.run(new Pair<>(r, null));
					sp.unblockSuccess(r);
				} catch (Exception e) {
					IOException err = IO.error(e);
					if (ondone != null) ondone.run(new Pair<>(null, err));
					sp.unblockError(err);
				}
			}), sp, ondone);
			return operation(sp);
		}
		if (!read2.isSuccessful()) {
			IOException e = read2.isCancelled() ? IO.error(read2.getCancelEvent()) : read2.getError();
			if (ondone != null) ondone.run(new Pair<>(null, e));
			return new AsyncWork<>(null, e);
		}
		nb2 = read2.getResult().intValue();
		if (nb2 < 0) nb2 = 0;
		return null;
	}
	
	@Override
	public int readAsync() throws IOException {
		if (nb1 < 0) {
			if (!read1.isUnblocked()) return -2;
			needRead1();
		}
		if (pos < nb1)
			return buf1[pos++] & 0xFF;
		if (buf2 == null) return -1;
		if (nb2 < 0) {
			if (read2 == null) waitRead2();
			if (!read2.isUnblocked()) return -2;
			needRead2();
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
			AsyncWork<Integer, IOException> res = needRead1Async(() -> {
				if (nb1 == 0) return Integer.valueOf(-1);
				return Integer.valueOf(readSync(pos, buffer));
			}, ondone);
			if (res != null) return res;
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
			operation(task).start();
			return task.getOutput();
		}
		if (buf2 == null) {
			if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
			return new AsyncWork<>(Integer.valueOf(-1), null);
		}
		if (nb2 < 0) {
			AsyncWork<Integer, IOException> res = needRead2Async(() -> {
				return Integer.valueOf(readSync(pos, buffer));
			}, ondone);
			if (res != null) return res;
		}
		if (pos >= nb1 + nb2) {
			if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
			return new AsyncWork<>(Integer.valueOf(-1), null);
		}
		Task<Integer,IOException> task = new Task.Cpu<Integer, IOException>("readAsync on TwoBuffersIO", this.getPriority(), ondone) {
			@Override
			public Integer run() {
				int l = (nb1 + nb2) - (int)pos;
				if (l > len) l = len;
				buffer.put(buf2, (int)pos - nb1, l);
				TwoBuffersIO.this.pos = (int)pos + l;
				return Integer.valueOf(l);
			}
		};
		operation(task.start());
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
					if (read2 == null) waitRead2();
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
				return operation(task).getOutput();
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
			operation(task.start());
			return task.getOutput();
		}
		if (buf2 == null) {
			if (ondone != null) ondone.run(new Pair<>(null, null));
			return new AsyncWork<>(null, null);
		}
		if (nb2 < 0) {
			if (!read1.isUnblocked()) {
				read1.listenAsync(task, true);
				return operation(task).getOutput();
			}
			if (read1.hasError()) {
				if (ondone != null) ondone.run(new Pair<>(null, read1.getError()));
				return new AsyncWork<>(null, read1.getError());
			}
			if (read1.isCancelled())
				return new AsyncWork<>(null, null, read1.getCancelEvent());
			if (read2 == null) {
				operation(task.start());
				return task.getOutput();
			}
			if (!read2.isUnblocked()) {
				read2.listenAsync(task, true);
				return operation(task).getOutput();
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
		operation(task.start());
		return task.getOutput();
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return readFullySync(pos, buffer);
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		if (nb1 < 0) needRead1();
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
		if (nb2 < 0) needRead2();
		if (pos >= nb1 + nb2) return done > 0 ? done : -1;
		int l = (nb1 + nb2) - (int)pos;
		if (l > len) l = len;
		buffer.put(buf2, (int)pos - nb1, l);
		this.pos = (int)pos + l;
		return l + done;
	}

	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsync(this, buffer, 0, ondone));
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsync(this, pos, buffer, ondone));
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
		if (nb1 < 0) needRead1();
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
		if (nb2 < 0) needRead2();
		if (pos >= nb1 + nb2) return done;
		if (pos + skip <= nb1 + nb2) {
			pos += skip;
			return skip + done;
		}
		done += (nb1 + nb2) - pos;
		pos = nb1 + nb2;
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
			long s = skip;
			AsyncWork<Long, IOException> res = needRead1Async(() -> {
				if (nb1 == 0) return Long.valueOf(0);
				return Long.valueOf(skipSync(s));
			}, ondone);
			if (res != null) return res;
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
			long s = skip;
			long d = done;
			AsyncWork<Long, IOException> res = needRead2Async(() -> {
				return Long.valueOf(skipSync(s) + d);
			}, ondone);
			if (res != null) return res;
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
		done += (nb1 + nb2) - pos;
		pos = nb1 + nb2;
		if (ondone != null) ondone.run(new Pair<>(Long.valueOf(done), null));
		return new AsyncWork<>(Long.valueOf(done),null);
	}

	@Override
	public long getSizeSync() throws IOException {
		if (nb1 < 0) needRead1();
		if (buf2 == null) return nb1;
		if (nb2 < 0) needRead2();
		return nb1 + nb2;
	}

	@Override
	public AsyncWork<Long, IOException> getSizeAsync() {
		if (nb1 < 0) {
			AsyncWork<Long, IOException> res = needRead1Async(() -> {
				return Long.valueOf(getSizeSync());
			}, null);
			if (res != null) return res;
		}
		if (buf2 == null) return new AsyncWork<>(Long.valueOf(nb1),null);
		if (nb2 < 0) {
			AsyncWork<Long, IOException> res = needRead2Async(() -> {
				return Long.valueOf(getSizeSync());
			}, null);
			if (res != null) return res;
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
		return operation(IOUtil.seekAsyncUsingSync(this, type, move, ondone)).getOutput();
	}
	
}
