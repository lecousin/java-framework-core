package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Implement an empty Readable IO, with Buffered, Seekable and KnownSize capabilities.
 */
public class EmptyReadable extends ConcurrentCloseable implements IO.Readable, IO.KnownSize, IO.Readable.Buffered, IO.Readable.Seekable {

	/** Constructor. */
	public EmptyReadable(String description, byte priority) {
		this.description = description;
		this.priority = priority;
	}
	
	private String description;
	private byte priority;
	
	@Override
	public String getSourceDescription() {
		return description;
	}

	@Override
	public IO getWrappedIO() {
		return null;
	}

	@Override
	public byte getPriority() {
		return priority;
	}

	@Override
	public void setPriority(byte priority) {
		this.priority = priority;
	}

	@Override
	public TaskManager getTaskManager() {
		return Threading.getCPUTaskManager();
	}

	@Override
	public int read() {
		return -1;
	}

	@Override
	public int read(byte[] buffer, int offset, int len) {
		return 0;
	}

	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		return new SynchronizationPoint<>(true);
	}

	@Override
	public long getPosition() {
		return 0;
	}

	@Override
	public long seekSync(SeekType type, long move) {
		return 0;
	}

	@Override
	public AsyncWork<Long, IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long, IOException>> ondone) {
		if (ondone != null)
			ondone.accept(new Pair<>(Long.valueOf(0), null));
		return new AsyncWork<>(Long.valueOf(0), null);
	}

	@Override
	public int readSync(long pos, ByteBuffer buffer) {
		return 0;
	}

	@Override
	public int readSync(ByteBuffer buffer) {
		return 0;
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		if (ondone != null) ondone.accept(new Pair<>(Integer.valueOf(-1), null));
		return new AsyncWork<>(Integer.valueOf(-1), null);
	}
	
	@Override
	public int readAsync() {
		return -1;
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		if (ondone != null)
			ondone.accept(new Pair<>(Integer.valueOf(0), null));
		return new AsyncWork<>(Integer.valueOf(0), null);
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		if (ondone != null)
			ondone.accept(new Pair<>(Integer.valueOf(0), null));
		return new AsyncWork<>(Integer.valueOf(0), null);
	}

	@Override
	public int readFullySync(long pos, ByteBuffer buffer) {
		return 0;
	}

	@Override
	public int readFullySync(ByteBuffer buffer) {
		return 0;
	}

	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return readAsync(buffer, ondone);
	}

	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return readAsync(buffer, ondone);
	}

	@Override
	public int readFully(byte[] buffer) {
		return 0;
	}

	@Override
	public int skip(int skip) {
		return 0;
	}

	@Override
	public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		if (ondone != null)
			ondone.accept(new Pair<>(null, null));
		return new AsyncWork<>(null, null);
	}

	@Override
	public long getSizeSync() {
		return 0;
	}

	@Override
	public AsyncWork<Long, IOException> getSizeAsync() {
		return new AsyncWork<>(Long.valueOf(0), null);
	}

	@Override
	public long skipSync(long n) {
		return 0;
	}

	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
		if (ondone != null)
			ondone.accept(new Pair<>(Long.valueOf(0), null));
		return new AsyncWork<>(Long.valueOf(0), null);
	}

	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		return null;
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		ondone.unblock();
	}

}
