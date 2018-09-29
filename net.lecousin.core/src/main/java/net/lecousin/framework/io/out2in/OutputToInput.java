package net.lecousin.framework.io.out2in;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.LockPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/** Implementation of IO.OutputToInput using the given IO. */
public class OutputToInput extends ConcurrentCloseable implements IO.OutputToInput, IO.Writable, IO.Readable.Seekable {

	/** Constructor. */
	public <T extends IO.Writable.Seekable & IO.Readable.Seekable> OutputToInput(T io, String sourceDescription) {
		this.io = io;
		this.sourceDescription = sourceDescription;
	}
	
	private IO io;
	private String sourceDescription;
	private boolean eof = false;
	private LockPoint<IOException> lock = new LockPoint<>();
	private long writePos = 0;
	private long readPos = 0;
	private LockPoint<NoException> lockIO = new LockPoint<>();

	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		eof = true;
		lock.error(new EOFException());
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		io = null;
		ondone.unblock();
	}
	
	@Override
	public byte getPriority() { return io != null ? io.getPriority() : Task.PRIORITY_NORMAL; }
	
	@Override
	public void setPriority(byte priority) { io.setPriority(priority); }
	
	@Override
	public String getSourceDescription() {
		return "OutputToInput from " + sourceDescription;
	}
	
	@Override
	public TaskManager getTaskManager() {
		return Threading.getCPUTaskManager();
	}
	
	@Override
	public IO getWrappedIO() {
		return io;
	}
	
	@Override
	public void endOfData() {
		eof = true;
		lock.error(new EOFException());
	}
	
	@Override
	public void signalErrorBeforeEndOfData(IOException error) {
		lock.error(error);
		lockIO.unlock();
	}
	
	@Override
	public boolean isFullDataAvailable() {
		return eof;
	}
	
	@Override
	public long getAvailableDataSize() {
		try { if (io instanceof IO.KnownSize) return ((IO.KnownSize)io).getSizeSync(); }
		catch (Throwable t) { /* ignore */ }
		return -1;
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartWriting() {
		return ((IO.Writable)io).canStartWriting();
	}
	
	@Override
	public int writeSync(ByteBuffer buffer) throws IOException {
		int nb;
		lockIO.lock();
		nb = ((IO.Writable.Seekable)io).writeSync(writePos, buffer);
		writePos += nb;
		lockIO.unlock();
		lock.unlock();
		return nb;
	}
	
	@Override
	public AsyncWork<Integer, IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		AsyncWork<Integer, IOException> result = new AsyncWork<>();
		operation(new Task.Cpu<Void, NoException>("OutputToInput.writeAsync", getPriority()) {
			@Override
			public Void run() {
				lockIO.lock();
				AsyncWork<Integer, IOException> write = ((IO.Writable.Seekable)io).writeAsync(writePos, buffer,
					new RunnableWithParameter<Pair<Integer,IOException>>() {
						@Override
						public void run(Pair<Integer, IOException> param) {
							if (param.getValue1() != null) {
								writePos += param.getValue1().intValue();
								lock.unlock();
								if (ondone != null) ondone.run(param);
							} else {
								if (ondone != null) ondone.run(param);
								lock.error(param.getValue2());
							}
						}
					});
				write.forwardCancel(lock);
				write.listenInline(result);
				lockIO.unlock();
				return null;
			}
		}).start();
		return operation(result);
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		if (eof) return new SynchronizationPoint<>(true);
		if (lock.hasError()) return lock;
		if (readPos < writePos) return new SynchronizationPoint<>(true);
		return lock;
	}
	
	@Override
	public int readSync(long pos, ByteBuffer buffer) throws IOException {
		if (lock.hasError() && !eof)
			throw new IOException("An error occured during the transfer of data", lock.getError());
		while (pos >= writePos) {
			if (eof) return -1;
			if (lock.hasError() && !eof)
				throw new IOException("An error occured during the transfer of data", lock.getError());
			lock.lock();
		}
		int nb;
		lockIO.lock();
		nb = ((IO.Readable.Seekable)io).readSync(pos, buffer);
		lockIO.unlock();
		return nb;
	}
	
	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		int nb = readSync(readPos, buffer);
		if (nb > 0) readPos += nb;
		return nb;
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		return IOUtil.readFullySync(this, pos, buffer);
	}
	
	@Override
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return readAsync(readPos, buffer, (res) -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				readPos += res.getValue1().intValue();
			if (ondone != null) ondone.run(res);
		});
	}
	
	@Override
	public AsyncWork<Integer, IOException> readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		if (lock.hasError() && !eof) {
			IOException e = new IOException("An error occured during the transfer of data", lock.getError());
			if (ondone != null) ondone.run(new Pair<>(null, e));
			return new AsyncWork<Integer, IOException>(null, e);
		}
		if (pos >= writePos) {
			if (eof) {
				if (pos >= writePos) {
					if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
					return new AsyncWork<Integer, IOException>(Integer.valueOf(-1), null);
				}
			}
			AsyncWork<Integer, IOException> result = new AsyncWork<>();
			lock.listenAsync(operation(new Task.Cpu<Integer, IOException>("OutputToInput.readAsync", io.getPriority()) {
				@Override
				public Integer run() throws IOException {
					try {
						Integer nb = Integer.valueOf(readSync(pos, buffer));
						if (ondone != null) ondone.run(new Pair<>(nb, null));
						result.unblockSuccess(nb);
						return nb;
					} catch (IOException e) {
						if (ondone != null) ondone.run(new Pair<>(null, e));
						result.unblockError(e);
						throw e;
					}
				}
			}), true);
			return result;
		}
		AsyncWork<Integer, IOException> result = new AsyncWork<>();
		new Task.Cpu<Void, NoException>("OutputToInput.readAsync", io.getPriority()) {
			@Override
			public Void run() {
				lockIO.lock();
				((IO.Readable.Seekable)io).readAsync(pos, buffer, ondone).listenInline(result);
				lockIO.unlock();
				return null;
			}
		}.start();
		return operation(result);
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsync(this, buffer, ondone));
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsync(this, pos, buffer, ondone));
	}
	
	@Override
	public long skipSync(long n) throws IOException {
		if (n == 0) return 0;
		if (n < 0) {
			if (readPos + n < 0)
				n = -readPos;
			readPos += n;
			return n;
		}
		if (readPos + n > writePos) {
			if (lock.hasError() && !eof)
				throw new IOException("An error occured during the transfer of data", lock.getError());
			while (readPos + n > writePos) {
				if (eof) {
					n = writePos - readPos;
					readPos = writePos;
					return n;
				}
				if (lock.hasError() && !eof)
					throw new IOException("An error occured during the transfer of data", lock.getError());
				lock.lock();
			}
		}
		readPos += n;
		return n;
	}
	
	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		if (n <= 0 || readPos + n <= writePos) {
			try {
				Long r = Long.valueOf(skipSync(n));
				if (ondone != null) ondone.run(new Pair<>(r, null));
				return new AsyncWork<Long, IOException>(r, null);
			} catch (IOException e) {
				if (ondone != null) ondone.run(new Pair<>(null, e));
				return new AsyncWork<>(null, e);
			}
		}
		if (eof) {
			long m = writePos - readPos;
			if (m > n) m = n;
			readPos += m;
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(m), null));
			return new AsyncWork<Long, IOException>(Long.valueOf(m), null);
		}
		AsyncWork<Long, IOException> result = new AsyncWork<>();
		lock.listenAsync(operation(new Task.Cpu<Long, IOException>("OutputToInput.skipAsync", io.getPriority()) {
			@Override
			public Long run() throws IOException {
				try {
					Long nb = Long.valueOf(skipSync(n));
					if (ondone != null) ondone.run(new Pair<>(nb, null));
					result.unblockSuccess(nb);
					return nb;
				} catch (IOException e) {
					if (ondone != null) ondone.run(new Pair<>(null, e));
					result.unblockError(e);
					throw e;
				}
			}
		}), true);
		return result;
	}	

	@Override
	public long getPosition() {
		return readPos;
	}

	@Override
	public long seekSync(SeekType type, long move) throws IOException {
		switch (type) {
		case FROM_BEGINNING:
			readPos = 0;
			skipSync(move);
			return readPos;
		case FROM_CURRENT:
			skipSync(move);
			return readPos;
		case FROM_END:
			while (!eof && !lock.hasError()) {
				lock.lock();
			}
			if (eof) {
				readPos = writePos;
				skipSync(-move);
				return readPos;
			}
			throw new IOException("An error occured during the transfer of data", lock.getError());
		default:
			throw new IOException("Unknown SeekType " + type);
		}
	}

	@Override
	public AsyncWork<Long, IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		AsyncWork<Long, IOException> res = new AsyncWork<>();
		switch (type) {
		case FROM_BEGINNING:
			readPos = 0;
			skipAsync(move).listenInline(() -> {
				if (ondone != null) ondone.run(new Pair<>(Long.valueOf(readPos), null));
				res.unblockSuccess(Long.valueOf(readPos));
			}, res);
			return res;
		case FROM_CURRENT:
			skipAsync(move).listenInline(() -> {
				if (ondone != null) ondone.run(new Pair<>(Long.valueOf(readPos), null));
				res.unblockSuccess(Long.valueOf(readPos));
			}, res);
			return res;
		case FROM_END:
			if (lock.hasError() && !eof) {
				IOException e = new IOException("An error occured during the transfer of data", lock.getError());
				if (ondone != null) ondone.run(new Pair<>(null, e));
				return new AsyncWork<Long, IOException>(null, e);
			}
			if (eof) {
				if (move <= 0)
					readPos = writePos;
				else
					readPos = writePos - move;
				if (readPos < 0) readPos = 0;
				Long r = Long.valueOf(readPos);
				if (ondone != null) ondone.run(new Pair<>(r, null));
				return new AsyncWork<Long, IOException>(r, null);
			}
			AsyncWork<Long, IOException> result = new AsyncWork<Long, IOException>();
			lock.listenAsync(operation(new Task.Cpu<Void, NoException>("OutputToInput.seekAsync", io.getPriority()) {
				@Override
				public Void run() {
					try {
						Long nb = Long.valueOf(seekSync(type, move));
						if (ondone != null) ondone.run(new Pair<>(nb, null));
						result.unblockSuccess(nb);
					} catch (IOException e) {
						if (ondone != null) ondone.run(new Pair<>(null, e));
						result.unblockError(e);
					}
					return null;
				}
			}), true);
			return result;
		default:
			return new AsyncWork<>(null, new IOException("Unknown SeekType " + type));
		}
	}

}
