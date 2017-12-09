package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Add the KnownSize capability: to determine the size, we can seek to the end and get the position.
 */
public class ReadableSeekableToDeterminedSize implements IO.Readable.Seekable, IO.KnownSize {
	
	/** Constructor. */
	public ReadableSeekableToDeterminedSize(IO.Readable.Seekable io) {
		this.io = io;
	}
	
	private IO.Readable.Seekable io;

	@Override
	public String getSourceDescription() {
		return io.getSourceDescription();
	}

	@Override
	public IO getWrappedIO() {
		return io.getWrappedIO();
	}

	@Override
	public void close() throws IOException {
		io.close();
	}

	@Override
	public ISynchronizationPoint<IOException> closeAsync() {
		return io.closeAsync();
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		return io.canStartReading();
	}

	@Override
	public void onclose(Runnable listener) {
		io.onclose(listener);
	}
	
	@Override
	public void lockClose() {
		io.lockClose();
	}
	
	@Override
	public void unlockClose() {
		io.unlockClose();
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
	public int readSync(long pos, ByteBuffer buffer) throws IOException {
		return io.readSync(pos, buffer);
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return io.readAsync(buffer, ondone);
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return io.readAsync(pos, buffer, ondone);
	}

	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return io.readFullySync(buffer);
	}

	@Override
	public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		return io.readFullySync(pos, buffer);
	}

	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return io.readFullyAsync(buffer, ondone);
	}

	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return io.readFullyAsync(pos, buffer, ondone);
	}

	@Override
	public long getPosition() throws IOException {
		return io.getPosition();
	}

	@Override
	public long seekSync(SeekType type, long move) throws IOException {
		return io.seekSync(type, move);
	}

	@Override
	public AsyncWork<Long, IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		return io.seekAsync(type, move, ondone);
	}

	@Override
	public long skipSync(long n) throws IOException {
		return io.skipSync(n);
	}

	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		return io.skipAsync(n, ondone);
	}

	@Override
	public long getSizeSync() throws IOException {
		long pos = io.getPosition();
		io.seekSync(SeekType.FROM_END, 0);
		long size = io.getPosition();
		io.seekSync(SeekType.FROM_BEGINNING, pos);
		return size;
	}

	@Override
	public AsyncWork<Long, IOException> getSizeAsync() {
		Task<Long,IOException> task = new Task<Long,IOException>(io.getTaskManager(), io.getSourceDescription(), io.getPriority()) {
			@Override
			public Long run() throws IOException {
				return Long.valueOf(getSizeSync());
			}
		};
		task.start();
		return task.getOutput();
	}
	
	

}
