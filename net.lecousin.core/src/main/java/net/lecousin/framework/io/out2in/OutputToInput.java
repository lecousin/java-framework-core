package net.lecousin.framework.io.out2in;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.LockPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.AbstractIO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/** Implementation of IO.OutputToInput using the given IO. */
public class OutputToInput extends AbstractIO implements IO.OutputToInput, IO.Writable, IO.Readable.Seekable {

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
	public void endOfData() {
		eof = true;
		lock.unlock();
	}
	
	@Override
	public void signalErrorBeforeEndOfData(IOException error) {
		lock.error(error);
		lockIO.unlock();
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
		new Task.Cpu<Void, NoException>("OutputToInput.writeAsync", getPriority()) {
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
				write.listenCancel(new Listener<CancelException>() {
					@Override
					public void fire(CancelException event) {
						lock.cancel(event);
					}
				});
				write.listenInline(result);
				lockIO.unlock();
				return null;
			}
		}.start();
		return result;
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		if (lock.hasError()) return lock;
		if (readPos < writePos) return new SynchronizationPoint<>(true);
		if (eof) return new SynchronizationPoint<>(true);
		return lock;
	}
	
	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		if (lock.hasError())
			throw new IOException("An error occured during the transfer of data", lock.getError());
		while (readPos >= writePos) {
			if (eof) return -1;
			if (lock.hasError())
				throw new IOException("An error occured during the transfer of data", lock.getError());
			lock.lock(); // TODO if eof or error in the middle, we may stay locked!
		}
		int nb;
		lockIO.lock();
		nb = ((IO.Readable.Seekable)io).readSync(readPos, buffer);
		readPos += nb;
		lockIO.unlock();
		return nb;
	}
	
	@Override
	public int readSync(long pos, ByteBuffer buffer) throws IOException {
		readPos = pos;
		return readSync(buffer);
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		readPos = pos;
		return readFullySync(buffer);
	}
	
	@Override
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return readAsync(readPos, buffer, ondone);
	}
	
	@Override
	public AsyncWork<Integer, IOException> readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		readPos = pos;
		if (lock.hasError()) {
			IOException e = new IOException("An error occured during the transfer of data", lock.getError());
			if (ondone != null) ondone.run(new Pair<>(null, e));
			return new AsyncWork<Integer, IOException>(null, e);
		}
		if (readPos >= writePos) {
			if (eof) {
				if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(-1), null));
				return new AsyncWork<Integer, IOException>(Integer.valueOf(-1), null);
			}
			AsyncWork<Integer, IOException> result = new AsyncWork<>();
			lock.listenAsynch(new Task.Cpu<Integer, IOException>("OutputToInput.readAsync", io.getPriority()) {
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
			}, true);
			return result;
		}
		AsyncWork<Integer, IOException> result = new AsyncWork<>();
		new Task.Cpu<Void, NoException>("OutputToInput.readAsync", io.getPriority()) {
			@Override
			public Void run() {
				lockIO.lock();
				((IO.Readable.Seekable)io).readAsync(readPos, buffer, new RunnableWithParameter<Pair<Integer,IOException>>() {
					@Override
					public void run(Pair<Integer, IOException> param) {
						if (param.getValue1() != null)
							readPos += param.getValue1().intValue();
					}
				}).listenInline(result);
				lockIO.unlock();
				return null;
			}
		}.start();
		return result;
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsynch(this, buffer, ondone);
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsynch(this, pos, buffer, ondone);
	}
	
	@Override
	public long skipSync(long n) {
		readPos += n;
		return n;
	}
	
	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		Long r = Long.valueOf(skipSync(n));
		if (ondone != null) ondone.run(new Pair<>(r, null));
		return new AsyncWork<Long, IOException>(r, null);
	}
	
	@Override
	protected ISynchronizationPoint<IOException> closeIO() {
		eof = true;
		lock.unlock();
		return new SynchronizationPoint<>(true);
	}
	
	@Override
	public byte getPriority() { return io.getPriority(); }
	
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
	public long getPosition() {
		return readPos;
	}

	@Override
	public long seekSync(SeekType type, long move) throws IOException {
		switch (type) {
		case FROM_BEGINNING:
			readPos = move;
			return move;
		case FROM_CURRENT:
			readPos += move;
			return readPos;
		case FROM_END:
			while (!eof && !lock.hasError()) {
				lock.lock();
			}
			if (eof) {
				readPos = writePos - move;
				return readPos;
			}
			throw new IOException("An error occured during the transfer of data", lock.getError());
		default:
			throw new IOException("Unknown SeekType " + type);
		}
	}

	@Override
	public AsyncWork<Long, IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		switch (type) {
		case FROM_BEGINNING:
			readPos = move;
			break;
		case FROM_CURRENT:
			readPos += move;
			break;
		case FROM_END:
			if (lock.hasError()) {
				IOException e = new IOException("An error occured during the transfer of data", lock.getError());
				if (ondone != null) ondone.run(new Pair<>(null, e));
				return new AsyncWork<Long, IOException>(null, e);
			}
			if (eof) {
				readPos = writePos - move;
				break;
			}
			AsyncWork<Long, IOException> result = new AsyncWork<Long, IOException>();
			lock.listenInline(new Runnable() {
				@Override
				public void run() {
					if (lock.hasError()) {
						IOException e = new IOException("An error occured during the transfer of data", lock.getError());
						if (ondone != null) ondone.run(new Pair<>(null, e));
						result.unblockError(e);
					} else if (eof) {
						readPos = writePos - move;
						if (ondone != null) ondone.run(new Pair<>(Long.valueOf(readPos), null));
						result.unblockSuccess(Long.valueOf(readPos));
					} else
						lock.listenInline(this);
				}
			});
			return result;
		default:
			return new AsyncWork<>(null, new IOException("Unknown SeekType " + type));
		}
		Long r = Long.valueOf(readPos);
		if (ondone != null) ondone.run(new Pair<>(r, null));
		return new AsyncWork<Long, IOException>(r, null);
	}

}
