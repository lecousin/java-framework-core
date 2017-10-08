package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;

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
	}
	
	private IO.Readable io;
	private byte[] buffer;
	private int len = 0;
	private int pos;
	private boolean useReadFully;
	private boolean eof = false;
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		return new SynchronizationPoint<>(true);
	}

	@Override
	public int getRemainingBufferedSize() {
		return len - pos;
	}

	@Override
	public int getMaxBufferedSize() {
		return buffer.length;
	}

	private void fillBuffer() throws IOException {
		AsyncWork<Integer,IOException> result;
		if (useReadFully)
			result = io.readFullyAsync(ByteBuffer.wrap(buffer));
		else
			result = io.readAsync(ByteBuffer.wrap(buffer));
		result.block(0);
		if (result.hasError())
			throw result.getError();
		if (result.isCancelled())
			throw new IOException("Operation cancelled", result.getCancelEvent());
		len = result.getResult().intValue();
		if (len <= 0) {
			len = 0;
			eof = true;
		} else if (useReadFully && len < buffer.length)
			eof = true;
		pos = 0;
	}

	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		if (pos == len) {
			if (eof) return 0;
			fillBuffer();
			return readSync(buffer);
		}
		int l = buffer.remaining();
		if (l > len - pos) l = len - pos;
		buffer.put(this.buffer, pos, l);
		pos += l;
		return l;
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readAsyncUsingSync(this, buffer, ondone).getSynch();
	}

	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}

	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsynch(this, buffer, ondone);
	}

	@Override
	public long skipSync(long n) throws IOException {
		if (n <= 0)
			return 0;
		long nb = 0;
		while (n > 0) {
			if (pos == len) {
				if (eof) return nb;
				fillBuffer();
			}
			int l = len - pos;
			if (l > n) l = (int)n;
			pos += l;
			nb += l;
			n -= l;
		}
		return nb;
	}

	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		return IOUtil.skipAsync(this, n, ondone).getSynch();
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
			fillBuffer();
			return read();
		}
		return buffer[pos++] & 0xFF;
	}

	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		if (pos == this.len) {
			if (eof) return 0;
			fillBuffer();
			return read(buffer, offset, len);
		}
		int l = len;
		if (l > this.len - pos) l = this.len - pos;
		System.arraycopy(this.buffer, pos, buffer, offset, l);
		pos += l;
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
			public ByteBuffer run() throws IOException {
				while (pos == len) {
					if (eof) return null;
					fillBuffer();
				}
				ByteBuffer buf = ByteBuffer.allocate(len - pos);
				buf.put(buffer, pos, len - pos);
				pos = len;
				buf.flip();
				return buf;
			}
		};
		task.start();
		return task.getSynch();
	}
	
	@Override
	protected ISynchronizationPoint<IOException> closeIO() {
		buffer = null;
		ISynchronizationPoint<IOException> res = io.closeAsync();
		io = null;
		return res;
	}
	
}
