package net.lecousin.framework.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.AsyncWork.AsyncWorkListener;
import net.lecousin.framework.concurrent.synch.AsyncWork.AsyncWorkListenerReady;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.mutable.MutableLong;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Make several IOs as a single one.
 */
public abstract class LinkedIO extends ConcurrentCloseable implements IO {

	protected LinkedIO(String description, Collection<IO> ios) {
		this.description = description;
		this.ios = new ArrayList<>(ios);
	}
	
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
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		JoinPoint<Exception> jp = new JoinPoint<>();
		for (IO io : ios) jp.addToJoin(io.closeAsync());
		jp.start();
		return jp;
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
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
		
		@SuppressWarnings("unused")
		@Override
		protected void nextIOSync() throws IOException {
			nextIOSyncStream();
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		protected void nextIOAsync(Runnable ondone, ISynchronizationPoint<IOException> onerror, RunnableWithParameter rp) {
			nextIOAsyncStream(ondone);
		}
		
		@SuppressWarnings("unused")
		@Override
		protected void previousIOSync() throws IOException {
			previousIOSyncStream();
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		protected void previousIOAsync(Runnable ondone, ISynchronizationPoint<IOException> onerror, RunnableWithParameter rp) {
			previousIOAsyncStream(ondone);
		}
		
		@Override
		public ISynchronizationPoint<IOException> canStartReading() {
			return super.canStartReading();
		}
		
		@Override
		public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
			return super.readAsync(buffer, ondone);
		}
		
		@Override
		public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
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
		public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
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
			public int readAsync() throws IOException {
				return super.readAsync();
			}
			
			@Override
			public int skip(int n) throws IOException {
				return super.skip(n);
			}
			
			@Override
			public ISynchronizationPoint<IOException> canStartReading() {
				return super.canStartReading();
			}
			
			@Override
			public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
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
				public AsyncWork<Long, IOException> getSizeAsync() {
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
			public AsyncWork<Long, IOException> getSizeAsync() {
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
			protected void nextIOAsync(Runnable ondone, ISynchronizationPoint<IOException> onerror, RunnableWithParameter rp) {
				nextIOAsyncSeekable(ondone, onerror, rp);
			}
			
			@Override
			protected void previousIOSync() throws IOException {
				previousIOSyncSeekable();
			}
			
			@SuppressWarnings("rawtypes")
			@Override
			protected void previousIOAsync(Runnable ondone, ISynchronizationPoint<IOException> onerror, RunnableWithParameter rp) {
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
			public AsyncWork<Long, IOException> seekAsync(
				SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone
			) {
				return super.seekAsync(type, move, ondone);
			}

			@Override
			public int readSync(long pos, ByteBuffer buffer) throws IOException {
				return super.readSync(pos, buffer);
			}

			@Override
			public AsyncWork<Integer, IOException> readAsync(
				long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
			) {
				return super.readAsync(pos, buffer, ondone);
			}

			@Override
			public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
				return super.readFullySync(pos, buffer);
			}

			@Override
			public AsyncWork<Integer, IOException> readFullyAsync(
				long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
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
				public int readAsync() throws IOException {
					return super.readAsync();
				}
				
				@Override
				public int skip(int n) throws IOException {
					return super.skip(n);
				}
				
				@Override
				public ISynchronizationPoint<IOException> canStartReading() {
					return super.canStartReading();
				}
				
				@Override
				public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(
					RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone
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
					public AsyncWork<Long, IOException> getSizeAsync() {
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
				public AsyncWork<Long, IOException> getSizeAsync() {
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
		public ISynchronizationPoint<IOException> canStartWriting() {
			return super.canStartWriting();
		}

		@Override
		public int writeSync(long pos, ByteBuffer buffer) throws IOException {
			return super.writeSync(pos, buffer);
		}

		@Override
		public int writeSync(ByteBuffer buffer) throws IOException {
			return super.writeSync(pos, buffer);
		}

		@Override
		public AsyncWork<Integer, IOException> writeAsync(
			long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone
		) {
			return super.writeAsync(pos, buffer, ondone);
		}

		@Override
		public AsyncWork<Integer, IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
			return super.writeAsync(pos, buffer, ondone);
		}
	}
	
	
	protected abstract void nextIOSync() throws IOException;
	
	protected abstract void previousIOSync() throws IOException;
	
	@SuppressWarnings("rawtypes")
	protected abstract void nextIOAsync(Runnable ondone, ISynchronizationPoint<IOException> onerror, RunnableWithParameter rp);
	
	@SuppressWarnings("rawtypes")
	protected abstract void previousIOAsync(Runnable ondone, ISynchronizationPoint<IOException> onerror, RunnableWithParameter rp);
	
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
	protected void nextIOAsyncSeekable(Runnable ondone, ISynchronizationPoint<IOException> onerror, RunnableWithParameter rp) {
		ioIndex++;
		posInIO = 0;
		AsyncWork<Long, IOException> seek = ((IO.Readable.Seekable)ios.get(ioIndex)).seekAsync(SeekType.FROM_BEGINNING, 0);
		seek.listenInline(() -> {
			if (seek.hasError()) {
				if (rp != null) rp.run(new Pair<Object,IOException>(null, seek.getError()));
				onerror.error(seek.getError());
			} else
				ondone.run();
		});
		operation(seek);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void previousIOAsyncSeekable(Runnable ondone, ISynchronizationPoint<IOException> onerror, RunnableWithParameter rp) {
		ioIndex--;
		posInIO = sizes.get(ioIndex).longValue();
		AsyncWork<Long, IOException> seek = ((IO.Readable.Seekable)ios.get(ioIndex)).seekAsync(SeekType.FROM_END, 0);
		seek.listenInline(() -> {
			if (seek.hasError()) {
				if (rp != null) rp.run(new Pair<Object,IOException>(null, seek.getError()));
				onerror.error(seek.getError());
			} else
				ondone.run();
		});
		operation(seek);
	}

	@SuppressWarnings("resource")
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
	
	@SuppressWarnings("resource")
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

	@SuppressWarnings("resource")
	protected AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		if (ioIndex == ios.size()) {
			if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
			return new AsyncWork<>(Integer.valueOf(-1),null);
		}
		AsyncWork<Integer, IOException> result = new AsyncWork<Integer, IOException>();
		IO.Readable io = (IO.Readable)ios.get(ioIndex);
		AsyncWork<Integer, IOException> read = io.readAsync(buffer);
		operation(read).listenInline(new AsyncWorkListenerReady<Integer, IOException>((nb, that) -> {
			if (nb.intValue() <= 0) {
				if (sizes.get(ioIndex) == null)
					sizes.set(ioIndex, Long.valueOf(posInIO));
				if (ioIndex == ios.size() - 1) {
					ioIndex++;
					posInIO = 0;
					if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
					result.unblockSuccess(Integer.valueOf(-1));
					return;
				}
				nextIOAsync(new Runnable() {
					@Override
					public void run() {
						readAsync(buffer, ondone).listenInline(result);
					}
				}, result, ondone);
				return;
			}
			posInIO += nb.intValue();
			pos += nb.intValue();
			if (ondone != null) ondone.run(new Pair<>(nb, null));
			result.unblockSuccess(nb);
		}, result, ondone));
		return result;
	}

	protected AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsync((IO.Readable)this, buffer, ondone);
	}
	
	@SuppressWarnings("resource")
	protected AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		if (ioIndex == ios.size()) {
			if (ondone != null) ondone.run(new Pair<>(null, null));
			return new AsyncWork<>(null,null);
		}
		IO.Readable.Buffered io = (IO.Readable.Buffered)ios.get(ioIndex);
		AsyncWork<ByteBuffer, IOException> result = new AsyncWork<>();
		AsyncWork<ByteBuffer, IOException> read = io.readNextBufferAsync();
		operation(read).listenInline(new AsyncWorkListenerReady<ByteBuffer, IOException>((buf, that) -> {
			if (buf == null) {
				if (sizes.get(ioIndex) == null)
					sizes.set(ioIndex, Long.valueOf(posInIO));
				if (ioIndex == ios.size() - 1) {
					ioIndex++;
					posInIO = 0;
					if (ondone != null) ondone.run(new Pair<>(null, null));
					result.unblockSuccess(null);
					return;
				}
				nextIOAsync(new Runnable() {
					@Override
					public void run() {
						readNextBufferAsync(ondone).listenInline(result);
					}
				}, result, ondone);
				return;
			}
			posInIO += buf.remaining();
			pos += buf.remaining();
			if (ondone != null) ondone.run(new Pair<>(buf, null));
			result.unblockSuccess(buf);
		}, result, ondone));
		return result;
	}


	@SuppressWarnings("resource")
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

	@SuppressWarnings("resource")
	protected AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		AsyncWork<Long, IOException> result = new AsyncWork<Long, IOException>();
		if (n == 0) {
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(0), null));
			result.unblockSuccess(Long.valueOf(0));
			return result;
		}
		if (n > 0) {
			if (ioIndex == ios.size()) {
				if (ondone != null) ondone.run(new Pair<>(Long.valueOf(0), null));
				result.unblockSuccess(Long.valueOf(0));
				return result;
			}
			IO.Readable io = (IO.Readable)ios.get(ioIndex);
			MutableLong done = new MutableLong(0);
			AsyncWork<Long, IOException> skip = io.skipAsync(n);
			operation(skip).listenInline(new AsyncWorkListenerReady<Long, IOException>((nb, that) -> {
				posInIO += nb.longValue();
				pos += nb.intValue();
				done.add(nb.longValue());
				if (done.get() == n) {
					if (ondone != null) ondone.run(new Pair<>(Long.valueOf(n), null));
					result.unblockSuccess(Long.valueOf(n));
					return;
				}
				if (sizes.get(ioIndex) == null)
					sizes.set(ioIndex, Long.valueOf(posInIO));
				if (ioIndex == ios.size() - 1) {
					ioIndex++;
					posInIO = 0;
					if (ondone != null) ondone.run(new Pair<>(Long.valueOf(done.get()), null));
					result.unblockSuccess(Long.valueOf(done.get()));
					return;
				}
				nextIOAsync(new Runnable() {
					@Override
					public void run() {
						operation(((IO.Readable)ios.get(ioIndex)).skipAsync(n - done.get(), null)).listenInline(that);
					}
				}, result, ondone);
			}, result, ondone));
			return result;
		}
		if (!(this instanceof IO.Readable.Seekable)) {
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(0), null));
			result.unblockSuccess(Long.valueOf(0));
			return result;
		}
		if (posInIO == 0) {
			if (ioIndex == 0) {
				if (ondone != null) ondone.run(new Pair<>(Long.valueOf(0), null));
				result.unblockSuccess(Long.valueOf(0));
				return result;
			}
			previousIOAsync(new Runnable() {
				@Override
				public void run() {
					skipAsync(n, ondone).listenInline(result);
				}
			}, result, ondone);
			return result;
		}
		IO.Readable io = (IO.Readable)ios.get(ioIndex);
		AsyncWork<Long, IOException> skip = io.skipAsync(n);
		MutableLong done = new MutableLong(0);
		operation(skip).listenInline(new AsyncWorkListener<Long, IOException>() {
			@Override
			public void ready(Long nb) {
				posInIO += nb.longValue();
				pos += nb.intValue();
				done.add(nb.longValue());
				if (done.get() == n) {
					if (ondone != null) ondone.run(new Pair<>(Long.valueOf(n), null));
					result.unblockSuccess(Long.valueOf(n));
					return;
				}
				if (ioIndex == 0) {
					if (ondone != null) ondone.run(new Pair<>(Long.valueOf(done.get()), null));
					result.unblockSuccess(Long.valueOf(done.get()));
					return;
				}
				AsyncWorkListener<Long, IOException> l = this;
				previousIOAsync(new Runnable() {
					@Override
					public void run() {
						((IO.Readable)ios.get(ioIndex)).skipAsync(n - done.get(), null).listenInline(l);
					}
				}, result, ondone);
			}
			
			@Override
			public void error(IOException error) {
				if (ondone != null) ondone.run(new Pair<>(null, error));
				result.unblockError(error);
			}
			
			@Override
			public void cancelled(CancelException event) {
				result.unblockCancel(event);
			}
		});
		return result;
	}
	
	@SuppressWarnings("resource")
	protected int read() throws IOException {
		if (ioIndex == ios.size())
			return -1;
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
	
	protected ISynchronizationPoint<IOException> canStartReading() {
		if (ioIndex == ios.size())
			return new SynchronizationPoint<>(true);
		return ((IO.Readable)ios.get(ioIndex)).canStartReading();
	}
	
	protected ISynchronizationPoint<IOException> canStartWriting() {
		if (ioIndex == ios.size())
			return new SynchronizationPoint<>(true);
		return ((IO.Writable)ios.get(ioIndex)).canStartWriting();
	}

	
	protected long getSizeSync() throws IOException {
		long total = 0;
		for (IO io : ios)
			total += ((IO.KnownSize)io).getSizeSync();
		return total;
	}
	
	protected AsyncWork<Long, IOException> getSizeAsync() {
		@SuppressWarnings("unchecked")
		AsyncWork<Long, IOException>[] sizes = new AsyncWork[ios.size()];
		for (int i = 0; i < ios.size(); ++i)
			sizes[i] = ((IO.KnownSize)ios.get(i)).getSizeAsync();
		JoinPoint<IOException> jp = JoinPoint.fromSynchronizationPointsSimilarError(sizes);
		AsyncWork<Long, IOException> result = new AsyncWork<Long, IOException>();
		operation(jp).listenInline(
			() -> {
				long total = 0;
				for (int i = 0; i < sizes.length; ++i)
					total += sizes[i].getResult().longValue();
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
					@SuppressWarnings("resource")
					IO.Readable.Seekable io = (IO.Readable.Seekable)ios.get(i);
					sizes.set(ioIndex,  Long.valueOf(io.seekSync(SeekType.FROM_END, 0)));
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
				@SuppressWarnings("resource")
				IO.Readable.Seekable io = (IO.Readable.Seekable)ios.get(ioIndex);
				sizes.set(ioIndex, s = Long.valueOf(io.seekSync(SeekType.FROM_END, 0)));
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

	protected AsyncWork<Long, IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		return operation(IOUtil.seekAsyncUsingSync((IO.Readable.Seekable)this, type, move, ondone).getOutput());
	}

	// skip checkstyle: OverloadMethodsDeclarationOrder
	@SuppressWarnings("resource")
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
				int nb = io.readSync(pos - p, buffer);
				ioIndex = i;
				posInIO = pos - p + nb;
				this.pos = pos + nb;
				return nb;
			}
			p += s.longValue();
			i++;
		}
		return -1;
	}

	@SuppressWarnings("resource")
	protected AsyncWork<Integer, IOException> readAsync(
		long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		long p = 0;
		int i = 0;
		while (i < ios.size()) {
			Long s = sizes.get(i);
			if (s == null) {
				IO.Readable.Seekable io = (IO.Readable.Seekable)ios.get(i);
				AsyncWork<Long, IOException> seek = io.seekAsync(SeekType.FROM_END, 0);
				if (!seek.isUnblocked()) {
					AsyncWork<Integer, IOException> result = new AsyncWork<>();
					int ii = i;
					seek.listenAsync(new Task.Cpu.FromRunnable("LinkedIO.readAsync", getPriority(), () -> {
						sizes.set(ii, seek.getResult());
						readAsync(pos, buffer, ondone).listenInline(result);
					}), result);
					return result;
				}
				sizes.set(i, s = seek.getResult());
			}
			if (p + s.longValue() > pos) {
				IO.Readable.Seekable io = (IO.Readable.Seekable)ios.get(i);
				ioIndex = i;
				posInIO = pos - p;
				AsyncWork<Integer, IOException> result = io.readAsync(pos - p, buffer, (res) -> {
					if (res.getValue1() != null) {
						posInIO += res.getValue1().intValue();
						LinkedIO.this.pos = pos + res.getValue1().intValue();
					}
					if (ondone != null) ondone.run(res);
				});
				return result;
			}
			p += s.longValue();
			i++;
		}
		if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
		return new AsyncWork<>(Integer.valueOf(-1), null);
	}

	protected int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		return IOUtil.readFullySync((IO.Readable.Seekable)this, pos, buffer);
	}

	protected AsyncWork<Integer, IOException> readFullyAsync(
		long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		return operation(IOUtil.readFullyAsync((IO.Readable.Seekable)this, pos, buffer, ondone));
	}

	@SuppressWarnings("resource")
	protected int writeSync(long pos, ByteBuffer buffer) throws IOException {
		long p = 0;
		int i = 0;
		while (i < ios.size()) {
			Long s = sizes.get(i);
			if (s == null) {
				IO.Writable.Seekable io = (IO.Writable.Seekable)ios.get(i);
				s = Long.valueOf(io.seekSync(SeekType.FROM_END, 0));
				sizes.set(i,  s);
			}
			if (p + s.longValue() > pos) {
				IO.Writable.Seekable io = (IO.Writable.Seekable)ios.get(i);
				int nb = io.writeSync(pos - p, buffer);
				ioIndex = i;
				posInIO = pos - p + nb;
				this.pos = pos + nb;
				return nb;
			}
			p += s.longValue();
			i++;
		}
		return -1;
	}

	@SuppressWarnings("resource")
	protected AsyncWork<Integer, IOException> writeAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
		long p = 0;
		int i = 0;
		while (i < ios.size()) {
			Long s = sizes.get(i);
			if (s == null) {
				IO.Writable.Seekable io = (IO.Writable.Seekable)ios.get(i);
				AsyncWork<Long, IOException> seek = io.seekAsync(SeekType.FROM_END, 0);
				if (!seek.isUnblocked()) {
					AsyncWork<Integer, IOException> result = new AsyncWork<>();
					int ii = i;
					seek.listenAsync(new Task.Cpu.FromRunnable("LinkedIO.readAsync", getPriority(), () -> {
						sizes.set(ii, seek.getResult());
						writeAsync(pos, buffer, ondone).listenInline(result);
					}), result);
					return result;
				}
				sizes.set(i, s = seek.getResult());
			}
			if (p + s.longValue() > pos) {
				IO.Writable.Seekable io = (IO.Writable.Seekable)ios.get(i);
				ioIndex = i;
				posInIO = pos - p;
				AsyncWork<Integer, IOException> result = io.writeAsync(pos - p, buffer, (res) -> {
					if (res.getValue1() != null) {
						posInIO += res.getValue1().intValue();
						LinkedIO.this.pos = pos + res.getValue1().intValue();
					}
					if (ondone != null) ondone.run(res);
				});
				return result;
			}
			p += s.longValue();
			i++;
		}
		if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
		return new AsyncWork<>(Integer.valueOf(-1), null);
	}
	
}
