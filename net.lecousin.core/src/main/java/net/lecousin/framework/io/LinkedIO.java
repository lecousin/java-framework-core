package net.lecousin.framework.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.AsyncSupplier.Listener;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.mutable.MutableLong;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Make several IOs as a single one.
 */
public abstract class LinkedIO extends ConcurrentCloseable<IOException> implements IO {

	protected LinkedIO(String description, IO[] ios) {
		this.description = description;
		this.ios = new ArrayList<>(ios.length);
		sizes = new ArrayList<>(ios.length);
		for (int i = 0 ; i < ios.length; ++i) {
			this.ios.add(ios[i]);
			if (ios[i] instanceof IO.KnownSize)
				try { sizes.add(Long.valueOf(((IO.KnownSize)ios[i]).getSizeSync())); }
				catch (IOException e) { sizes.add(null); }
			else
				sizes.add(null);
		}
	}
	
	protected String description;
	protected ArrayList<IO> ios;
	protected ArrayList<Long> sizes;
	protected int ioIndex = 0;
	protected long pos = 0;
	protected long posInIO = 0;
	
	private static final String WRITE_ASYNC_TASK_DESCRIPTION = "LinkedIO.writeAsync";
	
	@Override
	public byte getPriority() {
		return ios.isEmpty() ? Task.PRIORITY_NORMAL : ios.get(0).getPriority();
	}
	
	@Override
	public void setPriority(byte priority) {
		for (IO io : ios) io.setPriority(priority);
	}
	
	@Override
	public String getSourceDescription() {
		return description;
	}
	
	@Override
	public TaskManager getTaskManager() {
		return ios.isEmpty() ? Threading.getCPUTaskManager() : ios.get(0).getTaskManager();
	}
	
