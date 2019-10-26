package net.lecousin.framework.io.buffering;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * IO implemented with a single byte array.
 * It supports read, write and resize operations.
 */
public class ByteArrayIO extends ConcurrentCloseable<IOException> 
	implements IO.Readable.Buffered, IO.Readable.Seekable, IO.Writable.Seekable, IO.Writable.Buffered, IO.KnownSize, IO.Resizable {

	/** Constructor with initial buffer size of 4096. */
	public ByteArrayIO(String description) {
		this(4096, description);
	}
	
	/** Constructor. */
	public ByteArrayIO(int initialSize, String description) {
		this.description = description;
		array = new byte[initialSize];
		pos = size = 0;
	}
	
	/** Constructor. */
	public ByteArrayIO(byte[] data, String description) {
		this.description = description;
		array = data;
		pos = 0;
		size = data.length;
	}
	
	/** Constructor. */
	public ByteArrayIO(byte[] data, int bytesUsed, String description) {
		this.description = description;
		array = data;
		pos = 0;
		size = bytesUsed;
	}
	
	private String description;
	private byte[] array;
	private int pos;
	private int size;
	private byte priority = Task.PRIORITY_NORMAL;
	
	@Override
	public TaskManager getTaskManager() { return Threading.getCPUTaskManager(); }
	
	@Override
	public IO getWrappedIO() { return null; }
	
	@Override
	public String getSourceDescription() { return description; }
	
	@Override
	public byte getPriority() { return priority; }
	
	@Override
	public void setPriority(byte priority) { this.priority = priority; }
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return null;
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		pos = size = 0;
		array = null;
		ondone.unblock();
	}
	
	@Override
	public IAsync<IOException> canStartReading() {
		return new Async<>(true);
	}
	
	@Override
	public IAsync<IOException> canStartWriting() {
		return new Async<>(true);
	}
	
	@Override
	public long getPosition() { return pos; }
	
	@Override
	public long getSizeSync() { return size; }
	
	@Override
	public AsyncSupplier<Long, IOException> getSizeAsync() { return new AsyncSupplier<>(Long.valueOf(size),null); }
	
	@Override
	public int read() {
		if (pos == size) return -1;
		return array[pos++] & 0xFF;
	}
	
	@Override
	public int read(byte[] buffer, int offset, int len) {
		if (len > size - pos) len = size - pos;
		System.arraycopy(array, pos, buffer, offset, len);
		pos += len;
		return len;
	}
	
	@Override
	public byte readByte() throws EOFException {
		if (pos == size) throw new EOFException();
		return array[pos++];
	}
	
	@Override
	public int readFully(byte[] buffer) {
		return read(buffer, 0, buffer.length);
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) {
		int nb = readFullySync(pos, buffer);
		if (nb > 0) pos += nb;
		return nb;
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) {
		if (pos > size) pos = size;
		int len = buffer.remaining();
		if (len > size - pos) len = size - (int)pos;
		if (len == 0) return 0;
		buffer.put(array, (int)pos, len);
		return len;
	}
	
	@Override
	public int readSync(ByteBuffer buffer) {
		return readFullySync(buffer);
	}
	
	@Override
	public int readSync(long pos, ByteBuffer buffer) {
		return readFullySync(pos, buffer);
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return IOUtil.success(Integer.valueOf(readFullySync(buffer)), ondone);
	}
	
	@Override
	public int readAsync() {
		return read();
	}

	@Override
	public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		Task<Integer, IOException> task = new Task.Cpu<Integer, IOException>("readAsync on ByteArrayIO", priority, ondone) {
			@Override
			public Integer run() {
				return Integer.valueOf(readFullySync(buffer));
			}
		};
		task.start();
		return operation(task.getOutput());
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		Task<Integer, IOException> task = new Task.Cpu<Integer, IOException>("readAsync on ByteArrayIO", priority, ondone) {
			@Override
			public Integer run() {
				return Integer.valueOf(readFullySync(pos, buffer));
			}
		};
		task.start();
		return operation(task.getOutput());
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return readAsync(buffer, ondone);
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return readAsync(pos, buffer, ondone);
	}
	
	@Override
	public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		if (pos == size) return IOUtil.success(null, ondone);
		Task.Cpu<ByteBuffer, IOException> task = new Task.Cpu.FromSupplierThrows<>(
			"Read remaining bytes from ByteArrayIO", getPriority(), ondone, this::readNextBuffer
		);
		operation(task.start());
		return operation(task.getOutput());
	}
	
	@Override
	public ByteBuffer readNextBuffer() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(size - pos);
		buf.put(array, pos, size - pos);
		pos = size;
		buf.flip();
		return buf;
	}
	
	/** Convert the content of the buffer into a String encoded with the given charset. */
	public String getAsString(Charset charset) {
		return new String(array, 0, size, charset);
	}
	
	@Override
	public void write(byte b) {
		if (pos + 1 >= array.length) {
			byte[] a = new byte[Math.max(array.length * 2, pos + 1)];
			System.arraycopy(array, 0, a, 0, size);
			array = a;
		}
		array[pos++] = b;
		if (pos > size) size = pos;
	}
	
	@Override
	public void write(byte[] buffer, int offset, int len) {
		if (pos + len > array.length) {
			int newSize = array.length * 2;
			while (newSize < pos + len) newSize *= 2;
			byte[] a = new byte[newSize];
			System.arraycopy(array, 0, a, 0, size);
			array = a;
		}
		System.arraycopy(buffer, offset, array, pos, len);
		pos += len;
		if (pos > size) size = pos;
	}
	
	@Override
	public int writeSync(ByteBuffer buffer) {
		int len = buffer.remaining();
		if (pos + len > array.length) {
			int newSize = array.length * 2;
			while (newSize < pos + len) newSize *= 2;
			byte[] a = new byte[newSize];
			System.arraycopy(array, 0, a, 0, size);
			array = a;
		}
		buffer.get(array, pos, len);
		pos += len;
		if (pos > size) size = pos;
		return len;
	}
	
	@Override
	public int writeSync(long pos, ByteBuffer buffer) {
		int len = buffer.remaining();
		if (pos + len > array.length) {
			int newSize = array.length * 2;
			while (newSize < pos + len) newSize *= 2;
			byte[] a = new byte[newSize];
			System.arraycopy(array, 0, a, 0, size);
			array = a;
		}
		buffer.get(array, (int)pos, len);
		if (pos + len > size) size = (int)(pos + len);
		return len;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		Task<Integer, IOException> task = new Task.Cpu<Integer, IOException>("writeAsync on ByteArrayIO", priority, ondone) {
			@Override
			public Integer run() {
				return Integer.valueOf(writeSync(buffer));
			}
		};
		task.start();
		return operation(task.getOutput());
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		Task<Integer, IOException> task = new Task.Cpu<Integer, IOException>("writeAsync on ByteArrayIO", priority, ondone) {
			@Override
			public Integer run() {
				return Integer.valueOf(writeSync(pos, buffer));
			}
		};
		task.start();
		return operation(task.getOutput());
	}
	
	@Override
	public void setSizeSync(long newSize) {
		if (newSize <= size) {
			size = (int)newSize;
			if (pos > size) pos = size;
		} else if (newSize <= array.length) {
			size = (int)newSize;
		} else {
			byte[] a = new byte[(int)newSize];
			System.arraycopy(array, 0, a, 0, size);
			array = a;
			size = (int)newSize;
		}
	}
	
	@Override
	public AsyncSupplier<Void, IOException> setSizeAsync(long newSize) {
		Task<Void, IOException> task = new Task.Cpu<Void, IOException>("setSizeAsync on ByteArrayIO", priority) {
			@Override
			public Void run() {
				setSizeSync(newSize);
				return null;
			}
		};
		task.start();
		return operation(task.getOutput());
	}
	
	@Override
	public long seekSync(SeekType type, long move) {
		switch (type) {
		case FROM_BEGINNING:
			if (move < 0) move = 0;
			if (move > size) pos = size;
			else pos = (int)move;
			break;
		case FROM_END:
			if (move < 0) move = 0;
			if (move > size) pos = 0;
			else pos = (int)(size - move);
			break;
		case FROM_CURRENT:
			pos += (int)move;
			if (pos < 0) pos = 0;
			else if (pos > size) pos = size;
			break;
		default: break;
		}
		return pos;
	}
	
	@Override
	public AsyncSupplier<Long, IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		return IOUtil.success(Long.valueOf(seekSync(type, move)), ondone);
	}
	
	@Override
	public int skip(int skip) {
		if (skip < 0) {
			if (pos + skip < 0) {
				int done = pos;
				pos = 0;
				return -done;
			}
			pos += skip;
			return skip;
		}
		if (pos + skip > size) {
			int done = size - pos;
			pos = size;
			return done;
		}
		pos += skip;
		return skip;
	}
	
	@Override
	public long skipSync(long n) {
		return skip((int)n);
	}
	
	@Override
	public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		return IOUtil.success(Long.valueOf(skipSync(n)), ondone);
	}
	
	public byte[] getArray() { return array; }
	
	public int getCapacity() { return array.length; }
	
	/** Wrap the buffer into a ByteBuffer. */
	public ByteBuffer toByteBuffer() {
		return ByteBuffer.wrap(array, 0, size);
	}
	
	@Override
	public IAsync<IOException> flush() {
		return new Async<>(true);
	}
	
}
