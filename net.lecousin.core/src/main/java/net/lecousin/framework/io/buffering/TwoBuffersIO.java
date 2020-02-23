package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.ThreadUtil;

/**
 * Read an IO.Readable into 2 buffers, then those buffers can be read when ready.
 */
public class TwoBuffersIO extends ConcurrentCloseable<IOException> implements IO.Readable.Buffered, IO.Readable.Seekable, IO.KnownSize {

	/** Constructor. */
	public TwoBuffersIO(IO.Readable io, int firstBuffer, int secondBuffer) {
		this.io = io;
		buf1 = new byte[firstBuffer];
		read1 = io.readFullyAsync(ByteBuffer.wrap(buf1));
		operation(read1);
		if (secondBuffer > 0) {
			buf2 = new byte[secondBuffer];
			read1.onSuccess(() -> {
				int nb = read1.getResult().intValue();
				nb1 = nb > 0 ? nb : 0;
				if (nb < buf1.length) {
					buf2 = null;
					nb2 = 0;
					read2 = new AsyncSupplier<>(Integer.valueOf(0), null);
				} else {
					read2 = io.readFullyAsync(ByteBuffer.wrap(buf2));
					operation(read2);
				}
				synchronized (buf1) { buf1.notifyAll(); }
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
		public AsyncSupplier<Long, IOException> skipAsync(long n) {
			return new AsyncSupplier<>(Long.valueOf(skip((int)n)),null);
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
			return (long)buf1.length + (buf2 == null ? 0 : buf2.length);
		}

		@Override
		public AsyncSupplier<Long, IOException> getSizeAsync() {
			return new AsyncSupplier<>(Long.valueOf((long)buf1.length + (buf2 == null ? 0 : buf2.length)),null);
		}

		@Override
		public AsyncSupplier<Long, IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
			// seek does not need any task, we can do it sync
			try { return IOUtil.success(Long.valueOf(seekSync(type, move)), ondone); }
			catch (IOException e) { return IOUtil.error(e, ondone); }
		}

	}

	private IO.Readable io;
	protected byte[] buf1;
	protected byte[] buf2;
	private int nb1 = -1;
	private int nb2 = -1;
	private AsyncSupplier<Integer,IOException> read1;
	private AsyncSupplier<Integer,IOException>  read2;
	protected int pos = 0;

	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		io = null;
		buf1 = null;
		buf2 = null;
		ondone.unblock();
	}
	
	@Override
	public IAsync<IOException> canStartReading() {
		if (nb1 < 0 || pos < nb1 || buf2 == null)
			return read1;
		if (read2 == null) waitRead2();
		return read2;
	}
	
	private void waitRead2() {
		synchronized (buf1) {
			while (read2 == null)
				if (!ThreadUtil.wait(buf1, 0)) break;
		}
	}
	
	private void needRead1() throws IOException {
		if (!read1.isDone()) read1.block(0);
		if (!read1.isSuccessful()) {
			if (read1.isCancelled()) throw IO.errorCancelled(read1.getCancelEvent());
			throw read1.getError();
		}
		nb1 = read1.getResult().intValue();
		if (nb1 < 0) nb1 = 0;
	}
	
	private void needRead2() throws IOException {
		if (read2 == null) waitRead2();
		if (!read2.isDone()) read2.block(0);
		if (!read2.isSuccessful()) {
			if (read2.isCancelled()) throw IO.errorCancelled(read2.getCancelEvent());
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
	public Priority getPriority() {
		return io.getPriority();
	}

	@Override
	public void setPriority(Priority priority) {
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
		int nb = readSync(pos, buffer);
		if (nb > 0) pos += nb;
		return nb;
	}
	
	@Override
	public int readSync(long pos, ByteBuffer buffer) throws IOException {
		if (nb1 < 0) needRead1();
		int len = buffer.remaining();
		if (pos < nb1) {
			int l = nb1 - (int)pos;
			if (l > len) l = len;
			buffer.put(buf1, (int)pos, l);
			return l;
		}
		if (buf2 == null) return -1;
		if (nb2 < 0) needRead2();
		if (pos >= nb1 + nb2) return -1;
		int l = (nb1 + nb2) - (int)pos;
		if (l > len) l = len;
		buffer.put(buf2, (int)pos - nb1, l);
		return l;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		if (nb1 < 0) return readFullyAsync(buffer, ondone);
		int len = buffer.remaining();
		int done = 0;
		if (pos < nb1) {
			int l = nb1 - pos;
			if (l > len) l = len;
			buffer.put(buf1, pos, l);
			pos += l;
			if (l == len) return IOUtil.success(Integer.valueOf(l), ondone);
			len -= l;
			done = l;
		}
		if (buf2 == null) return IOUtil.success(Integer.valueOf(done > 0 ? done : -1), ondone);
		if (nb2 < 0) {
			if (done == 0) return readFullyAsync(buffer, ondone);
			AsyncSupplier<Integer, IOException> r = new AsyncSupplier<>();
			int d = done;
			readFullyAsync(buffer, res -> {
				if (ondone != null) {
					if (res.getValue1() != null) ondone.accept(new Pair<>(Integer.valueOf(d + res.getValue1().intValue()), null));
					else ondone.accept(res);
				}
			}).onDone(nb -> r.unblockSuccess(Integer.valueOf(nb.intValue() + d)), r);
			return r;
		}
		if (pos >= nb1 + nb2) return IOUtil.success(Integer.valueOf(done > 0 ? done : -1), ondone);
		int l = (nb1 + nb2) - pos;
		if (l > len) l = len;
		buffer.put(buf2, pos - nb1, l);
		pos += l;
		return IOUtil.success(Integer.valueOf(l + done), ondone);
	}

	private <T> AsyncSupplier<T, IOException> needRead1Async(Callable<T> onReady, Consumer<Pair<T,IOException>> ondone) {
		if (!read1.isDone()) {
			AsyncSupplier<T, IOException> sp = new AsyncSupplier<>();
			IOUtil.listenOnDone(read1, Task.cpu("TwoBuffersIO.needRead1Async", io.getPriority(), () -> {
				try {
					IOUtil.success(onReady.call(), sp, ondone);
				} catch (Exception e) {
					IOUtil.error(IO.error(e), sp, ondone);
				}
				return null;
			}), sp, ondone);
			return operation(sp);
		}
		if (!read1.isSuccessful()) {
			IOException e = read1.isCancelled() ? IO.error(read1.getCancelEvent()) : read1.getError();
			return IOUtil.error(e, ondone);
		}
		nb1 = read1.getResult().intValue();
		if (nb1 < 0) nb1 = 0;
		return null;
	}
	
	private <T> AsyncSupplier<T, IOException> needRead2Async(Callable<T> onReady, Consumer<Pair<T,IOException>> ondone) {
		if (!read1.isDone() || read2 == null) {
			AsyncSupplier<T, IOException> sp = new AsyncSupplier<>();
			IOUtil.listenOnDone(read1, Task.cpu("TwoBuffersIO.needRead2Async", io.getPriority(), () -> {
				try {
					IOUtil.success(onReady.call(), sp, ondone);
				} catch (Exception e) {
					IOUtil.error(IO.error(e), sp, ondone);
				}
				return null;
			}), sp, ondone);
			return operation(sp);
		}
		if (!read2.isDone()) {
			AsyncSupplier<T, IOException> sp = new AsyncSupplier<>();
			IOUtil.listenOnDone(read2, Task.cpu("TwoBuffersIO", io.getPriority(), () -> {
				try {
					IOUtil.success(onReady.call(), sp, ondone);
				} catch (Exception e) {
					IOUtil.error(IO.error(e), sp, ondone);
				}
				return null;
			}), sp, ondone);
			return operation(sp);
		}
		if (!read2.isSuccessful()) {
			IOException e = read2.isCancelled() ? IO.error(read2.getCancelEvent()) : read2.getError();
			return IOUtil.error(e, ondone);
		}
		nb2 = read2.getResult().intValue();
		if (nb2 < 0) nb2 = 0;
		return null;
	}
	
	@Override
	public int readAsync() throws IOException {
		if (nb1 < 0) {
			if (!read1.isDone()) return -2;
			needRead1();
		}
		if (pos < nb1)
			return buf1[pos++] & 0xFF;
		if (buf2 == null) return -1;
		if (nb2 < 0) {
			if (read2 == null) waitRead2();
			if (!read2.isDone()) return -2;
			needRead2();
		}
		if (pos >= nb1 + nb2) return -1;
		return buf2[(pos++) - nb1] & 0xFF;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return readAsync(pos, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				pos += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		if (nb1 < 0) {
			AsyncSupplier<Integer, IOException> res = needRead1Async(() -> {
				if (nb1 == 0) return Integer.valueOf(-1);
				return Integer.valueOf(readSync(pos, buffer));
			}, ondone);
			if (res != null) return res;
		}
		int len = buffer.remaining();
		if (pos < nb1) {
			Task<Integer,IOException> task = Task.cpu("readAsync on FullyBufferedIO", this.getPriority(), () -> {
				int l = nb1 - (int)pos;
				if (l > len) l = len;
				buffer.put(buf1, (int)pos, l);
				return Integer.valueOf(l);
			}, ondone);
			operation(task).start();
			return task.getOutput();
		}
		if (buf2 == null) return IOUtil.success(Integer.valueOf(-1), ondone);
		if (nb2 < 0) {
			AsyncSupplier<Integer, IOException> res = needRead2Async(() -> Integer.valueOf(readSync(pos, buffer)), ondone);
			if (res != null) return res;
		}
		if (pos >= nb1 + nb2) return IOUtil.success(Integer.valueOf(-1), ondone);
		Task<Integer,IOException> task = Task.cpu("readAsync on TwoBuffersIO", this.getPriority(), () -> {
			int l = (nb1 + nb2) - (int)pos;
			if (l > len) l = len;
			buffer.put(buf2, (int)pos - nb1, l);
			return Integer.valueOf(l);
		}, ondone);
		operation(task.start());
		return task.getOutput();
	}

	@Override
	@SuppressWarnings("squid:S3776") // complexity
	public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		Task<ByteBuffer, IOException> task = readNextBufferTask(ondone);
		
		if (nb1 < 0) {
			if (!read1.isDone()) {
				read1.thenStart(task, true);
				return operation(task).getOutput();
			}
			if (read1.hasError()) return IOUtil.error(read1.getError(), ondone);
			if (read1.isCancelled()) return new AsyncSupplier<>(null, null, read1.getCancelEvent());
			nb1 = read1.getResult().intValue();
			if (nb1 < 0) nb1 = 0;
			if (nb1 == 0) return IOUtil.success(null, ondone);
		}
		if (pos < nb1) {
			operation(task.start());
			return task.getOutput();
		}
		if (buf2 == null) return IOUtil.success(null, ondone);
		if (nb2 < 0) {
			if (!read1.isDone()) {
				read1.thenStart(task, true);
				return operation(task).getOutput();
			}
			if (read1.hasError()) return IOUtil.error(read1.getError(), ondone);
			if (read1.isCancelled()) return new AsyncSupplier<>(null, null, read1.getCancelEvent());
			if (read2 == null) {
				operation(task.start());
				return task.getOutput();
			}
			if (!read2.isDone()) {
				read2.thenStart(task, true);
				return operation(task).getOutput();
			}
			if (read2.hasError()) return IOUtil.error(read2.getError(), ondone);
			if (read2.isCancelled()) return new AsyncSupplier<>(null, null, read2.getCancelEvent());
			nb2 = read2.getResult().intValue();
			if (nb2 < 0) nb2 = 0;
			if (nb2 == 0) return IOUtil.success(null, ondone);
		}
		if (pos >= nb1 + nb2) return IOUtil.success(null, ondone);
		operation(task.start());
		return task.getOutput();
	}
	
	private Task<ByteBuffer, IOException> readNextBufferTask(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		return Task.cpu("Read next buffer", getPriority(), new ReadNextBuffer(), ondone);
	}
	
	private class ReadNextBuffer implements Executable<ByteBuffer, IOException> {
		@Override
		@SuppressWarnings("squid:S3776") // complexity
		public ByteBuffer execute() throws IOException {
			if (nb1 < 0) {
				if (read1.hasError()) throw read1.getError();
				if (read1.isCancelled()) throw IO.errorCancelled(read1.getCancelEvent());
				nb1 = read1.getResult().intValue();
				if (nb1 < 0) nb1 = 0;
				if (nb1 == 0)
					return null;
			}
			if (pos < nb1) return getRemainingBuf1();
			if (buf2 == null) return null;
			if (nb2 < 0) {
				if (read1.hasError()) throw read1.getError();
				if (read1.isCancelled()) throw IO.errorCancelled(read1.getCancelEvent());
				if (read2 == null) waitRead2();
				if (read2.hasError()) throw read2.getError();
				if (read2.isCancelled()) throw IO.errorCancelled(read2.getCancelEvent());
				nb2 = read2.getResult().intValue();
				if (nb2 < 0) nb2 = 0;
				if (nb2 == 0)
					return null;
			}
			if (pos >= nb1 + nb2) return null;
			return getRemainingBuf2();
		}
	}
	
	@Override
	@SuppressWarnings("squid:S3776") // complexity
	public ByteBuffer readNextBuffer() throws IOException {
		if (nb1 < 0) {
			needRead1();
			if (nb1 == 0) return null;
		}
		if (pos < nb1) return getRemainingBuf1();
		if (buf2 == null) return null;
		if (nb2 < 0) {
			needRead2();
			if (nb2 == 0)
				return null;
		}
		if (pos >= nb1 + nb2) return null;
		return getRemainingBuf2();
	}
	
	private ByteBuffer getRemainingBuf1() {
		ByteBuffer buf = ByteBuffer.wrap(buf1, pos, nb1 - pos).asReadOnlyBuffer();
		pos = nb1;
		return buf;
	}
	
	private ByteBuffer getRemainingBuf2() {
		ByteBuffer buf = ByteBuffer.wrap(buf2, pos - nb1, nb2 - (pos - nb1)).asReadOnlyBuffer();
		pos = nb1 + nb2;
		return buf;
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		int nb = readFullySync(pos, buffer);
		if (nb > 0) pos += nb;
		return nb;
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
		return l + done;
	}

	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsync(this, buffer, 0, ondone));
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
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
	public AsyncSupplier<Long, IOException> skipAsync(long skip, Consumer<Pair<Long,IOException>> ondone) {
		if (skip < 0) {
			if (-skip > pos) skip = -pos;
			pos += skip;
			return IOUtil.success(Long.valueOf(skip), ondone);
		}
		if (skip == 0) return IOUtil.success(Long.valueOf(0), ondone);
		if (nb1 < 0) {
			long s = skip;
			AsyncSupplier<Long, IOException> res = needRead1Async(() -> {
				if (nb1 == 0) return Long.valueOf(0);
				return Long.valueOf(skipSync(s));
			}, ondone);
			if (res != null) return res;
		}
		int done = 0;
		if (pos < nb1) {
			if (pos + skip <= nb1) {
				pos += skip;
				return IOUtil.success(Long.valueOf(skip), ondone);
			}
			done = nb1 - pos;
			pos = nb1;
			skip -= done;
		}
		if (buf2 == null) return IOUtil.success(Long.valueOf(done), ondone);
		if (nb2 < 0) {
			long s = skip;
			long d = done;
			AsyncSupplier<Long, IOException> res = needRead2Async(() -> Long.valueOf(skipSync(s) + d), ondone);
			if (res != null) return res;
		}
		if (pos >= nb1 + nb2) return IOUtil.success(Long.valueOf(done), ondone);
		if (pos + skip <= nb1 + nb2) {
			pos += skip;
			return IOUtil.success(Long.valueOf(skip + done), ondone);
		}
		done += (nb1 + nb2) - pos;
		pos = nb1 + nb2;
		return IOUtil.success(Long.valueOf(done), ondone);
	}

	@Override
	public long getSizeSync() throws IOException {
		if (nb1 < 0) needRead1();
		if (buf2 == null) return nb1;
		if (nb2 < 0) needRead2();
		return (long)nb1 + nb2;
	}

	@Override
	public AsyncSupplier<Long, IOException> getSizeAsync() {
		if (nb1 < 0) {
			AsyncSupplier<Long, IOException> res = needRead1Async(() -> Long.valueOf(getSizeSync()), null);
			if (res != null) return res;
		}
		if (buf2 == null) return new AsyncSupplier<>(Long.valueOf(nb1),null);
		if (nb2 < 0) {
			AsyncSupplier<Long, IOException> res = needRead2Async(() -> Long.valueOf(getSizeSync()), null);
			if (res != null) return res;
		}
		return new AsyncSupplier<>(Long.valueOf((long)nb1 + nb2),null);
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
	public AsyncSupplier<Long, IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		return operation(IOUtil.seekAsyncUsingSync(this, type, move, ondone));
	}
	
}
