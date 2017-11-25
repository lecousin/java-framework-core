package net.lecousin.framework.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * Implement an empty Readable IO, with Buffered, Seekable and KnownSize capabilities.
 */
public class EmptyReadable extends IO.AbstractIO implements IO.Readable, IO.KnownSize, IO.Readable.Buffered, IO.Readable.Seekable {

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
	public AsyncWork<Long, IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long, IOException>> ondone) {
		if (ondone != null)
			ondone.run(new Pair<>(Long.valueOf(0), null));
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
	public int readAsync() {
		return -1;
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
		if (ondone != null)
			ondone.run(new Pair<>(Integer.valueOf(0), null));
		return new AsyncWork<>(Integer.valueOf(0), null);
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
		if (ondone != null)
			ondone.run(new Pair<>(Integer.valueOf(0), null));
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
	public AsyncWork<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
		if (ondone != null)
			ondone.run(new Pair<>(Integer.valueOf(0), null));
		return new AsyncWork<>(Integer.valueOf(0), null);
	}

	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
		if (ondone != null)
			ondone.run(new Pair<>(Integer.valueOf(0), null));
		return new AsyncWork<>(Integer.valueOf(0), null);
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
	public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		if (ondone != null)
			ondone.run(new Pair<>(null, null));
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
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long, IOException>> ondone) {
		if (ondone != null)
			ondone.run(new Pair<>(Long.valueOf(0), null));
		return new AsyncWork<>(Long.valueOf(0), null);
	}

	@Override
	protected ISynchronizationPoint<IOException> closeIO() {
		return new SynchronizationPoint<>(true);
	}

}
