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
 * This implementation of buffered readable IO use a simple buffering strategy, with 2 buffers:
 * while the first one is used, the second is filled in background.
 * <br/>
 * Note that buffers are not necessarily fully filled, as the operation used to fill a buffer is readAsync and not readFullyAsync.
 * <br/>
 * The first read operation is started by the constructor.
 * <br/>
 * When a new byte is requested, if the position in the current buffer didn't reach its size, the byte is taken from the current buffer.
 * When reaching the end of the current buffer, a new buffer is requested:<ul>
 * 	<li>If the second buffer is ready, it becomes the current buffer, and a new read operation is started in background
 * 	to fill the next buffer</li>
 *  <li>If the second buffer is not yet ready, the operation blocks until the second buffer is ready.</li>
 * </ul>
 */
public class SimpleBufferedReadable extends IO.AbstractIO implements IO.Readable.Buffered {
	
	/** Constructor. */
	public SimpleBufferedReadable(IO.Readable io, int bufferSize) {
		this.io = io;
		readBuffer = ByteBuffer.allocate(bufferSize);
		readTask = io.readAsync(readBuffer);
		bb = ByteBuffer.allocate(bufferSize);
		buffer = bb.array();
	}
	
	private IO.Readable io;
	private byte[] buffer;
	private int pos = 0;
	private int len = 0;
	private ByteBuffer readBuffer;
	private ByteBuffer bb;
	private AsyncWork<Integer,IOException> readTask;
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		ISynchronizationPoint<IOException> sp = readTask;
		if (sp == null) sp = new SynchronizationPoint<>(true);
		return sp;
	}
	
	@Override
	public TaskManager getTaskManager() { return io.getTaskManager(); }
	
	@Override
	public IO getWrappedIO() { return io; }
	
	@Override
	public String getSourceDescription() { return io.getSourceDescription(); }
	
	@Override
	public byte getPriority() { return io.getPriority(); }
	
	@Override
	public void setPriority(byte priority) { io.setPriority(priority); }
	
	@Override
	protected ISynchronizationPoint<IOException> closeIO() {
		AsyncWork<Integer,IOException> currentRead = readTask;
		if (currentRead != null && !currentRead.isUnblocked()) {
			currentRead.cancel(new CancelException("IO closed"));
			SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
			currentRead.listenInline(new Runnable() {
				@Override
				public void run() {
					readTask = null;
					buffer = null;
					bb = null;
					readBuffer = null;
					io.closeAsync().listenInline(sp);
				}
			});
			return sp;
		}
		buffer = null;
		bb = null;
		readBuffer = null;
		return io.closeAsync();
	}
	
	/** Stop any pending read, and block until they are cancelled or done. */
	public void stop() {
		AsyncWork<Integer,IOException> currentRead = readTask;
		if (currentRead != null && !currentRead.isUnblocked()) {
			currentRead.cancel(new CancelException("SimpleBufferedReadable.stop"));
			currentRead.block(0);
		}
	}
	
	private void fill() throws IOException {
		AsyncWork<Integer,IOException> currentRead = readTask;
		if (currentRead == null)
			return;
		currentRead.block(0);
		if (!currentRead.isSuccessful()) {
			Exception e = currentRead.getError();
			if (e instanceof IOException) throw (IOException)e;
			throw new IOException(e);
		}
		int nb = currentRead.getResult().intValue();
		if (nb <= 0) {
			buffer = null;
			bb = null;
			readBuffer = null;
			readTask = null;
			return;
		}
		if (readTask == null) return;
		buffer = readBuffer.array();
		pos = 0;
		len = readBuffer.position();
		ByteBuffer b = readBuffer;
		readBuffer = bb;
		bb = b;
		readBuffer.clear();
		readTask = io.readAsync(readBuffer);
	}

	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		if (pos == len) {
			if (this.buffer == null) return -1;
			fill();
			if (pos == len) return -1;
		}
		int l = buffer.remaining();
		if (l > len - pos) l = len - pos;
		buffer.put(this.buffer, pos, l);
		pos += l;
		return l;
	}
	
	@Override
	public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readAsyncUsingSync(this, buffer, ondone).getOutput();
	}

	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}

	@Override
	public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return IOUtil.readFullyAsync(this, buffer, ondone);
	}

	@Override
	public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		if (pos == len && buffer == null) {
			if (ondone != null) ondone.run(new Pair<>(null, null));
			return new AsyncWork<>(null, null);
		}
		Task.Cpu<ByteBuffer, IOException> task = new Task.Cpu<ByteBuffer, IOException>("Read next buffer", getPriority(), ondone) {
			@Override
			public ByteBuffer run() throws IOException, CancelException {
				if (pos == len) {
					if (buffer == null) return null;
					fill();
					if (pos == len) return null;
				}
				ByteBuffer buf = ByteBuffer.allocate(len - pos);
				try { buf.put(buffer, pos, len - pos); }
				catch (NullPointerException e) {
					throw new CancelException("IO closed");
				}
				pos = len;
				buf.flip();
				return buf;
			}
		};
		task.start();
		return task.getOutput();
	}
	
	@Override
	public int skip(int skip) throws IOException {
		return (int)skipSync(skip);
	}

	@Override
	public long skipSync(long n) throws IOException {
		if (this.buffer == null || n <= 0) return 0;
		if (n <= len - pos) {
			pos += (int)n;
			return n;
		}
		AsyncWork<Integer,IOException> currentRead = readTask;
		if (currentRead == null)
			return 0;
		currentRead.block(0);
		if (!currentRead.isSuccessful()) {
			Exception e = currentRead.getError();
			if (e instanceof IOException) throw (IOException)e;
			throw new IOException(e);
		}
		int avail = currentRead.getResult().intValue();
		if (avail < 0) avail = 0;
		if (n <= len - pos + avail) {
			int i = len - pos;
			fill();
			return skipSync(n - i) + i;
		}
		n = io.skipSync(n - ((len - pos) + avail));
		n += len - pos;
		n += avail;
		len = pos = 0;
		readBuffer.clear();
		readTask = io.readAsync(readBuffer);
		return n;
	}

	@Override
	public AsyncWork<Long,IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		if (this.buffer == null || n <= 0) {
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(0), null));
			return new AsyncWork<Long,IOException>(Long.valueOf(0),null);
		}
		if (n <= len - pos) {
			pos += (int)n;
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(n), null));
			return new AsyncWork<Long,IOException>(Long.valueOf(n),null);
		}
		AsyncWork<Integer,IOException> currentRead = readTask;
		if (currentRead == null) {
			if (ondone != null) ondone.run(new Pair<>(Long.valueOf(0), null));
			return new AsyncWork<Long,IOException>(Long.valueOf(0),null);
		}
		Task<Long,IOException> task = new Task.Cpu<Long,IOException>("Skipping bytes", io.getPriority(), ondone) {
			@Override
			public Long run() throws IOException {
				if (currentRead.isCancelled()) return Long.valueOf(0);
				if (!currentRead.isSuccessful()) throw currentRead.getError();
				int avail = currentRead.getResult().intValue();
				if (avail < 0) avail = 0;
				if (n <= len - pos + avail) {
					int i = len - pos;
					fill();
					return Long.valueOf(skipSync(n - i) + i);
				}
				long res = io.skipSync(n - ((len - pos) + avail));
				res += len - pos;
				res += avail;
				len = pos = 0;
				readBuffer.clear();
				readTask = io.readAsync(readBuffer);
				return Long.valueOf(res);
			}
		};
		task.startOn(readTask, true);
		return task.getOutput();
	}

	@Override
	public int read() throws IOException {
		if (pos == len) {
			if (this.buffer == null) return -1;
			fill();
			if (pos == len) return -1;
		}
		return buffer[pos++] & 0xFF;
	}

	@Override
	public int read(byte[] buffer, int offset, int l) throws IOException {
		if (pos == len) {
			if (this.buffer == null) return -1;
			fill();
			if (pos == len) return -1;
		}
		if (l > len - pos) l = len - pos;
		System.arraycopy(this.buffer, pos, buffer, offset, l);
		pos += l;
		return l;
	}
	
	@Override
	public int readAsync() {
		if (pos == len) {
			if (this.buffer == null) return -1;
			return -2;
		}
		return buffer[pos++] & 0xFF;
	}

	@Override
	public int readFully(byte[] buffer) throws IOException {
		return IOUtil.readFully(this, buffer);
	}

}
