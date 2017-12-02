package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * This is the less efficient buffered readable implementation: it uses a single buffer, which is filled only when first byte
 * is read (and so is blocking).
 * <br/>
 * It should not be used except in very specific situations, when we don't know the size of the data, and cannot predict
 * if more bytes are available until we actually need to read more data.
 * <br/>
 * An example of usage is in case of data split over several files. When reaching the end of a file, we need to open the next
 * file, but the next file may require to ask the user to insert a new disc. In that case, if we try to fill a buffer in
 * advance we may ask the user to insert a disc that does not exist, because we don't know in advance the number of discs.
 */
public class SingleBufferReadable extends IO.AbstractIO implements IO.Readable.Buffered {

	/** Constructor. */
	public SingleBufferReadable(IO.Readable io, int bufferSize, boolean useReadFully) {
		this.io = io;
		this.buffer = new byte[bufferSize];
		this.useReadFully = useReadFully;
		fillNextBuffer();
	}
	
	private IO.Readable io;
	private byte[] buffer;
	private int len = 0;
	private int pos;
	private boolean useReadFully;
	private boolean eof = false;
	private AsyncWork<Integer, IOException> reading;
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		return new SynchronizationPoint<>(true);
	}

	private void fillNextBuffer() {
		if (useReadFully)
			reading = io.readFullyAsync(ByteBuffer.wrap(buffer), (result) -> {
				if (result.getValue1() == null) return;
				len = result.getValue1().intValue();
				if (len <= 0) {
					len = 0;
					eof = true;
				} else if (len < buffer.length)
					eof = true;
				pos = 0;
			});
		else
			reading = io.readAsync(ByteBuffer.wrap(buffer), (result) -> {
				len = result.getValue1().intValue();
				if (len <= 0) {
					len = 0;
					eof = true;
				}
				pos = 0;
			});
	}
	
	private void waitBufferSync() throws IOException {
		reading.block(0);
		if (reading.hasError()) throw reading.getError();
		if (reading.isCancelled()) throw new IOException("Read cancelled", reading.getCancelEvent());
	}

	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		if (pos == len) {
			if (eof) return 0;
			waitBufferSync();
			return readSync(buffer);
		}
		int l = buffer.remaining();
		if (l > len - pos) l = len - pos;
		buffer.put(this.buffer, pos, l);
		pos += l;
		if (pos == len)
			fillNextBuffer();
		return l;
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readAsyncUsingSync(this, buffer, ondone).getSynch();
	}
	
	@Override
	public int readAsync() {
		if (pos == len) {
			if (eof) return -1;
			return -2;
		}
		int c = buffer[pos++] & 0xFF;
		if (pos == len) fillNextBuffer();
		return c;
	}

	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}

	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsync(this, buffer, ondone);
	}

	@Override
	public long skipSync(long n) throws IOException {
		if (n <= 0)
			return 0;
		long nb = 0;
		while (n > 0) {
			if (pos == len) {
				if (eof) return nb;
				waitBufferSync();
			}
			int l = len - pos;
			if (l > n) l = (int)n;
			pos += l;
			nb += l;
			n -= l;
			if (pos == len)
				fillNextBuffer();
		}
		return nb;
	}

	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		return IOUtil.skipAsyncByReading(this, n, ondone);
	}

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
	public int read() throws IOException {
		if (pos == len) {
			if (eof) return -1;
			waitBufferSync();
			return read();
		}
		int c = buffer[pos++] & 0xFF;
		if (pos == len) fillNextBuffer();
		return c;
	}

	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		if (pos == this.len) {
			if (eof) return 0;
			waitBufferSync();
			return read(buffer, offset, len);
		}
		int l = len;
		if (l > this.len - pos) l = this.len - pos;
		System.arraycopy(this.buffer, pos, buffer, offset, l);
		pos += l;
		if (pos == this.len) fillNextBuffer();
		return l;
	}

	@Override
	public int readFully(byte[] buffer) throws IOException {
		return IOUtil.readFully(this, ByteBuffer.wrap(buffer));
	}

	@Override
	public int skip(int skip) throws IOException {
		return (int)skipSync(skip);
	}

	@Override
	public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		if (pos == len && eof) {
			if (ondone != null) ondone.run(new Pair<>(null, null));
			return new AsyncWork<>(null, null);
		}
		Task.Cpu<ByteBuffer, IOException> task = new Task.Cpu<ByteBuffer, IOException>("Read next buffer", getPriority(), ondone) {
			@Override
			public ByteBuffer run() throws IOException, CancelException {
				if (reading.hasError()) throw reading.getError();
				if (reading.isCancelled()) throw reading.getCancelEvent();
				if (pos == len && eof) return null;
				ByteBuffer buf = ByteBuffer.allocate(len - pos);
				buf.put(buffer, pos, len - pos);
				pos = len;
				fillNextBuffer();
				buf.flip();
				return buf;
			}
		};
		task.startOn(reading, true);
		return task.getSynch();
	}
	
	@Override
	protected ISynchronizationPoint<IOException> closeIO() {
		buffer = null;
		if (!reading.isUnblocked()) reading.cancel(new CancelException("IO closed"));
		ISynchronizationPoint<IOException> res = io.closeAsync();
		io = null;
		return res;
	}
	
}
