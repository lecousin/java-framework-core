package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Utility class to implement the Buffered interface even in case buffers cannot be used.
 * An example it cannot be used is in case of a non-seekable IO, and we do not know in advance the size
 * of the data to read. Because we cannot seek, we may not exceed a certain position in the stream.
 */
public class NonBufferedReadableIOAsBuffered extends IO.AbstractIO implements IO.Readable.Buffered {

	/** Constructor. */
	public NonBufferedReadableIOAsBuffered(IO.Readable io) {
		this.io = io;
	}
	
	private IO.Readable io;

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
		return io.getTaskManager();
	}

	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		return io.readSync(buffer);
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return io.readAsync(buffer, ondone);
	}

	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return io.readFullySync(buffer);
	}

	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return io.readFullyAsync(buffer, ondone);
	}

	@Override
	public long skipSync(long n) throws IOException {
		return io.skipSync(n);
	}

	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		return io.skipAsync(n, ondone);
	}

	private byte[] b1 = new byte[1];
	private ByteBuffer bb1 = ByteBuffer.wrap(b1);
	
	@Override
	public int read() throws IOException {
		int nb = io.readSync(bb1);
		if (nb <= 0) return -1;
		return b1[0] & 0xFF;
	}

	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		return io.readSync(ByteBuffer.wrap(buffer, offset, len));
	}

	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		return new SynchronizationPoint<>(true);
	}

	@Override
	public int getRemainingBufferedSize() {
		return 0;
	}

	@Override
	public int getMaxBufferedSize() {
		return 0;
	}

	@Override
	public int readFully(byte[] buffer) throws IOException {
		return io.readFullySync(ByteBuffer.wrap(buffer));
	}

	@Override
	public int skip(int skip) throws IOException {
		return (int)io.skipSync(skip);
	}

	@Override
	protected ISynchronizationPoint<IOException> closeIO() {
		return io.closeAsync();
	}

	@Override
	public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		AsyncWork<ByteBuffer, IOException> result = new AsyncWork<>();
		Task.Cpu<Void, NoException> task = new Task.Cpu<Void, NoException>("Read next buffer", getPriority()) {
			@Override
			public Void run() {
				ByteBuffer buf = ByteBuffer.allocate(4096);
				AsyncWork<Integer, IOException> read = readAsync(buf);
				read.listenInline(new Runnable() {
					@Override
					public void run() {
						if (read.hasError()) {
							if (ondone != null) ondone.run(new Pair<>(null, read.getError()));
							result.unblockError(read.getError());
							return;
						}
						int nb = read.getResult().intValue();
						if (nb <= 0) {
							if (ondone != null) ondone.run(new Pair<>(null, null));
							result.unblockSuccess(null);
							return;
						}
						buf.flip();
						result.unblockSuccess(buf);
					}
				});
				return null;
			}
		};
		task.start();
		return result;
	}
	
}
