package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.io.AbstractIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.data.ByteArray;
import net.lecousin.framework.util.Pair;

/**
 * Implementation of IO using a list of byte array.
 */
public class ByteBuffersIO extends AbstractIO implements IO.Readable.Buffered, IO.Readable.Seekable, IO.KnownSize, IO.Writable {

	/** Constructor.
	 * @param copyBuffers if true each written buffer is copied into a new buffer,
	 *     if false the buffer are kept but any modification of them outside of this class may lead to unexpected behavior.
	 * @param description description
	 */
	public ByteBuffersIO(boolean copyBuffers, String description, Priority priority) {
		super(description, priority);
		this.copyBuffers = copyBuffers;
	}
	
	private boolean copyBuffers;
	private LinkedArrayList<ByteArray.Writable> buffers = new LinkedArrayList<>(10);
	private int pos = 0;
	private int bufferIndex = 0;
	private int bufferPos = 0;
	private int totalSize = 0;
	
	/** Must be called only when no more modification are being done. */
	public LinkedArrayList<ByteArray.Writable> getBuffers() {
		return buffers;
	}
	
	/** Merge the byte arrays of this class into a single one and return it. */
	public byte[] createSingleByteArray() {
		byte[] buf = new byte[totalSize];
		int bufPos = 0;
		for (ByteArray.Writable b : buffers) {
			int p = b.position();
			b.get(buf, bufPos, b.remaining());
			b.setPosition(p);
			bufPos += b.remaining();
		}
		return buf;
	}
	
	/** Append the given buffer. */
	public synchronized void addBuffer(ByteArray.Writable array) {
		if (copyBuffers) {
			byte[] b = new byte[array.remaining()];
			array.get(b, 0, b.length);
			array = new ByteArray.Writable(b, false);
		}
		buffers.add(array);
		totalSize += array.remaining();
	}

	@Override
	public synchronized int readSync(ByteBuffer buffer) {
		if (bufferIndex == buffers.size()) return -1;
		int done = 0;
		while (bufferIndex < buffers.size() && buffer.hasRemaining()) {
			ByteArray.Writable b = buffers.get(bufferIndex);
			int len = buffer.remaining();
			if (len > b.remaining() - bufferPos)
				len = b.remaining() - bufferPos;
			buffer.put(b.getArray(), b.getCurrentArrayOffset() + bufferPos, len);
			bufferPos += len;
			pos += len;
			done += len;
			if (bufferPos == b.remaining()) {
				bufferIndex++;
				bufferPos = 0;
			}
		}
		return done;
	}

	@Override
	public synchronized int readSync(long pos, ByteBuffer buffer) {
		// seek to given pos
		int readBufferIndex = this.bufferIndex;
		int readBufferPos = this.bufferPos;
		long n = pos - this.pos;
		if (n > 0) {
			long rem = n;
			while (readBufferIndex < buffers.size() && rem > 0) {
				int l = buffers.get(readBufferIndex).remaining() - readBufferPos;
				if (l > rem) {
					readBufferPos += rem;
					//rem = 0;
					break;
				}
				rem -= l;
				readBufferPos = 0;
				readBufferIndex++;
			}
		} else {
			if (this.pos + n < 0) n = -this.pos;
			long rem = -n;
			while (rem > 0) {
				if (readBufferPos >= rem) {
					readBufferPos -= rem;
					break;
				}
				rem -= readBufferPos;
				if (readBufferIndex == 0) {
					readBufferPos = 0;
					//n += rem;
					break;
				}
				readBufferIndex--;
				readBufferPos = buffers.get(readBufferIndex).remaining();
			}
		}
		
		if (readBufferIndex == buffers.size()) return -1;
		int done = 0;
		while (readBufferIndex < buffers.size() && buffer.hasRemaining()) {
			ByteArray.Writable b = buffers.get(readBufferIndex);
			int len = buffer.remaining();
			if (len > b.remaining() - readBufferPos)
				len = b.remaining() - readBufferPos;
			buffer.put(b.getArray(), b.getCurrentArrayOffset() + readBufferPos, len);
			readBufferPos += len;
			done += len;
			if (readBufferPos == b.remaining()) {
				readBufferIndex++;
				readBufferPos = 0;
			}
		}
		return done;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return IOUtil.success(Integer.valueOf(readFullySync(buffer)), ondone);
	}

