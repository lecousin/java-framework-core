package net.lecousin.framework.io.out2in;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.LockPoint;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.Runnables.SupplierThrows;

/** Implementation of IO.OutputToInput using the given IO. */
public class OutputToInput extends ConcurrentCloseable<IOException> implements IO.OutputToInput, IO.Writable, IO.Readable.Seekable {

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
	protected IAsync<IOException> closeUnderlyingResources() {
		eof = true;
		lock.error(new EOFException());
		return io.closeAsync();
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		io = null;
		ondone.unblock();
	}
	
	@Override
	public Priority getPriority() { return io != null ? io.getPriority() : Priority.NORMAL; }
	
	@Override
	public void setPriority(Priority priority) { io.setPriority(priority); }
	
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
		catch (Exception t) { /* ignore */ }
		return -1;
	}
	
	@Override
	public IAsync<IOException> canStartWriting() {
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
	public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
		operation(Task.cpu("OutputToInput.writeAsync", getPriority(), () -> {
			lockIO.lock();
			AsyncSupplier<Integer, IOException> write = ((IO.Writable.Seekable)io).writeAsync(writePos, buffer, param -> {
				if (param.getValue1() != null) {
					writePos += param.getValue1().intValue();
					lockIO.unlock();
					lock.unlock();
					if (ondone != null) ondone.accept(param);
				} else {
					lockIO.unlock();
					if (ondone != null) ondone.accept(param);
					lock.error(param.getValue2());
				}
			});
			write.onCancel(lock::cancel);
			write.forward(result);
			return null;
		})).start();
		return operation(result);
	}
	
	@Override
	public IAsync<IOException> canStartReading() {
		if (eof) return new Async<>(true);
		if (lock.hasError()) return lock;
		if (readPos < writePos) return new Async<>(true);
		return lock;
	}
	
	@Override
	@SuppressWarnings("squid:S2589") // eof may change in a concurrent operation
	public int readSync(long pos, ByteBuffer buffer) throws IOException {
		if (lock.hasError() && !eof)
			throw new OutputToInputTransferException(lock.getError());
		while (pos >= writePos) {
			if (eof) return -1;
			if (lock.hasError() && !eof)
				throw new OutputToInputTransferException(lock.getError());
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
	public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return readAsync(readPos, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				readPos += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}
	
	@Override
	@SuppressWarnings("squid:S2589") // may change in a concurrent operation
	public AsyncSupplier<Integer, IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		if (lock.hasError() && !eof) {
			IOException e = new OutputToInputTransferException(lock.getError());
			if (ondone != null) ondone.accept(new Pair<>(null, e));
			return new AsyncSupplier<>(null, e);
		}
		if (pos >= writePos) {
			if (eof && pos >= writePos) {
				if (ondone != null) ondone.accept(new Pair<>(Integer.valueOf(-1), null));
				return new AsyncSupplier<>(Integer.valueOf(-1), null);
			}
			AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
			lock.thenStart(operation(
				taskSyncToAsync("OutputToInput.readAsync", result, ondone, () -> Integer.valueOf(readSync(pos, buffer)))), true);
			return result;
		}
		AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
		Task.cpu("OutputToInput.readAsync", io.getPriority(), () -> {
			lockIO.lock();
			AsyncSupplier<Integer, IOException> read = ((IO.Readable.Seekable)io).readAsync(pos, buffer, ondone);
			read.onDone(() -> {
				lockIO.unlock();
				read.forward(result);
			});
			return null;
		}).start();
		return operation(result);
	}
	
	private <T> Task<T, IOException> taskSyncToAsync(
		String description, AsyncSupplier<T, IOException> result, Consumer<Pair<T,IOException>> ondone, SupplierThrows<T, IOException> sync
	) {
		return Task.cpu(description, io.getPriority(), () -> {
			try {
				T res = sync.get();
				if (ondone != null) ondone.accept(new Pair<>(res, null));
				result.unblockSuccess(res);
				return res;
			} catch (IOException e) {
				if (ondone != null) ondone.accept(new Pair<>(null, e));
				result.unblockError(e);
				throw e;
			}
		}).setMaxBlockingTimeInNanoBeforeToLog(1000L * 1000000); // 1s
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsync(this, buffer, ondone));
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsync(this, pos, buffer, ondone));
	}
	
	@Override
	@SuppressWarnings("squid:S2589") // may change in a concurrent operation
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
				throw new OutputToInputTransferException(lock.getError());
			while (readPos + n > writePos) {
				if (eof) {
					n = writePos - readPos;
					readPos = writePos;
					return n;
				}
				if (lock.hasError() && !eof)
					throw new OutputToInputTransferException(lock.getError());
				lock.lock();
			}
		}
		readPos += n;
		return n;
	}
	
	@Override
	public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		if (n <= 0 || readPos + n <= writePos) {
			try {
				Long r = Long.valueOf(skipSync(n));
				if (ondone != null) ondone.accept(new Pair<>(r, null));
				return new AsyncSupplier<>(r, null);
			} catch (IOException e) {
				if (ondone != null) ondone.accept(new Pair<>(null, e));
				return new AsyncSupplier<>(null, e);
			}
		}
		if (eof) {
			long m = writePos - readPos;
			if (m > n) m = n;
			readPos += m;
			if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(m), null));
			return new AsyncSupplier<>(Long.valueOf(m), null);
		}
		AsyncSupplier<Long, IOException> result = new AsyncSupplier<>();
		lock.thenStart(operation(taskSyncToAsync("OutputToInput.skipAsync", result, ondone, () -> Long.valueOf(skipSync(n)))), true);
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
			throw new OutputToInputTransferException(lock.getError());
		default:
			throw new IOException("Unknown SeekType " + type);
		}
	}

	@Override
	public AsyncSupplier<Long, IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		AsyncSupplier<Long, IOException> res = new AsyncSupplier<>();
		switch (type) {
		case FROM_BEGINNING:
			readPos = 0;
			skipAsync(move).onDone(() -> {
				if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(readPos), null));
				res.unblockSuccess(Long.valueOf(readPos));
			}, res);
			return res;
		case FROM_CURRENT:
			skipAsync(move).onDone(() -> {
				if (ondone != null) ondone.accept(new Pair<>(Long.valueOf(readPos), null));
				res.unblockSuccess(Long.valueOf(readPos));
			}, res);
			return res;
		case FROM_END:
			if (lock.hasError() && !eof)
				return IOUtil.error(new OutputToInputTransferException(lock.getError()), ondone);
			if (eof) {
				if (move <= 0)
					readPos = writePos;
				else
					readPos = writePos - move;
				if (readPos < 0) readPos = 0;
				return IOUtil.success(Long.valueOf(readPos), ondone);
			}
			AsyncSupplier<Long, IOException> result = new AsyncSupplier<>();
			lock.thenStart(operation(Task.cpu("OutputToInput.seekAsync", io.getPriority(), () -> {
				try {
					Long nb = Long.valueOf(seekSync(type, move));
					if (ondone != null) ondone.accept(new Pair<>(nb, null));
					result.unblockSuccess(nb);
				} catch (IOException e) {
					if (ondone != null) ondone.accept(new Pair<>(null, e));
					result.unblockError(e);
				}
				return null;
			})), true);
			return result;
		default:
			return new AsyncSupplier<>(null, new IOException("Unknown SeekType " + type));
		}
	}

}
