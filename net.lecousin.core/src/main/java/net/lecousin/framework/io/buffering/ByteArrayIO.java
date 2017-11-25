package net.lecousin.framework.io.buffering;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.AbstractIO;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

/**
 * IO implemented with a single byte array.
 * It supports read, write and resize operations.
 */
public class ByteArrayIO extends AbstractIO 
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
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public ByteArrayIO(byte[] data, String description) {
		this.description = description;
		array = data;
		pos = 0;
		size = data.length;
	}
	
	/** Constructor. */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
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
	protected ISynchronizationPoint<IOException> closeIO() {
		pos = size = 0;
		array = null;
		return new SynchronizationPoint<>(true);
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartReading() {
		return new SynchronizationPoint<IOException>(true);
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartWriting() {
		return new SynchronizationPoint<IOException>(true);
	}
	
	@Override
	public long getPosition() { return pos; }
	
	@Override
	public long getSizeSync() { return size; }
	
	@Override
	public AsyncWork<Long, IOException> getSizeAsync() { return new AsyncWork<>(Long.valueOf(size),null); }
	
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
	public int readAsync() {
		return read();
	}
	
	@Override
	public int readFully(byte[] buffer) {
		return read(buffer, 0, buffer.length);
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) {
		return readFullySync(pos, buffer);
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) {
		if (pos > size) pos = size;
		int len = buffer.remaining();
		if (len > size - pos) len = size - (int)pos;
		if (len == 0) {
			this.pos = (int)pos;
			return 0;
		}
		buffer.put(array, (int)pos, len);
		this.pos = ((int)pos) + len;
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
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		Task<Integer, IOException> task = new Task.Cpu<Integer, IOException>("readAsync on ByteArrayIO", priority, ondone) {
			@Override
			public Integer run() {
				return Integer.valueOf(readFullySync(buffer));
			}
		};
		task.start();
		return task.getSynch();
	}
	
	@Override
	public AsyncWork<Integer, IOException> readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		Task<Integer, IOException> task = new Task.Cpu<Integer, IOException>("readAsync on ByteArrayIO", priority, ondone) {
			@Override
			public Integer run() {
				return Integer.valueOf(readFullySync(pos, buffer));
			}
		};
		task.start();
		return task.getSynch();
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return readAsync(buffer, ondone);
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return readAsync(pos, buffer, ondone);
	}
	
	@Override
	public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		if (pos == size) {
			if (ondone != null) ondone.run(new Pair<>(null, null));
			return new AsyncWork<>(null, null);
		}
		Task.Cpu<ByteBuffer, IOException> task = new Task.Cpu<ByteBuffer, IOException>(
			"Read remaining bytes from ByteArrayIO", getPriority(), ondone
		) {
			@Override
			public ByteBuffer run() {
				if (pos == size)
					return null;
				ByteBuffer buf = ByteBuffer.allocate(size - pos);
				buf.put(array, pos, size - pos);
				pos = size;
				buf.flip();
				return buf;
			}
		};
		task.start();
		return task.getSynch();
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
		this.pos = (int)(pos + len);
		if (this.pos > size) size = this.pos;
		return len;
	}
	
	@Override
	public AsyncWork<Integer, IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		Task<Integer, IOException> task = new Task.Cpu<Integer, IOException>("writeAsync on ByteArrayIO", priority, ondone) {
			@Override
			public Integer run() {
				return Integer.valueOf(writeSync(buffer));
			}
		};
		task.start();
		return task.getSynch();
	}
	
	@Override
	public AsyncWork<Integer, IOException> writeAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		Task<Integer, IOException> task = new Task.Cpu<Integer, IOException>("writeAsync on ByteArrayIO", priority, ondone) {
			@Override
			public Integer run() {
				return Integer.valueOf(writeSync(pos, buffer));
			}
		};
		task.start();
		return task.getSynch();
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
	public AsyncWork<Void, IOException> setSizeAsync(long newSize) {
		Task<Void, IOException> task = new Task.Cpu<Void, IOException>("setSizeAsync on ByteArrayIO", priority) {
			@Override
			public Void run() {
				setSizeSync(newSize);
				return null;
			}
		};
		task.start();
		return task.getSynch();
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
	public AsyncWork<Long, IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		Long r = Long.valueOf(seekSync(type, move));
		if (ondone != null) ondone.run(new Pair<>(r, null));
		return new AsyncWork<>(r, null);
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
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		Long r = Long.valueOf(skipSync(n));
		if (ondone != null) ondone.run(new Pair<>(r, null));
		return new AsyncWork<>(r, null);
	}
	
	@SuppressFBWarnings("EI_EXPOSE_REP")
	public byte[] getArray() { return array; }
	
	public int getCapacity() { return array.length; }
	
	/** Wrap the buffer into a ByteBuffer. */
	public ByteBuffer toByteBuffer() {
		return ByteBuffer.wrap(array, 0, size);
	}
	
	@Override
	public ISynchronizationPoint<IOException> flush() {
		return new SynchronizationPoint<>(true);
	}
	
}
