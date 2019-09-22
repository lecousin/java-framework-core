package net.lecousin.framework.io.buffering;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.concurrent.tasks.drives.RemoveFileTask;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Convert a Readable into a Seekable.
 * <br/>
 * As the source IO cannot seek, this implementation uses a temporary file
 * in which data is written each time data is read from the source.
 * Thus when seeking backward, we still have the data in the temporary file making the seek operation possible.
 * When seeking forward, data is first read from the source, then written to the temporary file
 * so we can go backward later.
 * <br/>
 * The temporary file is then wrapped into a BufferedIO so the operation are still efficient because
 * data is kept into memory as much as possible, and write operations are delayed.
 * <br/>
 * A first read operation is started in background by the constructor, so a read operation can start as soon as possible.
 * <br/>
 * If the source IO is not Buffered, as PreBufferedReadable wraps it in order to read data in advance.
 */
public class ReadableToSeekable extends ConcurrentCloseable implements IO.Readable.Seekable, IO.Readable.Buffered, IO.KnownSize {

	/** Constructor. */
	public ReadableToSeekable(IO.Readable io, int bufferSize) throws IOException {
		this.bufferSize = bufferSize;
		if (io instanceof IO.KnownSize)
			knownSize = ((IO.KnownSize)io).getSizeSync();
		if (!(io instanceof IO.Readable.Buffered))
			this.io = new PreBufferedReadable(io, 512, io.getPriority(), bufferSize, io.getPriority(), 3);
		else
			this.io = (IO.Readable.Buffered)io;
		file = File.createTempFile("net.lecousin.framework", "ReedableToSeekable");
		file.deleteOnExit();
		FileIO.ReadWrite fio = new FileIO.ReadWrite(file, io.getPriority());
		buffered = new BufferedIO.ReadWrite(fio, 0L, 512, bufferSize, false);
		readNextBuffer();
	}
	
	private IO.Readable.Buffered io;
	private long ioPos = 0;
	private long pos = 0;
	private long knownSize = -1;
	private File file;
	private BufferedIO.ReadWrite buffered;
	private AsyncWork<Boolean,IOException> buffering;
	private int bufferSize;
	