	@Override
	public IO getWrappedIO() {
		return null;
	}
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		JoinPoint<IOException> jp = new JoinPoint<>();
		for (IO io : ios) jp.addToJoin(io.closeAsync());
		jp.start();
		return jp;
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		ios = null;
		sizes = null;
		ondone.unblock();
	}
	
	
	/** Linked Readable IO. */
	public static class Readable extends LinkedIO implements IO.Readable {
		
		/** Constructor. */
		public Readable(String description, IO.Readable... ios) {
			super(description, ios);
		}
		
		@Override
		protected void nextIOSync() throws IOException {
			nextIOSyncStream();
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		protected void nextIOAsync(Runnable ondone, IAsync<IOException> onerror, Consumer rp) {
			nextIOAsyncStream(ondone);
		}
		
		@Override
		protected void previousIOSync() throws IOException {
			previousIOSyncStream();
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		protected void previousIOAsync(Runnable ondone, IAsync<IOException> onerror, Consumer rp) {
			previousIOAsyncStream(ondone);
		}
		
		@Override
		public IAsync<IOException> canStartReading() {
			return super.canStartReading();
		}
		
		@Override
		public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readAsync(buffer, ondone);
		}
		
		@Override
		public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
			return super.readFullyAsync(buffer, ondone);
		}
		
		@Override
		public int readSync(ByteBuffer buffer) throws IOException {
			return super.readSync(buffer);
		}
		
		@Override
		public int readFullySync(ByteBuffer buffer) throws IOException {
			return super.readFullySync(buffer);
		}
		
		@Override
		public long skipSync(long n) throws IOException {
			return super.skipSync(n);
		}
		
		@Override
		public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
			return super.skipAsync(n, ondone);
		}
		
		/** Linked Readable Buffered IO. */
		public static class Buffered extends LinkedIO.Readable implements IO.Readable.Buffered {

			/** Constructor. */
			public Buffered(String description, IO.Readable.Buffered... ios) {
				super(description, ios);
			}
			
			@Override
			public int read() throws IOException {
				return super.read();
			}
			
			@Override
			public int read(byte[] buf, int off, int len) throws IOException {
				return super.read(buf, off, len);
			}
			
			@Override
			public int readFully(byte[] buffer) throws IOException {
				return super.readFully(buffer);
			}
			
			@Override
			public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(
				ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
			) {
				return super.readFullySyncIfPossible(buffer, ondone);
			}
			
			@Override
			public int readAsync() throws IOException {
				return super.readAsync();
			}
			
			@Override
			public int skip(int n) throws IOException {
				return super.skip(n);
			}
			
			@Override
			public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
				return super.readNextBufferAsync(ondone);
			}
			
			/** Linked Readable Buffered IO and add the DeterminedSize capability. */
			public static class DeterminedSize extends LinkedIO.Readable.Buffered implements IO.KnownSize {

				/** Constructor. */
				public DeterminedSize(String description, IO.Readable.Buffered... ios) {
					super(description, ios);
				}
				
				@Override
				public long getSizeSync() throws IOException {
					return super.getSizeSync();
				}
				
				@Override
				public AsyncSupplier<Long, IOException> getSizeAsync() {
					return super.getSizeAsync();
				}
				
			}
			
		}
		
		/** Linked Readable IO and add the DeterminedSize capability. */
		public static class DeterminedSize extends LinkedIO.Readable implements IO.KnownSize {
			
			/** Constructor. */
			public DeterminedSize(String description, IO.Readable... ios) {
				super(description, ios);
			}
			
			@Override
			public long getSizeSync() throws IOException {
				return super.getSizeSync();
			}
			
			@Override
			public AsyncSupplier<Long, IOException> getSizeAsync() {
				return super.getSizeAsync();
			}

		}
		
		/** Linked Readable Seekable IO. */
		public static class Seekable extends LinkedIO.Readable implements IO.Readable.Seekable {
			
			/** Constructor. */
			public Seekable(String description, IO.Readable.Seekable... ios) {
				super(description, ios);
			}
			
			@Override
			protected void nextIOSync() throws IOException {
				nextIOSyncSeekable();
			}
			
			@SuppressWarnings("rawtypes")
			@Override
			protected void nextIOAsync(Runnable ondone, IAsync<IOException> onerror, Consumer rp) {
				nextIOAsyncSeekable(ondone, onerror, rp);
			}
			
			@Override
			protected void previousIOSync() throws IOException {
				previousIOSyncSeekable();
			}
			
			@SuppressWarnings("rawtypes")
			@Override
			protected void previousIOAsync(Runnable ondone, IAsync<IOException> onerror, Consumer rp) {
				previousIOAsyncSeekable(ondone, onerror, rp);
			}

			@Override
			public long getPosition() {
				return super.getPosition();
			}

			@Override
			public long seekSync(SeekType type, long move) throws IOException {
				return super.seekSync(type, move);
			}

			@Override
			public AsyncSupplier<Long, IOException> seekAsync(
				SeekType type, long move, Consumer<Pair<Long,IOException>> ondone
			) {
				return super.seekAsync(type, move, ondone);
			}

			@Override
			public int readSync(long pos, ByteBuffer buffer) throws IOException {
				return super.readSync(pos, buffer);
			}

			@Override
			public AsyncSupplier<Integer, IOException> readAsync(
				long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
			) {
				return super.readAsync(pos, buffer, ondone);
			}

			@Override
			public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
				return super.readFullySync(pos, buffer);
			}

			@Override
			public AsyncSupplier<Integer, IOException> readFullyAsync(
				long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
			) {
				return super.readFullyAsync(pos, buffer, ondone);
			}

			
			/** Linked Readable, Seekable and Buffered IO. */
			public static class Buffered extends LinkedIO.Readable.Seekable implements IO.Readable.Buffered {
				
				/** Constructor. */
				public Buffered(String description, IO.Readable.Seekable... ios) {
					super(description, ios);
				}
				
				@Override
				public int read() throws IOException {
					return super.read();
				}
				
				@Override
				public int read(byte[] buf, int off, int len) throws IOException {
					return super.read(buf, off, len);
				}
				
				@Override
				public int readFully(byte[] buffer) throws IOException {
					return super.readFully(buffer);
				}
				
				@Override
				public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(
					ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
				) {
					return super.readFullySyncIfPossible(buffer, ondone);
				}
				
				@Override
				public int readAsync() throws IOException {
					return super.readAsync();
				}
				
				@Override
				public int skip(int n) throws IOException {
					return super.skip(n);
				}
				
				@Override
				public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(
					Consumer<Pair<ByteBuffer, IOException>> ondone
				) {
					return super.readNextBufferAsync(ondone);
				}
				
				/** Add the DeterminedSize capability. */
				public static class DeterminedSize extends LinkedIO.Readable.Seekable.Buffered implements IO.KnownSize {
					
					/** Constructor. */
					public DeterminedSize(String description, IO.Readable.Seekable... ios) {
						super(description, ios);
					}
					
					@Override
					public long getSizeSync() throws IOException {
						return super.getSizeSync();
					}
					
					@Override
					public AsyncSupplier<Long, IOException> getSizeAsync() {
						return super.getSizeAsync();
					}
					
				}

			}

			/** Add the DeterminedSize capability. */
			public static class DeterminedSize extends LinkedIO.Readable.Seekable implements IO.KnownSize {
				/** Constructor. */
				public DeterminedSize(String description, IO.Readable.Seekable... ios) {
					super(description, ios);
				}
				
				@Override
				public long getSizeSync() throws IOException {
					return super.getSizeSync();
				}
				
				@Override
				public AsyncSupplier<Long, IOException> getSizeAsync() {
					return super.getSizeAsync();
				}
				
			}
			
		}
		
	}
	
	/** Linked Readable and Writable IO. */
	public static class ReadWrite extends LinkedIO.Readable.Seekable implements IO.Writable.Seekable {
		
		/** Constructor. */
		@SafeVarargs
		public <T extends IO.Readable.Seekable & IO.Writable.Seekable> ReadWrite(
			String description, T... ios
		) {
			super(description, ios);
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
			return super.writeSync(buffer);
		}

		@Override
		public AsyncSupplier<Integer, IOException> writeAsync(
			long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
		) {
			return super.writeAsync(pos, buffer, ondone);
		}

		@Override
		public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return super.writeAsync(buffer, ondone);
		}
	}
	
	
	protected abstract void nextIOSync() throws IOException;
	
	protected abstract void previousIOSync() throws IOException;
	
	@SuppressWarnings("rawtypes")
	protected abstract void nextIOAsync(Runnable ondone, IAsync<IOException> onerror, Consumer rp);
	
	@SuppressWarnings("rawtypes")
	protected abstract void previousIOAsync(Runnable ondone, IAsync<IOException> onerror, Consumer rp);
	
	protected void nextIOSyncStream() {
		ioIndex++;
		posInIO = 0;
	}
	
	protected void previousIOSyncStream() {
		ioIndex--;
		posInIO = sizes.get(ioIndex).longValue();
	}
	
	protected void nextIOAsyncStream(Runnable ondone) {
		ioIndex++;
		posInIO = 0;
		ondone.run();
	}
	
	protected void previousIOAsyncStream(Runnable ondone) {
		ioIndex--;
		posInIO = sizes.get(ioIndex).longValue();
		ondone.run();
	}
	
	protected void nextIOSyncSeekable() throws IOException {
		ioIndex++;
		posInIO = 0;
		if (ioIndex == ios.size()) return;
		((IO.Readable.Seekable)ios.get(ioIndex)).seekSync(SeekType.FROM_BEGINNING, 0);
	}
	
	protected void previousIOSyncSeekable() throws IOException {
		ioIndex--;
		posInIO = sizes.get(ioIndex).longValue();
		((IO.Readable.Seekable)ios.get(ioIndex)).seekSync(SeekType.FROM_END, 0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void nextIOAsyncSeekable(Runnable ondone, IAsync<IOException> onerror, Consumer rp) {
		ioIndex++;
		posInIO = 0;
		AsyncSupplier<Long, IOException> seek = ((IO.Readable.Seekable)ios.get(ioIndex)).seekAsync(SeekType.FROM_BEGINNING, 0);
		seek.onDone(() -> {
			if (seek.hasError()) {
				if (rp != null) rp.accept(new Pair<Object,IOException>(null, seek.getError()));
				onerror.error(seek.getError());
			} else {
				ondone.run();
			}
		});
		operation(seek);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void previousIOAsyncSeekable(Runnable ondone, IAsync<IOException> onerror, Consumer rp) {
		ioIndex--;
		posInIO = sizes.get(ioIndex).longValue();
		AsyncSupplier<Long, IOException> seek = ((IO.Readable.Seekable)ios.get(ioIndex)).seekAsync(SeekType.FROM_END, 0);
		seek.onDone(() -> {
			if (seek.hasError()) {
				if (rp != null) rp.accept(new Pair<Object,IOException>(null, seek.getError()));
				onerror.error(seek.getError());
			} else {
				ondone.run();
			}
		});
		operation(seek);
	}

	protected int readSync(ByteBuffer buffer) throws IOException {
		if (ioIndex == ios.size())
			return -1;
		IO.Readable io = (IO.Readable)ios.get(ioIndex);
		int nb = io.readSync(buffer);
		if (nb <= 0) {
			if (sizes.get(ioIndex) == null)
				sizes.set(ioIndex, Long.valueOf(posInIO));
			nextIOSync();
			return readSync(buffer);
		}
		posInIO += nb;
		pos += nb;
		return nb;
	}
	
	protected int readFullySync(ByteBuffer buffer) throws IOException {
		return IOUtil.readFully((IO.Readable)this, buffer);
	}
	
	protected AsyncSupplier<Integer, IOException> readFullySyncIfPossible(
		ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
	) {
		if (ioIndex == ios.size()) return IOUtil.success(Integer.valueOf(-1), ondone);
		IO.Readable.Buffered io = (IO.Readable.Buffered)ios.get(ioIndex);
		AsyncSupplier<Integer, IOException> r = io.readFullySyncIfPossible(buffer);
		if (r.isDone()) {
			if (!r.isSuccessful()) {
				if (ondone != null && r.hasError()) ondone.accept(new Pair<>(null, r.getError()));
				return r;
			}
			int nb = r.getResult().intValue();
			if (nb <= 0) {
				if (sizes.get(ioIndex) == null)
					sizes.set(ioIndex, Long.valueOf(posInIO));
				return readFullyAsync(buffer, ondone);
			}
			posInIO += nb;
			pos += nb;
			if (!buffer.hasRemaining()) {
				if (ondone != null) ondone.accept(new Pair<>(r.getResult(), null));
				return r;
			}
			return continueSyncIfPossible(nb, buffer, ondone);
		}
		AsyncSupplier<Integer, IOException> r2 = new AsyncSupplier<>();
		IOUtil.listenOnDone(r, nb -> {
			int n = nb.intValue();
			if (n > 0) {
				posInIO += n;
				pos += n;
			}
			if (!buffer.hasRemaining()) {
				IOUtil.success(nb, r2, ondone);
			} else {
				continueReadAsync(n, buffer, r2, ondone);
			}
		}, r2, ondone);
		return r2;
	}
	
	private AsyncSupplier<Integer, IOException> continueSyncIfPossible(
		int nbDone,
		ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
	) {
		AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
		readFullySyncIfPossible(buffer, res -> {
			if (ondone == null) return;
			if (res.getValue1() == null) ondone.accept(res);
			else {
				int n = res.getValue1().intValue();
				if (n < 0) n = nbDone;
				else n = nbDone + n;
				ondone.accept(new Pair<>(Integer.valueOf(n), null));
			}
		}).onDone(nb2 -> {
			int n = nb2.intValue();
			if (n < 0) n = nbDone;
			else n = nbDone + n;
			result.unblockSuccess(Integer.valueOf(n));
		}, result);
		return result;
	}
	
	private void continueReadAsync(
		int nbDone, ByteBuffer buffer,
		AsyncSupplier<Integer, IOException> result, Consumer<Pair<Integer, IOException>> ondone
	) {
		readFullyAsync(buffer, res -> {
			if (ondone == null) return;
			if (res.getValue1() == null) ondone.accept(res);
			else {
				int n1 = nbDone;
				if (n1 < 0) n1 = 0;
				int n2 = res.getValue1().intValue();
				if (n2 < 0) n2 = n1;
				else n2 += n1;
				ondone.accept(new Pair<>(Integer.valueOf(n2), null));
			}
		}).onDone(nb2 -> {
			int n1 = nbDone;
			if (n1 < 0) n1 = 0;
			int n2 = nb2.intValue();
			if (n2 < 0) n2 = n1;
			else n2 += n1;
			result.unblockSuccess(Integer.valueOf(n2));
		}, result);

	}
	
	protected int readAsync() throws IOException {
		if (ioIndex == ios.size())
			return -1;
		IO.Readable.Buffered io = (IO.Readable.Buffered)ios.get(ioIndex);
		int i = io.readAsync();
		if (i == -1) {
			if (sizes.get(ioIndex) == null)
				sizes.set(ioIndex, Long.valueOf(posInIO));
			nextIOSync();
			return readAsync();
		}
		if (i == -2)
			return -2;
		posInIO++;
		pos++;
		return i;
	}

	protected AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		if (ioIndex == ios.size()) return IOUtil.success(Integer.valueOf(-1), ondone);
		AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
		IO.Readable io = (IO.Readable)ios.get(ioIndex);
		AsyncSupplier<Integer, IOException> read = io.readAsync(buffer);
		operation(read).listen(new IOUtil.RecursiveAsyncSupplierListener<Integer>((nb, that) -> {
			if (nb.intValue() <= 0) {
				if (sizes.get(ioIndex) == null)
					sizes.set(ioIndex, Long.valueOf(posInIO));
				if (ioIndex == ios.size() - 1) {
					ioIndex++;
					posInIO = 0;
					IOUtil.success(Integer.valueOf(-1), result, ondone);
					return;
				}
				nextIOAsync(() -> readAsync(buffer, ondone).forward(result), result, ondone);
				return;
			}
			posInIO += nb.intValue();
			pos += nb.intValue();
			if (ondone != null) ondone.accept(new Pair<>(nb, null));
			result.unblockSuccess(nb);
		}, result, ondone));
		return result;
	}

	protected AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsync((IO.Readable)this, buffer, ondone);
	}
	
	protected AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		if (ioIndex == ios.size()) return IOUtil.success(null, ondone);
		IO.Readable.Buffered io = (IO.Readable.Buffered)ios.get(ioIndex);
		AsyncSupplier<ByteBuffer, IOException> result = new AsyncSupplier<>();
		AsyncSupplier<ByteBuffer, IOException> read = io.readNextBufferAsync();
		operation(read).listen(new IOUtil.RecursiveAsyncSupplierListener<ByteBuffer>((buf, that) -> {
			if (buf == null) {
				if (sizes.get(ioIndex) == null)
					sizes.set(ioIndex, Long.valueOf(posInIO));
				if (ioIndex == ios.size() - 1) {
					ioIndex++;
					posInIO = 0;
					IOUtil.success(null, result, ondone);
					return;
				}
				nextIOAsync(() -> readNextBufferAsync(ondone).forward(result), result, ondone);
				return;
			}
			posInIO += buf.remaining();
			pos += buf.remaining();
			if (ondone != null) ondone.accept(new Pair<>(buf, null));
			result.unblockSuccess(buf);
		}, result, ondone));
		return result;
	}


	protected long skipSync(long n) throws IOException {
		if (n == 0) return 0;
		if (n > 0) {
			if (ioIndex == ios.size())
				return 0;
			IO.Readable io = (IO.Readable)ios.get(ioIndex);
			long nb = io.skipSync(n);
			posInIO += nb;
			pos += nb;
			if (nb == n)
				return n;
			if (sizes.get(ioIndex) == null)
				sizes.set(ioIndex, Long.valueOf(posInIO));
			nextIOSync();
			return nb + skipSync(n - nb);
		}
		if (!(this instanceof IO.Readable.Seekable))
			return 0;
		if (posInIO == 0) {
			if (ioIndex == 0)
				return 0;
			previousIOSync();
			return skipSync(n);
		}
		IO.Readable io = (IO.Readable)ios.get(ioIndex);
		long nb = io.skipSync(n);
		if (nb == 0)
			return 0;
		posInIO += nb;
		pos += nb;
		if (nb == n)
			return n;
		if (posInIO == 0) {
			if (ioIndex == 0)
				return nb;
			previousIOSync();
		}
		return nb + skipSync(n - nb);
	}

	protected AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		if (n == 0) return IOUtil.success(Long.valueOf(0), ondone);
		if (n > 0) {
			if (ioIndex == ios.size()) return IOUtil.success(Long.valueOf(0), ondone);
			IO.Readable io = (IO.Readable)ios.get(ioIndex);
			MutableLong done = new MutableLong(0);
			AsyncSupplier<Long, IOException> skip = io.skipAsync(n);
			AsyncSupplier<Long, IOException> result = new AsyncSupplier<>();
			operation(skip).listen(new IOUtil.RecursiveAsyncSupplierListener<Long>((nb, that) -> {
				posInIO += nb.longValue();
				pos += nb.intValue();
				done.add(nb.longValue());
				if (done.get() == n) {
					IOUtil.success(Long.valueOf(n), result, ondone);
					return;
				}
				if (sizes.get(ioIndex) == null)
					sizes.set(ioIndex, Long.valueOf(posInIO));
				if (ioIndex == ios.size() - 1) {
					ioIndex++;
					posInIO = 0;
					IOUtil.success(Long.valueOf(done.get()), result, ondone);
					return;
				}
				nextIOAsync(() -> operation(((IO.Readable)ios.get(ioIndex)).skipAsync(n - done.get(), null)).listen(that),
					result, ondone);
			}, result, ondone));
			return result;
		}
		if (!(this instanceof IO.Readable.Seekable)) return IOUtil.success(Long.valueOf(0), ondone);
		if (posInIO == 0) {
			if (ioIndex == 0) return IOUtil.success(Long.valueOf(0), ondone);
			AsyncSupplier<Long, IOException> result = new AsyncSupplier<>();
			previousIOAsync(() -> skipAsync(n, ondone).forward(result), result, ondone);
			return result;
		}
		IO.Readable io = (IO.Readable)ios.get(ioIndex);
		AsyncSupplier<Long, IOException> skip = io.skipAsync(n);
		MutableLong done = new MutableLong(0);
		AsyncSupplier<Long, IOException> result = new AsyncSupplier<>();
		operation(skip).listen(new Listener<Long, IOException>() {
			@Override
			public void ready(Long nb) {
				posInIO += nb.longValue();
				pos += nb.intValue();
				done.add(nb.longValue());
				if (done.get() == n) {
					IOUtil.success(Long.valueOf(n), result, ondone);
					return;
				}
				if (ioIndex == 0) {
					IOUtil.success(Long.valueOf(done.get()), result, ondone);
					return;
				}
				Listener<Long, IOException> l = this;
				previousIOAsync(() ->
					((IO.Readable)ios.get(ioIndex)).skipAsync(n - done.get(), null).listen(l),
				result, ondone);
			}
			
			@Override
			public void error(IOException error) { IOUtil.error(error, result, ondone); }
			
			@Override
			public void cancelled(CancelException event) { result.unblockCancel(event); }
		});
		return result;
	}
	
	protected int read() throws IOException {
		if (ioIndex == ios.size()) return -1;
		IO.Readable.Buffered io = (IO.Readable.Buffered)ios.get(ioIndex);
		int i = io.read();
		if (i < 0) {
			if (sizes.get(ioIndex) == null)
				sizes.set(ioIndex, Long.valueOf(posInIO));
			nextIOSync();
			return read();
		}
		posInIO++;
		pos++;
		return i;
	}
	
	protected int read(byte[] buf, int off, int len) throws IOException {
		return readFullySync(ByteBuffer.wrap(buf,off,len));
	}

	protected int readFully(byte[] buffer) throws IOException {
		return readFullySync(ByteBuffer.wrap(buffer));
	}
	
	protected int skip(int n) throws IOException {
		return (int)skipSync(n);
	}
	
	protected IAsync<IOException> canStartReading() {
		if (ioIndex == ios.size()) return new Async<>(true);
		return ((IO.Readable)ios.get(ioIndex)).canStartReading();
	}
	
	protected IAsync<IOException> canStartWriting() {
		if (ioIndex == ios.size()) return new Async<>(true);
		return ((IO.Writable)ios.get(ioIndex)).canStartWriting();
	}

	
	protected long getSizeSync() throws IOException {
		long total = 0;
		for (IO io : ios)
			total += ((IO.KnownSize)io).getSizeSync();
		return total;
	}
	
	protected AsyncSupplier<Long, IOException> getSizeAsync() {
		@SuppressWarnings("unchecked")
		AsyncSupplier<Long, IOException>[] getSizes = new AsyncSupplier[ios.size()];
		for (int i = 0; i < ios.size(); ++i)
			getSizes[i] = ((IO.KnownSize)ios.get(i)).getSizeAsync();
		JoinPoint<IOException> jp = JoinPoint.fromSimilarError(getSizes);
		AsyncSupplier<Long, IOException> result = new AsyncSupplier<>();
		operation(jp).onDone(
			() -> {
				long total = 0;
				for (int i = 0; i < getSizes.length; ++i)
					total += getSizes[i].getResult().longValue();
				result.unblockSuccess(Long.valueOf(total));
			},
			result
		);
		return result;
	}
	
	
	protected long getPosition() {
		return pos;
	}

	protected long seekSync(SeekType type, long move) throws IOException {
		switch (type) {
		case FROM_CURRENT:
			move += pos;
			break;
		case FROM_BEGINNING: break;
		case FROM_END:
			long p = 0;
			for (int i = 0; i < ios.size(); ++i) {
				if (sizes.get(i) == null) {
					IO.Readable.Seekable io = (IO.Readable.Seekable)ios.get(i);
					sizes.set(i,  Long.valueOf(io.seekSync(SeekType.FROM_END, 0)));
				}
				p += sizes.get(i).longValue();
			}
			move = p - move;
			break;
		default: break;
		}
		if (move < 0) move = 0;
		pos = 0;
		ioIndex = 0;
		posInIO = 0;
		if (move == 0) {
			if (!ios.isEmpty())
				((IO.Readable.Seekable)ios.get(0)).seekSync(SeekType.FROM_BEGINNING, 0);
			return 0;
		}
		while (ioIndex < ios.size()) {
			Long s = sizes.get(ioIndex);
			if (s == null) {
				IO.Readable.Seekable io = (IO.Readable.Seekable)ios.get(ioIndex);
				s = Long.valueOf(io.seekSync(SeekType.FROM_END, 0));
				sizes.set(ioIndex, s);
			}
			if (pos + s.longValue() > move) {
				posInIO = move - pos;
				pos = move;
				((IO.Readable.Seekable)ios.get(ioIndex)).seekSync(SeekType.FROM_BEGINNING, posInIO);
				return move;
			}
			pos += s.longValue();
			ioIndex++;
		}
		return pos;
	}

	protected AsyncSupplier<Long, IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		return operation(IOUtil.seekAsyncUsingSync((IO.Readable.Seekable)this, type, move, ondone).getOutput());
	}

	// skip checkstyle: OverloadMethodsDeclarationOrder
	protected int readSync(long pos, ByteBuffer buffer) throws IOException {
		long p = 0;
		int i = 0;
		while (i < ios.size()) {
			Long s = sizes.get(i);
			if (s == null) {
				IO.Readable.Seekable io = (IO.Readable.Seekable)ios.get(i);
				s = Long.valueOf(io.seekSync(SeekType.FROM_END, 0));
				sizes.set(i,  s);
			}
			if (p + s.longValue() > pos) {
				IO.Readable.Seekable io = (IO.Readable.Seekable)ios.get(i);
				return io.readSync(pos - p, buffer);
			}
			p += s.longValue();
			i++;
		}
		return -1;
	}

	protected AsyncSupplier<Integer, IOException> readAsync(
		long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		long p = 0;
		int i = 0;
		while (i < ios.size()) {
			Long s = sizes.get(i);
			if (s == null) {
				IO.Readable.Seekable io = (IO.Readable.Seekable)ios.get(i);
				AsyncSupplier<Long, IOException> seek = io.seekAsync(SeekType.FROM_END, 0);
				if (!seek.isDone()) {
					AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
					int ii = i;
					seek.thenStart(new Task.Cpu.FromRunnable("LinkedIO.readAsync", getPriority(), () -> {
						sizes.set(ii, seek.getResult());
						readAsync(pos, buffer, ondone).forward(result);
					}), result);
					return operation(result);
				}
				if (seek.hasError())
					return IOUtil.error(seek.getError(), ondone);
				s = seek.getResult();
				sizes.set(i, s);
			}
			if (p + s.longValue() > pos) {
				IO.Readable.Seekable io = (IO.Readable.Seekable)ios.get(i);
				return operation(io.readAsync(pos - p, buffer, ondone));
			}
			p += s.longValue();
			i++;
		}
		if (ondone != null) ondone.accept(new Pair<>(Integer.valueOf(-1), null));
		return new AsyncSupplier<>(Integer.valueOf(-1), null);
	}

	protected int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		return IOUtil.readFullySync((IO.Readable.Seekable)this, pos, buffer);
	}

	protected AsyncSupplier<Integer, IOException> readFullyAsync(
		long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		return operation(IOUtil.readFullyAsync((IO.Readable.Seekable)this, pos, buffer, ondone));
	}

	protected int writeSync(ByteBuffer buffer) throws IOException {
		int done = 0;
		while (ioIndex < ios.size()) {
			Long s = sizes.get(ioIndex);
			IO.Writable.Seekable io = (IO.Writable.Seekable)ios.get(ioIndex);
			if (s == null) {
				s = Long.valueOf(io.seekSync(SeekType.FROM_END, 0));
				sizes.set(ioIndex,  s);
				io.seekSync(SeekType.FROM_BEGINNING, posInIO);
			}
			if (posInIO < s.longValue()) {
				int len = (int)(s.longValue() - posInIO);
				int limit = buffer.limit();
				if (buffer.remaining() > len)
					buffer.limit(limit - (buffer.remaining() - len));
				int nb = io.writeSync(buffer);
				buffer.limit(limit);
				posInIO += nb;
				done += nb;
				pos += nb;
				if (!buffer.hasRemaining())
					return done;
			}
			nextIOSync();
		}
		return done;
	}

	protected int writeSync(long pos, ByteBuffer buffer) throws IOException {
		long p = 0;
		int i = 0;
		int done = 0;
		while (i < ios.size()) {
			Long s = sizes.get(i);
			if (s == null) {
				IO.Writable.Seekable io = (IO.Writable.Seekable)ios.get(i);
				s = Long.valueOf(io.seekSync(SeekType.FROM_END, 0));
				sizes.set(i,  s);
			}
			if (p + s.longValue() > pos) {
				IO.Writable.Seekable io = (IO.Writable.Seekable)ios.get(i);
				int len = (int)(s.longValue() - (pos - p));
				int limit = buffer.limit();
				if (buffer.remaining() > len)
					buffer.limit(limit - (buffer.remaining() - len));
				int nb = io.writeSync(pos - p, buffer);
				buffer.limit(limit);
				done += nb;
				pos += nb;
				if (!buffer.hasRemaining())
					return done;
			}
			p += s.longValue();
			i++;
		}
		return done;
	}

	protected AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
		writeAsync(buffer, 0, result, ondone);
		return operation(result);
	}
	
	private void writeAsync(
		ByteBuffer buffer, int done, AsyncSupplier<Integer, IOException> result, Consumer<Pair<Integer, IOException>> ondone
	) {
		if (ioIndex == ios.size()) {
			IOUtil.success(Integer.valueOf(done), result, ondone);
			return;
		}
		IO.Writable.Seekable io = (IO.Writable.Seekable)ios.get(ioIndex);
		Long s = sizes.get(ioIndex);
		if (s == null) {
			AsyncSupplier<Long, IOException> seek = io.seekAsync(SeekType.FROM_END, 0);
			if (!seek.isDone()) {
				seek.thenStart(new Task.Cpu.FromRunnable(WRITE_ASYNC_TASK_DESCRIPTION, getPriority(), () -> {
					if (seek.hasError())
						IOUtil.error(seek.getError(), result, ondone);
					else {
						sizes.set(ioIndex, seek.getResult());
						writeAsync(buffer, done, result, ondone);
					}
				}), true);
				return;
			}
			s = seek.getResult();
			sizes.set(ioIndex, s);
		}
		if (posInIO == s.longValue()) {
			nextIOAsync(() -> writeAsync(buffer, done, result, ondone), result, ondone);
			return;
		}
		int len = (int)(s.longValue() - posInIO);
		int limit = buffer.limit();
		if (buffer.remaining() > len)
			buffer.limit(limit - (buffer.remaining() - len));
		AsyncSupplier<Long, IOException> seek = io.seekAsync(SeekType.FROM_BEGINNING, posInIO);
		if (seek.isDone())
			writeAsync(io, limit, buffer, done, result, ondone);
		else
			seek.onDone(() -> {
				if (seek.hasError())
					IOUtil.error(seek.getError(), result, ondone);
				else
					writeAsync(io, limit, buffer, done, result, ondone);
			});
	}
	
	private void writeAsync(
		IO.Writable.Seekable io, int limit, ByteBuffer buffer, int done,
		AsyncSupplier<Integer, IOException> result, Consumer<Pair<Integer, IOException>> ondone
	) {
		AsyncSupplier<Integer, IOException> write = io.writeAsync(buffer);
		write.thenStart(new Task.Cpu.FromRunnable(WRITE_ASYNC_TASK_DESCRIPTION, getPriority(), () -> {
			buffer.limit(limit);
			if (write.hasError()) {
				IOUtil.error(write.getError(), result, ondone);
				return;
			}
			int nb = write.getResult().intValue();
			posInIO += nb;
			pos += nb;
			if (!buffer.hasRemaining() || ioIndex == ios.size() - 1) {
				IOUtil.success(Integer.valueOf(done + nb), result, ondone);
				return;
			}
			writeAsync(buffer, done + nb, result, ondone);
		}), true);
	}

	protected AsyncSupplier<Integer, IOException> writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		long p = 0;
		int i = 0;
		while (i < ios.size()) {
			Long s = sizes.get(i);
			if (s == null) {
				IO.Writable.Seekable io = (IO.Writable.Seekable)ios.get(i);
				AsyncSupplier<Long, IOException> seek = io.seekAsync(SeekType.FROM_END, 0);
				if (!seek.isDone()) {
					AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
					int ii = i;
					seek.thenStart(new Task.Cpu.FromRunnable(WRITE_ASYNC_TASK_DESCRIPTION, getPriority(), () -> {
						sizes.set(ii, seek.getResult());
						writeAsync(pos, buffer, ondone).forward(result);
					}), result);
					return result;
				}
				s = seek.getResult();
				sizes.set(i, s);
			}
			if (p + s.longValue() > pos) {
				AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
				writeAsync(i, p, pos, 0, buffer, result, ondone);
				return result;
			}
			p += s.longValue();
			i++;
		}
		if (ondone != null) ondone.accept(new Pair<>(Integer.valueOf(-1), null));
		return new AsyncSupplier<>(Integer.valueOf(-1), null);
	}
	
	private void writeAsync(
		int i, long p, long pos, int done, ByteBuffer buffer, AsyncSupplier<Integer, IOException> result,
		Consumer<Pair<Integer, IOException>> ondone
	) {
		IO.Writable.Seekable io = (IO.Writable.Seekable)ios.get(i);
		Long s = sizes.get(i);
		if (s == null) {
			AsyncSupplier<Long, IOException> seek = io.seekAsync(SeekType.FROM_END, 0);
			if (!seek.isDone()) {
				seek.thenStart(new Task.Cpu.FromRunnable(WRITE_ASYNC_TASK_DESCRIPTION, getPriority(), () -> {
					sizes.set(i, seek.getResult());
					writeAsync(i, p, pos, done, buffer, result, ondone);
				}), result);
				return;
			}
			s = seek.getResult();
			sizes.set(i, s);
		}
		int len = (int)(s.longValue() - (pos - p));
		int limit = buffer.limit();
		if (buffer.remaining() > len)
			buffer.limit(limit - (buffer.remaining() - len));
		AsyncSupplier<Integer, IOException> write = io.writeAsync(pos - p, buffer);
		long ioSize = s.longValue();
		write.onDone(() -> {
			buffer.limit(limit);
			if (write.hasError()) {
				IOUtil.error(write.getError(), result, ondone);
				return;
			}
			int nb = write.getResult().intValue();
			if (!buffer.hasRemaining() || i == ios.size() - 1) {
				IOUtil.success(Integer.valueOf(done + nb), result, ondone);
				return;
			}
			new Task.Cpu.FromRunnable(WRITE_ASYNC_TASK_DESCRIPTION, getPriority(), () ->
				writeAsync(i + 1, p + ioSize, p + ioSize, done + nb, buffer, result, ondone)
			).start();
		});
	}
	
}
