package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.CloseableListenable;
import net.lecousin.framework.util.Pair;

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
	public void close() throws Exception {
		io.close();
	}

	@Override
	public IAsync<IOException> closeAsync() {
		return io.closeAsync();
	}
	
	@Override
	public boolean isClosed() {
		return io.isClosed();
	}
	
	@Override
	public IAsync<IOException> canStartReading() {
		return io.canStartReading();
	}

	@Override
	public void addCloseListener(Runnable listener) {
		io.addCloseListener(listener);
	}
	
	@Override
	public void addCloseListener(Consumer<CloseableListenable> listener) {
		io.addCloseListener(listener);
	}
	
	@Override
	public void removeCloseListener(Runnable listener) {
		io.removeCloseListener(listener);
	}
	
	@Override
	public void removeCloseListener(Consumer<CloseableListenable> listener) {
		io.removeCloseListener(listener);
	}
	
	@Override
	public boolean lockClose() {
		return io.lockClose();
	}
	
	@Override
	public void unlockClose() {
		io.unlockClose();
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
	public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return io.readAsync(buffer, ondone);
	}

	@Override
	public AsyncSupplier<Integer, IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
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
	public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return io.readFullyAsync(buffer, ondone);
	}

	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
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
	public AsyncSupplier<Long, IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		return io.seekAsync(type, move, ondone);
	}

	@Override
	public long skipSync(long n) throws IOException {
		return io.skipSync(n);
	}

	@Override
	public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
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
	public AsyncSupplier<Long, IOException> getSizeAsync() {
		return new Task<>(io.getTaskManager(), io.getSourceDescription(), io.getPriority(),
			() -> Long.valueOf(getSizeSync()), null).start().getOutput();
	}
	
	

}