	@Override
	public String getSourceDescription() { return io.getSourceDescription(); }

	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		JoinPoint<Exception> jp = new JoinPoint<>();
		// TODO buffered.cancelAll();
		buffering.unblockCancel(new CancelException("IO closed"));
		jp.addToJoin(buffered.closeAsync());
		jp.addToJoin(io.closeAsync());
		jp.start();
		jp.listenAsync(new RemoveFileTask(file, Task.PRIORITY_LOW), true);
		return jp;
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		io = null;
		buffered = null;
		ondone.unblock();
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		if (pos < ioPos)
			return buffered.canStartReading();
		return buffering;
	}
	
	@Override
	public long getPosition() {
		return pos;
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
	public IO getWrappedIO() {
		return io;
	}
	
	@Override
	public TaskManager getTaskManager() {
		return Threading.getCPUTaskManager();
	}
	
	@Override
	public long getSizeSync() throws IOException {
		if (knownSize >= 0) return knownSize;
		do {
			synchronized (this) {
				if (knownSize >= 0) return knownSize;
				if (buffering.isUnblocked())
					readNextBuffer();
			}
			buffering.block(0);
			if (buffering.isCancelled()) throw new IOException("IO closed");
			if (!buffering.isSuccessful()) throw buffering.getError();
		} while (true);
	}
	
	@Override
	public AsyncWork<Long, IOException> getSizeAsync() {
		AsyncWork<Long, IOException> sp = new AsyncWork<>();
		if (knownSize >= 0) {
			sp.unblockSuccess(Long.valueOf(knownSize));
			return sp;
		}
		AsyncWork<Long,IOException> seek = seekAsync(SeekType.FROM_END, 0);
		seek.listenInline(result -> sp.unblockSuccess(Long.valueOf(knownSize)), sp);
		sp.onCancel(seek::unblockCancel);
		return operation(seek);
	}

	private void readNextBuffer() {
		if (knownSize == ioPos) {
			buffering = new AsyncWork<>(Boolean.TRUE, null);
			return;
		}
		buffering = new AsyncWork<>();
		ByteBuffer buffer = ByteBuffer.allocate(8192);
		AsyncWork<Integer,IOException> read = io.readFullyAsync(buffer);
		operation(read).listenInline(result -> {
			if (result.intValue() <= 0) {
				knownSize = ioPos;
				buffering.unblockSuccess(Boolean.TRUE);
				return;
			}
			buffer.flip();
			AsyncWork<Integer,IOException> write = buffered.writeAsync(ioPos, buffer);
			operation(write).listenInline(result2 -> {
				int nb = result2.intValue();
				if (nb != result.intValue()) {
					buffering.unblockError(
						new IOException("Only " + nb + " bytes written in BufferedIO, "
								+ result.intValue() + " expected")
					);
					return;
				}
				synchronized (ReadableToSeekable.this) {
					ioPos += nb;
					if (nb < 8192) {
						if (knownSize >= 0 && knownSize != ioPos)
							LCCore.getApplication().getDefaultLogger().error(
								"Unexpected end on ReadableToSeekable: known size is " + knownSize
								+ ", but end reached at " + ioPos + " (" + nb + "/8192 bytes read)");
						knownSize = ioPos;
					}
				}
				buffering.unblockSuccess(Boolean.valueOf(nb < 8192));
			}, buffering);
		}, buffering);
	}

	/** Wait to be able to read at least one byte at the given position, return false if this is beyond the end. */
	@SuppressWarnings("squid:S2583") // false positive because situation may change during block
	private boolean waitPosition(long pos) throws IOException {
		while (pos >= ioPos) {
			if (knownSize == ioPos) return false;
			buffering.block(0);
			if (pos < ioPos) break;
			synchronized (this) {
				if (buffering.isUnblocked()) {
					if (buffering.isCancelled()) return false;
					if (!buffering.isSuccessful())
						throw buffering.getError();
					if (buffering.getResult().booleanValue()) {
						if (pos >= ioPos) return false;
						break;
					}
					readNextBuffer();
				}
			}
		}
		return true;
	}
	
	private AsyncWork<Boolean,IOException> bufferizeTo(long pos) {
		if (pos < ioPos) return null;
		synchronized (this) {
			if (pos < ioPos) return null;
			if (!buffering.isUnblocked()) {
				if (pos < ioPos + 8192)
					return buffering; // will be ok with the next buffer
			} else {
				boolean nextEnough = pos < ioPos + 8192; // will be ok with the next buffer
				readNextBuffer();
				if (nextEnough)
					return buffering;
			}
		}
		// we need to read more
		AsyncWork<Boolean,IOException> sp = new AsyncWork<>();
		buffering.listenInline(result -> {
			if (result.booleanValue()) {
				// end reached
				sp.unblockSuccess(null);
				return;
			}
			operation(new Task.Cpu<Void,NoException>("Bufferize in ReadableToSeekable", io.getPriority()) {
				@Override
				public Void run() {
					synchronized (ReadableToSeekable.this) {
						if (buffering.isUnblocked())
							readNextBuffer();
					}
					AsyncWork<Boolean,IOException> next = bufferizeTo(pos);
					if (next == null)
						sp.unblockSuccess(null);
					else
						next.listenInline(result -> sp.unblockSuccess(null), sp);
					return null;
				}
			}.start());
		}, sp);
		return operation(sp);
	}
	
	@Override
	public int read() throws IOException {
		if (!waitPosition(pos))
			return -1;
		byte[] b = new byte[1];
		buffered.readSync(pos, ByteBuffer.wrap(b));
		pos++;
		return b[0] & 0xFF;
	}
	
	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		if (!waitPosition(pos))
			return -1;
		waitPosition(pos + len - 1);
		int nb = buffered.readFullySync(pos, ByteBuffer.wrap(buffer, offset, len));
		pos += nb;
		return nb;
	}
	
	@Override
	public int readFully(byte[] buffer) throws IOException {
		return read(buffer, 0, buffer.length);
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		if (knownSize >= 0 && pos >= knownSize)
			return IOUtil.success(Integer.valueOf(-1), ondone);
		if (pos >= ioPos) {
			bufferizeTo(pos);
			return readFullyAsync(buffer, ondone);
		}
		return buffered.readFullySyncIfPossible(pos, buffer, ondone);
	}
	
	@Override
	public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return readAsync(pos, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				pos += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}
	
	@Override
	public int readAsync() throws IOException {
		if (knownSize >= 0 && pos >= knownSize) return -1;
		if (pos >= ioPos) {
			bufferizeTo(pos);
			return -2;
		}
		byte[] b = new byte[1];
		AsyncWork<Integer, IOException> r = buffered.readFullySyncIfPossible(pos, ByteBuffer.wrap(b), null);
		if (!r.isUnblocked()) return -2;
		if (r.hasError()) throw r.getError();
		if (r.isCancelled()) return -1;
		if (r.getResult().intValue() <= 0) return -1;
		pos++;
		return b[0] & 0xFF;
	}
	
	@Override
	public AsyncWork<Integer,IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		AsyncWork<Integer,IOException> result = new AsyncWork<>();
		AsyncWork<Boolean,IOException> bufferize = bufferizeTo(pos);
		Runnable onBuffered = () -> {
			if (bufferize != null) {
				if (bufferize.isCancelled()) {
					result.unblockCancel(bufferize.getCancelEvent());
					return;
				}
				if (bufferize.hasError()) {
					IOUtil.error(bufferize.getError(), result, ondone);
					return;
				}
			}
			AsyncWork<Integer,IOException> read = buffered.readAsync(pos, buffer);
			IOUtil.listenOnDone(read, res -> {
				int nb = res.intValue();
				if (nb <= 0 && knownSize >= 0 && pos < knownSize)
					LCCore.getApplication().getDefaultLogger().error(
						"Unexpected end on ReadableToSeekable: no byte read at "
						+ pos + " but knownSize is " + knownSize);
				if (ondone != null) ondone.accept(new Pair<>(res, null));
				result.unblockSuccess(res);
			}, result, ondone);
		};
		if (bufferize == null)
			onBuffered.run();
		else
			bufferize.listenInline(onBuffered);
		return operation(result);
	}

	@Override
	public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		AsyncWork<ByteBuffer,IOException> result = new AsyncWork<>();
		AsyncWork<Boolean,IOException> bufferize = bufferizeTo(pos);
		Task.Cpu<Void, NoException> task = new Task.Cpu<Void, NoException>("Read next buffer", getPriority()) {
			@Override
			public Void run() {
				if (bufferize != null) {
					if (bufferize.isCancelled()) {
						result.unblockCancel(bufferize.getCancelEvent());
						return null;
					}
					if (!bufferize.isSuccessful()) {
						IOUtil.error(bufferize.getError(), result, ondone);
						return null;
					}
				}
				ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
				AsyncWork<Integer,IOException> read = buffered.readAsync(pos, buffer);
				IOUtil.listenOnDone(read, res -> {
					int nb = res.intValue();
					if (nb > 0) {
						ReadableToSeekable.this.pos = pos + nb;
						buffer.flip();
						if (ondone != null) ondone.accept(new Pair<>(buffer, null));
						result.unblockSuccess(buffer);
					} else {
						if (ondone != null) ondone.accept(new Pair<>(null, null));
						result.unblockSuccess(null);
					}
				}, result, ondone);
				return null;
			}
		};
		operation(task);
		if (bufferize == null)
			task.start();
		else
			bufferize.listenAsync(task, true);
		return result;
	}
	
	@Override
	public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsync(this, buffer, ondone);
	}
	
	@Override
	public AsyncWork<Integer,IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsync(this, pos, buffer, ondone);
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		int nb = readFullySync(pos, buffer);
		if (nb > 0) pos += nb;
		return nb;
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		int len = buffer.remaining();
		if (knownSize != -1 && pos + len > knownSize)
			len = (int)(knownSize - pos);
		waitPosition(pos + len - 1);
		if (pos >= ioPos) return -1;
		return buffered.readFullySync(pos, buffer);
	}
	
	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		int nb = readSync(pos, buffer);
		if (nb > 0) pos += nb;
		return nb;
	}
	
	@Override
	public int readSync(long pos, ByteBuffer buffer) throws IOException {
		if (!waitPosition(pos)) return -1;
		return buffered.readSync(pos, buffer);
	}
	
	@Override
	public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		Task<Long,IOException> task = new Task.Cpu<Long,IOException>("Seeking in non-seekable", io.getPriority(), ondone) {
			@Override
			public Long run() throws IOException {
				return Long.valueOf(seekSync(type, move));
			}
		};
		operation(task.start());
		return task.getOutput();
	}
	
	@Override
	@SuppressWarnings("squid:S1199") // nested block
	public long seekSync(SeekType type, long move) throws IOException {
		switch (type) {
		case FROM_BEGINNING:
			if (move < 0) move = 0;
			waitPosition(move);
			if (move > ioPos)
				pos = ioPos;
			else
				pos = move;
			break;
		case FROM_CURRENT: {
			long newPos = pos + move;
			if (newPos < 0) newPos = 0;
			waitPosition(newPos);
			if (newPos > ioPos)
				pos = ioPos;
			else
				pos = newPos;
			break;
		}
		case FROM_END: {
			if (move < 0) move = 0;
			if (knownSize < 0) getSizeSync();
			long newPos = knownSize - move;
			if (newPos < 0) newPos = 0;
			if (newPos > knownSize) newPos = knownSize;
			waitPosition(newPos);
			if (newPos > ioPos)
				pos = ioPos;
			else
				pos = newPos;
			break;
		}
		default: break;
		}
		return pos;
	}
	
	@Override
	public int skip(int skip) throws IOException {
		return (int)skipSync(skip);
	}
	
	@Override
	public long skipSync(long n) throws IOException {
		long prevPos = pos;
		long newPos = pos + n;
		if (newPos < 0) newPos = 0;
		waitPosition(newPos);
		if (newPos > ioPos)
			pos = ioPos;
		else
			pos = newPos;
		return pos - prevPos;
	}
	
	@Override
	public AsyncWork<Long,IOException> skipAsync(long move, Consumer<Pair<Long,IOException>> ondone) {
		Task<Long,IOException> task = new Task.Cpu<Long,IOException>("Seeking in non-seekable", io.getPriority(), ondone) {
			@Override
			public Long run() throws IOException {
				return Long.valueOf(skipSync(move));
			}
		};
		operation(task.start());
		return task.getOutput();
	}

}