	@Override
	public AsyncSupplier<Integer, IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(Task.cpu("readAsync on ByteBuffersIO", this.getPriority(),
			t -> Integer.valueOf(readSync(pos, buffer)), ondone).start()).getOutput();
	}
	
	@Override
	public int readAsync() {
		return read();
	}

	@Override
	public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readAsyncUsingSync(this, buffer, ondone));
	}
	
	@Override
	public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		return operation(Task.cpu("Read next buffer", getPriority(),
			new Executable.FromSupplierThrows<>(this::readNextBuffer), ondone).start()).getOutput();
	}
	
	@Override
	public ByteBuffer readNextBuffer() throws IOException {
		if (bufferIndex == buffers.size()) return null;
		ByteArray.Writable b = buffers.get(bufferIndex);
		int len = b.remaining() - bufferPos;
		ByteBuffer buf = ByteBuffer.wrap(b.getArray(), b.getCurrentArrayOffset() + bufferPos, len).asReadOnlyBuffer();
		pos += len;
		bufferIndex++;
		bufferPos = 0;
		return buf;
	}

	@Override
	public int readFullySync(ByteBuffer buffer) {
		return readSync(buffer);
	}

	@Override
	public int readFullySync(long pos, ByteBuffer buffer) {
		return readSync(pos, buffer);
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
	public synchronized long skipSync(long n) {
		if (n == 0) return 0;
		if (n > 0) {
			long rem = n;
			while (bufferIndex < buffers.size() && rem > 0) {
				int l = buffers.get(bufferIndex).remaining() - bufferPos;
				if (l > rem) {
					bufferPos += rem;
					rem = 0;
					break;
				}
				rem -= l;
				bufferPos = 0;
				bufferIndex++;
			}
			pos += n - rem;
			return n - rem;
		}
		if (pos + n < 0) n = -pos;
		long rem = -n;
		while (rem > 0) {
			if (bufferPos >= rem) {
				bufferPos -= rem;
				break;
			}
			rem -= bufferPos;
			if (bufferIndex == 0) {
				bufferPos = 0;
				n += rem;
				break;
			}
			bufferIndex--;
			bufferPos = buffers.get(bufferIndex).remaining();
		}
		pos += n;
		return n;
	}

	@Override
	public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long,IOException>> ondone) {
		return IOUtil.success(Long.valueOf(skipSync(n)), ondone);
	}

	@Override
	public IO getWrappedIO() {
		return null;
	}

	@Override
	public TaskManager getTaskManager() {
		return Threading.getCPUTaskManager();
	}

	@Override
	public synchronized int read() {
		if (bufferIndex == buffers.size()) return -1;
		ByteArray.Writable buf = buffers.get(bufferIndex);
		byte b = buf.getForward(bufferPos);
		if (++bufferPos == buf.remaining()) {
			bufferIndex++;
			bufferPos = 0;
		}
		pos++;
		return b & 0xFF;
	}

	@Override
	public int read(byte[] buffer, int offset, int len) {
		return readSync(ByteBuffer.wrap(buffer, offset, len));
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
	public long getSizeSync() {
		return totalSize;
	}

	@Override
	public AsyncSupplier<Long, IOException> getSizeAsync() {
		return new AsyncSupplier<>(Long.valueOf(totalSize), null);
	}

	@Override
	public long getPosition() {
		return pos;
	}

	@Override
	public long seekSync(SeekType type, long move) {
		switch (type) {
		case FROM_BEGINNING:
			skipSync(move - pos);
			break;
		case FROM_CURRENT:
			skipSync(move);
			break;
		default: //case FROM_END:
			skipSync(totalSize - move - pos);
			break;
		}
		return pos;
	}

	@Override
	public AsyncSupplier<Long, IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		return IOUtil.success(Long.valueOf(seekSync(type, move)), ondone);
	}

	@Override
	public int readFully(byte[] buffer) {
		return read(buffer, 0, buffer.length);
	}

	@Override
	public int skip(int skip) {
		return (int)skipSync(skip);
	}

	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		return null;
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		buffers = null;
		ondone.unblock();
	}

	@Override
	public int writeSync(ByteBuffer buffer) {
		addBuffer(ByteArray.Writable.fromByteBuffer(buffer));
		pos = totalSize;
		bufferIndex = buffers.size();
		bufferPos = 0;
		int len = buffer.remaining();
		buffer.position(buffer.position() + len);
		return len;
	}

	@Override
	public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		if (!copyBuffers) return IOUtil.success(Integer.valueOf(writeSync(buffer)), ondone);
		return operation(IOUtil.writeAsyncUsingSync(this, buffer, ondone));
	}
	
}
