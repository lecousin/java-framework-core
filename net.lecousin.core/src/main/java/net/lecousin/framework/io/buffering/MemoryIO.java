package net.lecousin.framework.io.buffering;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
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
 * Implementation of a seekable readable and writable IO, using an array of byte array.
 * <br/>
 * Each byte array is allocated using the given bufferSize.
 */
public class MemoryIO extends ConcurrentCloseable<IOException>
	implements IO.Readable.Buffered, IO.Readable.Seekable, IO.Writable.Seekable, IO.Writable.Buffered, IO.KnownSize, IO.Resizable {

	/** Constructor. */
	public MemoryIO(int bufferSize, String description) {
		this.bufferSize = bufferSize;
		this.description = description;
	}
	
	private String description;
	private int bufferSize;
	private byte[][] buffers = new byte[10][];
	private int pos = 0;
	private int size = 0;
	private byte priority = Task.PRIORITY_NORMAL;
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return null;
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		pos = size = -1;
		buffers = null;
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
	public byte getPriority() { return priority; }
	
	@Override
	public void setPriority(byte priority) { this.priority = priority; }
	
	@Override
	public String getSourceDescription() { return description; }
	
	@Override
	public IO getWrappedIO() { return null; }
	
	@Override
	public TaskManager getTaskManager() { return Threading.getCPUTaskManager(); }
	
	@Override
	public int read() {
		if (pos == size) return -1;
		int c = buffers[pos / bufferSize][pos % bufferSize] & 0xFF;
		pos++;
		return c;
	}
	
	@Override
	public int read(byte[] buffer, int offset, int len) {
		int index = pos / bufferSize;
		int bufferPos = pos % bufferSize;
		if (len > size - pos) len = size - pos;
		if (len > bufferSize - bufferPos) len = bufferSize - bufferPos;
		if (len == 0) return 0;
		System.arraycopy(buffers[index], bufferPos, buffer, offset, len);
		pos += len;
		return len;
	}
	
	@Override
	public byte readByte() throws EOFException {
		if (pos == size) throw new EOFException();
		byte b = buffers[pos / bufferSize][pos % bufferSize];
		pos++;
		return b;
	}
	
	@Override
	public int readFully(byte[] buffer) {
		if (pos == size) return 0;
		int index = pos / bufferSize;
		int bufferPos = pos % bufferSize;
		int remaining = buffer.length;
		int offset = 0;
		do {
			int len = remaining;
			if (len > size - pos) len = size - pos;
			if (len > bufferSize - bufferPos) len = bufferSize - bufferPos;
			if (len == 0) return offset;
			System.arraycopy(buffers[index], bufferPos, buffer, offset, len);
			offset += len;
			pos += len;
			if (len == remaining || pos == size) return offset;
			remaining -= len;
			index++;
			bufferPos = 0;
		} while (true);
	}

	@Override
	public int readSync(ByteBuffer buffer) {
		int nb = readSync(pos, buffer);
		if (nb > 0) pos += nb;
		return nb;
	}
	
	@Override
	public int readSync(long pos, ByteBuffer buffer) {
		int p = (int)pos;
		if (p > size) p = size;
		if (p == size) return 0;
		int index = p / bufferSize;
		int bufferPos = p % bufferSize;
		int len = buffer.remaining();
		if (len > size - p) len = size - p;
		if (len > bufferSize - bufferPos) len = bufferSize - bufferPos;
		buffer.put(buffers[index], bufferPos, len);
		return len;
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) {
		int nb = readFullySync(pos, buffer);
		if (nb > 0) pos += nb;
		return nb;
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) {
		int p = (int)pos;
		if (p > size) p = size;
		if (p == size) return 0;
		int index = p / bufferSize;
		int bufferPos = p % bufferSize;
		int offset = 0;
		do {
			int len = buffer.remaining();
			if (len > size - p) len = size - p;
			if (len > bufferSize - bufferPos) len = bufferSize - bufferPos;
			buffer.put(buffers[index], bufferPos, len);
			offset += len;
			p += len;
			if (buffer.remaining() == 0 || p == size)
				return offset;
			index++;
			bufferPos = 0;
		} while (true);
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
		return operation(IOUtil.readAsyncUsingSync(this, buffer, ondone).getOutput());
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readAsyncUsingSync(this, pos, buffer, ondone).getOutput());
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsyncUsingSync(this, buffer, ondone).getOutput());
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readFullyAsyncUsingSync(this, pos, buffer, ondone).getOutput());
	}

	@Override
	public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		if (pos == size) return IOUtil.success(null, ondone);
		Task.Cpu<ByteBuffer, IOException> task = new Task.Cpu.FromSupplierThrows<>(
			"Read next buffer", getPriority(), ondone, this::readNextBuffer);
		task.start();
		operation(task);
		return task.getOutput();
	}
	
	@Override
	public ByteBuffer readNextBuffer() throws IOException {
		if (pos == size) return null;
		int index = pos / bufferSize;
		int bufferPos = pos % bufferSize;
		int len = size - pos;
		if (len > bufferSize - bufferPos) len = bufferSize - bufferPos;
		ByteBuffer buf = ByteBuffer.allocate(len);
		buf.put(buffers[index], bufferPos, len);
		pos += len;
		buf.flip();
		return buf;
	}
	
	@Override
	public long getSizeSync() {
		return size;
	}
	
	@Override
	public AsyncSupplier<Long, IOException> getSizeAsync() {
		return new AsyncSupplier<>(Long.valueOf(size), null);
	}
	
	@Override
	public long getPosition() {
		return pos;
	}
	
	@Override
	public long seekSync(SeekType type, long move) {
		switch (type) {
		case FROM_BEGINNING:
			if (move < 0) pos = 0;
			else if (move > size) pos = size;
			else pos = (int)move;
			break;
		case FROM_END:
			if (move < 0) pos = size;
			else if (move > size) pos = 0;
			else pos = size - (int)move;
			break;
		case FROM_CURRENT: 
			if (move < 0) {
				if (pos + move < 0) pos = 0;
				else pos += (int)move;
			} else {
				if (pos + move > size) pos = size;
				else pos += (int)move;
			}
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
				int done = -pos;
				pos = 0;
				return done;
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
		if (n < 0 && n < Integer.MIN_VALUE) n = Integer.MIN_VALUE;
		if (n > 0 && n > Integer.MAX_VALUE) n = Integer.MAX_VALUE;
		return skip((int)n);
	}
	
	@Override
	public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		return IOUtil.success(Long.valueOf(skipSync(n)), ondone);
	}
	
	private void increaseBuffers(int min) {
		// need to resize
		byte[][] b = new byte[min >= buffers.length * 2 - 10 ? min + 10 : buffers.length * 2][];
		System.arraycopy(buffers, 0, b, 0, buffers.length);
		buffers = b;
	}
	
	@Override
	public void write(byte byt) {
		int index = pos / bufferSize;
		int bufferPos = pos % bufferSize;
		if (index >= buffers.length)
			increaseBuffers(index);
		if (buffers[index] == null) {
			// need to allocate
			for (int i = index; i >= 0; --i)
				if (buffers[i] == null)
					buffers[i] = new byte[bufferSize];
				else
					break;
		}
		buffers[index][bufferPos] = byt;
		if (++pos > size) size = pos;
	}
	
	@Override
	public void write(byte[] buffer, int offset, int length) {
		while (length > 0) {
			int index = pos / bufferSize;
			int bufferPos = pos % bufferSize;
			if (index >= buffers.length)
				increaseBuffers(index);
			if (buffers[index] == null) {
				// need to allocate
				for (int i = index; i >= 0; --i)
					if (buffers[i] == null)
						buffers[i] = new byte[bufferSize];
					else
						break;
			}
			int len = length;
			if (len > bufferSize - bufferPos) len = bufferSize - bufferPos;
			System.arraycopy(buffer, offset, buffers[index], bufferPos, len);
			pos += len;
			offset += len;
			length -= len;
			if (pos > size) size = pos;
		}
	}
	
	@Override
	public int writeSync(ByteBuffer buffer) {
		int nb = writeSync(pos, buffer);
		if (nb > 0) pos += nb;
		return nb;
	}
	
	@Override
	public int writeSync(long pos, ByteBuffer buffer) {
		int p = (int)pos;
		int done = 0;
		while (buffer.remaining() > 0) {
			int index = p / bufferSize;
			int bufferPos = p % bufferSize;
			if (index >= buffers.length)
				increaseBuffers(index);
			if (buffers[index] == null) {
				// need to allocate
				for (int i = index; i >= 0; --i)
					if (buffers[i] == null)
						buffers[i] = new byte[bufferSize];
					else
						break;
			}
			int len = buffer.remaining();
			if (len > bufferSize - bufferPos) len = bufferSize - bufferPos;
			buffer.get(buffers[index], bufferPos, len);
			p += len;
			done += len;
			if (p > size) size = p;
		}
		return done;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.writeAsyncUsingSync(this, buffer, ondone)).getOutput();
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.writeAsyncUsingSync(this, pos, buffer, ondone)).getOutput();
	}
	
	@Override
	public void setSizeSync(long newSize) {
		if (newSize == size) return;
		if (newSize > size) {
			size = (int)newSize;
			int index = size / bufferSize;
			if (index >= buffers.length)
				increaseBuffers(index);
			for (int i = index; i >= 0; --i)
				if (buffers[i] == null)
					buffers[i] = new byte[bufferSize];
				else
					break;
		} else {
			if (newSize < 0) size = 0;
			else size = (int)newSize;
			if (pos > size) pos = size;
			int nbBuffers = size / bufferSize;
			if ((size % bufferSize) != 0) nbBuffers++;
			for (int i = buffers.length - 1; i >= nbBuffers; --i)
				buffers[i] = null;
		}
	}
	
	@Override
	public AsyncSupplier<Void, IOException> setSizeAsync(long newSize) {
		return operation(IOUtil.setSizeAsyncUsingSync(this, newSize, priority)).getOutput();
	}
	
	/** Create a MemoryIO and fill it with the content of the given Readable.
	 * This can be used to convert a Readable into a Seekable.
	 */
	public static AsyncSupplier<IO.Readable.Seekable,IOException> from(IO.Readable io) {
		AsyncSupplier<IO.Readable.Seekable,IOException> sp = new AsyncSupplier<>();
		if (io instanceof IO.KnownSize) {
			// if we know the size, better to use FullyBufferedIO
			((IO.KnownSize)io).getSizeAsync().onDone(result -> {
				long size = result.longValue();
				TwoBuffersIO.DeterminedSize buf = new TwoBuffersIO.DeterminedSize(io, (int)size, 0);
				buf.canStartReading().onDone(() -> sp.unblockSuccess(buf));
			}, sp);
			return sp;
		}
		MemoryIO mem = new MemoryIO(32768, "MemoryIO: " + io.getSourceDescription());
		IOUtil.copy(io, mem, -1, false, null, 0).onDone(result -> {
			mem.seekSync(SeekType.FROM_BEGINNING, 0);
			sp.unblockSuccess(mem);
		}, sp);
		return sp;
	}

	@Override
	public IAsync<IOException> flush() {
		return new Async<>(true);
	}
	
	/** Asynchronously write the content of this MemoryIO into the given Writable. */
	public Async<IOException> writeAsyncTo(IO.Writable io) {
		Async<IOException> sp = new Async<>();
		writeAsyncTo(io, sp, 0);
		operation(sp);
		return sp;
	}
	
	private void writeAsyncTo(IO.Writable io, Async<IOException> sp, int bufferIndex) {
		int lastIndex = size / bufferSize;
		while (bufferIndex < lastIndex) {
			AsyncSupplier<Integer, IOException> write = io.writeAsync(ByteBuffer.wrap(buffers[bufferIndex]));
			if (!write.isDone()) {
				int i = bufferIndex;
				write.onDone(() -> {
					if (write.hasError()) sp.error(write.getError());
					else if (write.isCancelled()) sp.cancel(write.getCancelEvent());
					else writeAsyncTo(io, sp, i + 1);
				});
				return;
			}
			if (write.hasError()) sp.error(write.getError());
			else if (write.isCancelled()) sp.cancel(write.getCancelEvent());
			if (sp.isDone()) return;
			bufferIndex++;
		}
		int bufferPos = size % bufferSize;
		if (bufferPos == 0) {
			sp.unblock();
			return;
		}
		io.writeAsync(ByteBuffer.wrap(buffers[bufferIndex], 0, bufferPos)).onDone(sp);
	}
}
