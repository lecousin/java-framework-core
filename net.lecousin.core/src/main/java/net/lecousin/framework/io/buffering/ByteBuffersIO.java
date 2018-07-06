package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;
import net.lecousin.framework.util.Triple;

/**
 * Implementation of IO using a list of byte array.
 */
public class ByteBuffersIO extends ConcurrentCloseable implements IO.Readable.Buffered, IO.Readable.Seekable, IO.KnownSize, IO.Writable {

	/** Constructor.
	 * @param copyBuffers if true each written buffer is copied into a new buffer,
	 *     if false the buffer are kept but any modification of them outside of this class may lead to unexpected behavior.
	 * @param description description
	 */
	public ByteBuffersIO(boolean copyBuffers, String description, byte priority) {
		this.copyBuffers = copyBuffers;
		this.description = description;
		this.priority = priority;
	}
	
	private boolean copyBuffers;
	private String description;
	private byte priority;
	private LinkedArrayList<Triple<byte[],Integer,Integer>> buffers = new LinkedArrayList<>(10);
	private int pos = 0;
	private int bufferIndex = 0;
	private int bufferPos = 0;
	private int totalSize = 0;
	
	/** Must be called only when no more modification are being done. */
	public LinkedArrayList<Triple<byte[],Integer,Integer>> getBuffers() {
		return buffers;
	}
	
	/** Merge the byte arrays of this class into a single one and return it. */
	public byte[] createSingleByteArray() {
		byte[] buf = new byte[totalSize];
		int pos = 0;
		for (Triple<byte[],Integer,Integer> t : buffers) {
			System.arraycopy(t.getValue1(), t.getValue2().intValue(), buf, pos, t.getValue3().intValue());
			pos += t.getValue3().intValue();
		}
		return buf;
	}
	
	/** Append the given buffer. */
	public synchronized void addBuffer(byte[] buf, int off, int len) {
		if (copyBuffers) {
			byte[] b = new byte[len];
			System.arraycopy(buf, off, b, 0, len);
			buf = b;
			off = 0;
		}
		buffers.add(new Triple<>(buf, Integer.valueOf(off), Integer.valueOf(len)));
		totalSize += len;
	}

	@Override
	public synchronized int readSync(ByteBuffer buffer) {
		if (bufferIndex == buffers.size())
			return -1;
		int done = 0;
		while (bufferIndex < buffers.size() && buffer.hasRemaining()) {
			Triple<byte[],Integer,Integer> b = buffers.get(bufferIndex);
			int len = buffer.remaining();
			if (len > b.getValue3().intValue() - bufferPos)
				len = b.getValue3().intValue() - bufferPos;
			buffer.put(b.getValue1(), b.getValue2().intValue() + bufferPos, len);
			bufferPos += len;
			pos += len;
			done += len;
			if (bufferPos == b.getValue3().intValue()) {
				bufferIndex++;
				bufferPos = 0;
			}
		}
		return done;
	}

	@Override
	public synchronized int readSync(long pos, ByteBuffer buffer) {
		if (pos != this.pos)
			skipSync(pos - this.pos);
		return readSync(buffer);
	}
	
	@Override
	public AsyncWork<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, RunnableWithParameter<Pair<Integer, IOException>> ondone) {
		Integer r = Integer.valueOf(readFullySync(buffer));
		if (ondone != null) ondone.run(new Pair<>(r, null));
		return new AsyncWork<>(r, null);
	}
	
	@Override
	public int readAsync() {
		return read();
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return operation(IOUtil.readAsyncUsingSync(this, buffer, ondone).getOutput());
	}

	@Override
	public AsyncWork<Integer, IOException> readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		Task<Integer, IOException> task = new Task.Cpu<Integer, IOException>("readAsync on ByteBuffersIO", this.getPriority(), ondone) {
			@Override
			public Integer run() {
				return Integer.valueOf(readSync(pos, buffer));
			}
		};
		task.start();
		return operation(task.getOutput());
	}
	
	@Override
	public AsyncWork<ByteBuffer, IOException> readNextBufferAsync(RunnableWithParameter<Pair<ByteBuffer, IOException>> ondone) {
		Task.Cpu<ByteBuffer, IOException> task = new Task.Cpu<ByteBuffer, IOException>("Read next buffer", getPriority(), ondone) {
			@Override
			public ByteBuffer run() {
				if (bufferIndex == buffers.size())
					return null;
				Triple<byte[],Integer,Integer> b = buffers.get(bufferIndex);
				int len = b.getValue3().intValue() - bufferPos;
				ByteBuffer buf = ByteBuffer.wrap(b.getValue1(), b.getValue2().intValue() + bufferPos, len);
				pos += len;
				bufferIndex++;
				bufferPos = 0;
				return buf;
			}
		};
		task.start();
		return operation(task.getOutput());
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
	public AsyncWork<Integer, IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return readAsync(buffer, ondone);
	}

	@Override
	public AsyncWork<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return readAsync(pos, buffer, ondone);
	}

	@Override
	public synchronized long skipSync(long n) {
		if (n == 0) return 0;
		if (n > 0) {
			long rem = n;
			while (bufferIndex < buffers.size() && rem > 0) {
				int l = buffers.get(bufferIndex).getValue3().intValue() - bufferPos;
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
			bufferPos = buffers.get(bufferIndex).getValue3().intValue();
		}
		pos += n;
		return n;
	}

	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		Long r = Long.valueOf(skipSync(n));
		if (ondone != null) ondone.run(new Pair<>(r, null));
		return new AsyncWork<>(r,null);
	}

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
	public synchronized int read() {
		if (bufferIndex == buffers.size())
			return -1;
		Triple<byte[],Integer,Integer> buf = buffers.get(bufferIndex);
		byte b = buf.getValue1()[buf.getValue2().intValue() + bufferPos];
		if (++bufferPos == buf.getValue3().intValue()) {
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
	public ISynchronizationPoint<IOException> canStartReading() {
		return new SynchronizationPoint<>(true);
	}
	
	@Override
	public ISynchronizationPoint<IOException> canStartWriting() {
		return new SynchronizationPoint<>(true);
	}

	@Override
	public long getSizeSync() {
		return totalSize;
	}

	@Override
	public AsyncWork<Long, IOException> getSizeAsync() {
		return new AsyncWork<>(Long.valueOf(totalSize), null);
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
		case FROM_END:
			skipSync(totalSize - move - pos);
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
	public int readFully(byte[] buffer) {
		return read(buffer, 0, buffer.length);
	}

	@Override
	public int skip(int skip) {
		return (int)skipSync(skip);
	}

	@Override
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		return null;
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		buffers = null;
		ondone.unblock();
	}

	@Override
	public int writeSync(ByteBuffer buffer) {
		addBuffer(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
		pos = totalSize;
		bufferIndex = buffers.size();
		bufferPos = 0;
		int len = buffer.remaining();
		buffer.position(buffer.position() + len);
		return len;
	}

	@Override
	public AsyncWork<Integer, IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		if (!copyBuffers) {
			Integer r = Integer.valueOf(writeSync(buffer));
			if (ondone != null) ondone.run(new Pair<>(r, null));
			return new AsyncWork<>(r, null);
		}
		return operation(IOUtil.writeAsyncUsingSync(this, buffer, ondone).getOutput());
	}
	
}
