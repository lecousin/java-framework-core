package net.lecousin.framework.io.buffering;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * Store data first in memory, then if exceeding a given amount of memory, store the remaining
 * data in a file.<br/>
 * <br/>
 * This can be used when the amount of data is not known, and we want to keep good performance with
 * small data (all in memory), but do not allocate too much memory when having large data.<br/>
 * A typical usage is when a server is receiving an uploaded file, and the size is not known in advance.<br/>
 * <br/>
 * This IO is writable and readable: the written data are readable.
 */
public class IOInMemoryOrFile extends ConcurrentCloseable implements IO.Readable.Seekable, IO.Writable.Seekable, IO.KnownSize, IO.Resizable {

	/** Constructor. */
	public IOInMemoryOrFile(int maxSizeInMemory, byte priority, String sourceDescription) {
		this.priority = priority;
		this.sourceDescription = sourceDescription;
		int nbBuffers = maxSizeInMemory / BUFFER_SIZE;
		if ((maxSizeInMemory % BUFFER_SIZE) > 0) nbBuffers++;
		this.maxSizeInMemory = nbBuffers * BUFFER_SIZE;
		memory = new byte[nbBuffers][];
	}
	
	private static final int BUFFER_SIZE = 8192;
	
	private int maxSizeInMemory;
	private byte priority;
	private String sourceDescription;
	private long pos = 0;
	private long size = 0;
	private byte[][] memory;
	private FileIO.ReadWrite file = null;
	
	@Override
	public String getSourceDescription() { return sourceDescription; }
	
	private void createFileSync() throws IOException {
		this.file = TemporaryFiles.get().createAndOpenFileSync("net.lecousin.framework", "tempIO");
	}
	
	private SynchronizationPoint<IOException> createFileAsync() {
		SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
		TemporaryFiles.get().createAndOpenFileAsync("net.lecousin.framework", "tempIO").listenInline(f -> {
			IOInMemoryOrFile.this.file = f;
			sp.unblock();
		}, sp);
		return sp;
	}
	
	@Override
	public IO getWrappedIO() {
		return null;
	}
	
	public int getMaxInMemory() {
		return memory.length * BUFFER_SIZE;
	}
	
	@Override
	public TaskManager getTaskManager() {
		// by default...
		return Threading.getCPUTaskManager();
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
	protected ISynchronizationPoint<?> closeUnderlyingResources() {
		if (file != null) {
			SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
			file.closeAsync().listenInline(() -> {
				if (!file.getFile().delete())
					LCCore.getApplication().getDefaultLogger()
						.warn("Unable to remove temporary file: " + file.getFile().getAbsolutePath());
				sp.unblock();
			}, sp);
			return sp;
		}
		return null;
	}
	
	@Override
	protected void closeResources(SynchronizationPoint<Exception> ondone) {
		file = null;
		memory = null;
		ondone.unblock();
	}
	
	@Override
	public long getPosition() {
		return pos;
	}
	
	@Override
	public long seekSync(SeekType type, long move) {
		switch (type) {
		case FROM_BEGINNING:
			this.pos = move;
			break;
		case FROM_CURRENT:
			this.pos += move;
			break;
		case FROM_END:
			this.pos = size - move;
			break;
		default: break;
		}
		if (this.pos < 0) this.pos = 0;
		if (this.pos > size) this.pos = this.size;
		return this.pos;
	}
	
	@Override
	public long skipSync(long n) {
		long prevPos = pos;
		this.pos += n;
		if (this.pos < 0) this.pos = 0;
		if (this.pos > size) this.pos = this.size;
		return this.pos - prevPos;
	}
	
	@Override
	public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		seekSync(type, move);
		return IOUtil.success(Long.valueOf(pos), ondone);
	}
	
	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
		return IOUtil.success(Long.valueOf(skipSync(n)), ondone);
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
	public long getSizeSync() {
		return size;
	}

	@Override
	public AsyncWork<Long, IOException> getSizeAsync() {
		return new AsyncWork<>(Long.valueOf(size), null);
	}

	@Override
	public int writeSync(ByteBuffer buffer) throws IOException {
		int nb = writeSync(pos, buffer);
		if (nb > 0) pos += nb;
		return nb;
	}
	
	@Override
	public int writeSync(long pos, ByteBuffer buffer) throws IOException {
		if (pos < 0) pos = 0;
		if (pos > size) {
			setSizeSync(pos);
		}
		int len = buffer.remaining();
		if (pos < maxSizeInMemory) {
			// some data will go to memory
			int inMem;
			if (pos + len <= maxSizeInMemory) {
				// all go to memory
				inMem = len;
			} else {
				inMem = (int)(maxSizeInMemory - pos);
			}
			int p = (int)pos;
			int rem = inMem;
			do {
				int i = p / BUFFER_SIZE;
				int j = p % BUFFER_SIZE;
				int l = rem;
				if (l > BUFFER_SIZE - j) l = BUFFER_SIZE - j;
				if (memory[i] == null) memory[i] = new byte[BUFFER_SIZE];
				buffer.get(memory[i], j, l);
				rem -= l;
				p += l;
			} while (rem > 0);
			if (pos + len <= maxSizeInMemory) {
				if (p > size) size = p;
				return len;
			}

			// some other will go to file
			if (file == null)
				createFileSync();
			
			int inFil = file.writeSync(0, buffer);
			if (inFil < len - inMem) len = inMem + inFil;
			if (pos + len > size) size = pos + len;
			return len;
		}

		// all go to file
		if (file == null)
			createFileSync();
		int nb = file.writeSync(pos - maxSizeInMemory, buffer);
		if (pos + nb > size) size = pos + nb;
		return nb;
	}

	@Override
	public AsyncWork<Integer,IOException> writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		if (pos < 0) pos = 0;
		if (pos > size) {
			AsyncWork<Void, IOException> resize = setSizeAsync(pos);
			AsyncWork<Integer,IOException> result = new AsyncWork<>();
			long p = pos;
			IOUtil.listenOnDone(resize, res -> writeAsync(p, buffer, ondone).listenInline(result), result, ondone);
			return operation(result);
		}
		int len = buffer.remaining();
		if (pos < maxSizeInMemory) {
			// some data will go to memory
			if (pos + len <= maxSizeInMemory) {
				// all go to memory
				Task<Integer,IOException> task = new WriteInMemory((int)pos, buffer, len, ondone);
				if (pos + len > size) size = pos + len;
				task.start();
				return operation(task.getOutput());
			}
			// some other will go to file
			Task<Integer,IOException> mem = new WriteInMemory((int)pos, buffer, (int)(maxSizeInMemory - pos), null);
			mem.start();
			AsyncWork<Integer, IOException> memReady;
			if (file == null) {
				memReady = new AsyncWork<>();
				SynchronizationPoint<IOException> createFile = createFileAsync();
				JoinPoint.fromSynchronizationPointsSimilarError(mem.getOutput(), createFile).listenInline(
					() -> memReady.unblockSuccess(mem.getOutput().getResult()),
					memReady
				);
			} else {
				memReady = mem.getOutput();
			}
			AsyncWork<Integer,IOException> sp = new AsyncWork<>();
			Mutable<AsyncWork<Integer,IOException>> fil = new Mutable<>(null);
			if (pos + len > size) size = pos + len;
			IOUtil.listenOnDone(memReady,
			res -> {
				fil.set(file.writeAsync(0, buffer));
				IOUtil.listenOnDone(fil.get(), result -> {
					Integer r = Integer.valueOf(mem.getResult().intValue() + result.intValue());
					if (ondone != null) ondone.accept(new Pair<>(r, null));
					sp.unblockSuccess(r);
				}, sp, ondone);
			}, sp, ondone);
			sp.onCancel(event -> {
				mem.cancel(event);
				if (fil.get() != null) fil.get().unblockCancel(event);
			});
			return operation(sp);
		}
		// all go to file
		if (file != null) {
			if (pos + len > size) size = pos + len;
			AsyncWork<Integer,IOException> task = file.writeAsync(pos - maxSizeInMemory, buffer, ondone);
			return operation(task);
		}
		AsyncWork<Integer,IOException> result = new AsyncWork<>();
		long p = pos;
		createFileAsync().listenInline(() -> {
			if (p + len > size) size = p + len;
			AsyncWork<Integer,IOException> task = file.writeAsync(p - maxSizeInMemory, buffer, ondone);
			operation(task).listenInline(result);
		}, result);
		return result;
	}
	
	@Override
	public AsyncWork<Integer,IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return writeAsync(pos, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				pos += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}

	@Override
	public void setSizeSync(long newSize) throws IOException {
		if (newSize == size) return;
		if (newSize < size) {
			// shrink
			if (newSize <= maxSizeInMemory) {
				// only memory remaining
				if (file != null) {
					File f = file.getFile();
					file.closeAsync().listenInline(() -> {
						if (!f.delete())
							LCCore.getApplication().getDefaultLogger()
								.warn("Unable to remove temporary file " + f.getAbsolutePath());
					});
					file = null;
				}
			} else {
				file.setSizeSync(newSize - maxSizeInMemory);
			}
			size = newSize;
			if (pos > size) pos = size;
			return;
		}
		// enlarge
		if (size < maxSizeInMemory) {
			// we need to enlarge memory
			int nbBuf = (int)(newSize / BUFFER_SIZE);
			if ((newSize % BUFFER_SIZE) != 0) nbBuf++;
			if (nbBuf > memory.length) {
				byte[][] n = new byte[nbBuf][];
				System.arraycopy(memory, 0, n, 0, memory.length);
				memory = n;
				for (int i = 0; i < memory.length; ++i)
					if (memory[i] == null)
						memory[i] = new byte[BUFFER_SIZE];
			}
		}
		if (newSize > maxSizeInMemory) {
			if (file == null)
				createFileSync();
			file.setSizeSync(newSize - maxSizeInMemory);
		}
		size = newSize;
	}
	
	@Override
	public AsyncWork<Void, IOException> setSizeAsync(long newSize) {
		if (newSize == size) return new AsyncWork<>(null, null);
		if (newSize < size) {
			// shrink
			if (newSize <= maxSizeInMemory) {
				// only memory remaining
				if (file != null) {
					File f = file.getFile();
					file.closeAsync().listenInline(() -> {
						if (!f.delete())
							LCCore.getApplication().getDefaultLogger()
								.warn("Unable to remove temporary file " + f.getAbsolutePath());
					});
					file = null;
				}
				Task.Cpu<Void, IOException> task = new Task.Cpu<Void, IOException>(
					"Shrink memory of IOInMemoryOrFile", getPriority()
				) {
					@Override
					public Void run() {
						int nbBuf = (int)(newSize / BUFFER_SIZE);
						if ((newSize % BUFFER_SIZE) != 0) nbBuf++;
						if (memory.length > nbBuf) {
							byte[][] newMem = new byte[nbBuf][];
							System.arraycopy(memory, 0, newMem, 0, nbBuf);
							memory = newMem;
						}
						size = newSize;
						if (pos > size) pos = size;
						return null;
					}
				};
				operation(task);
				task.start();
				return task.getOutput();
			}
			AsyncWork<Void, IOException> result = new AsyncWork<>();
			AsyncWork<Void, IOException> resize = operation(file.setSizeAsync(newSize - maxSizeInMemory));
			resize.listenInline(() -> {
				size = newSize;
				if (pos > size) pos = size;
				result.unblockSuccess(null);
			}, result);
			return result;
		}
		// enlarge
		AsyncWork<Void, IOException> taskMemory = null;
		if (size < maxSizeInMemory) {
			// we need to enlarge memory
			Task.Cpu<Void, IOException> task = new Task.Cpu<Void, IOException>(
					"Enlarge memory of IOInMemoryOrFile", getPriority()
				) {
					@Override
					public Void run() {
						int nbBuf = (int)(newSize / BUFFER_SIZE);
						if ((newSize % BUFFER_SIZE) != 0) nbBuf++;
						if (nbBuf > memory.length) {
							byte[][] n = new byte[nbBuf][];
							System.arraycopy(memory, 0, n, 0, memory.length);
							memory = n;
							for (int i = 0; i < memory.length; ++i)
								if (memory[i] == null)
									memory[i] = new byte[BUFFER_SIZE];
						}
						return null;
					}
			};
			task.start();
			taskMemory = operation(task.getOutput());
		}
		AsyncWork<Void, IOException> taskFile;
		if (newSize > maxSizeInMemory) {
			if (file != null)
				taskFile = operation(file.setSizeAsync(newSize - maxSizeInMemory));
			else {
				taskFile = new AsyncWork<>();
				createFileAsync().listenInline(() -> operation(file.setSizeAsync(newSize - maxSizeInMemory)).listenInline(taskFile),
					taskFile);
			}
		} else {
			taskFile = null;
		}
		AsyncWork<Void, IOException> result = new AsyncWork<>();
		JoinPoint.fromSynchronizationPointsSimilarError(taskMemory, taskFile).listenInline(() -> {
			size = newSize;
			result.unblockSuccess(null);
		}, result);
		return result;
	}
	
	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		int nb = readSync(pos, buffer);
		if (nb > 0) pos += nb;
		return nb;
	}
	
	@Override
	public int readSync(long pos, ByteBuffer buffer) throws IOException {
		if (pos < 0) pos = 0;
		if (pos > size) pos = size;
		if (pos == size)
			return 0;
		int len = buffer.remaining();
		if (pos + len > size) len = (int)(size - pos);
		if (pos < maxSizeInMemory) {
			// some data are in memory
			if (pos + len > maxSizeInMemory)
				len = (int)(maxSizeInMemory - pos);
			int p = (int)pos;
			int rem = len;
			do {
				int i = p / BUFFER_SIZE;
				int j = p % BUFFER_SIZE;
				int l = rem;
				if (l > BUFFER_SIZE - j) l = BUFFER_SIZE - j;
				if (memory[i] == null)
					throw new IOException("Try to read at " + pos + " but it was not written before !");
				buffer.put(memory[i], j, l);
				rem -= l;
				p += l;
			} while (rem > 0 && buffer.remaining() > 0);
			return len;
		}

		// data in file
		int nb;
		if (len < buffer.remaining()) {
			int limit = buffer.limit();
			buffer.limit(buffer.position() + len);
			nb = file.readSync(pos - maxSizeInMemory, buffer);
			buffer.limit(limit);
		} else {
			nb = file.readSync(pos - maxSizeInMemory, buffer);
		}
		
		return nb;
	}

	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		int nb = readFullySync(pos, buffer);
		if (nb > 0) pos += nb;
		return nb;
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		return IOUtil.readFullySync(this, pos, buffer);
	}

	@Override
	public AsyncWork<Integer,IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		if (pos < 0) pos = 0;
		if (pos > size) pos = size;
		if (pos == size) return IOUtil.success(Integer.valueOf(0), ondone);
		int len = buffer.remaining();
		if (pos + len > size) len = (int)(size - pos);
		if (pos < maxSizeInMemory) {
			// some data are in memory
			if (pos + len > maxSizeInMemory)
				len = (int)(maxSizeInMemory - pos);
			ReadInMemory task = new ReadInMemory((int)pos, len, buffer, ondone);
			operation(task);
			task.start();
			return task.getOutput();
		}
		// data in file
		AsyncWork<Integer,IOException> task;
		if (len < buffer.remaining()) {
			int limit = buffer.limit();
			buffer.limit(buffer.position() + len);
			task = file.readAsync(pos - maxSizeInMemory, buffer, param -> {
				buffer.limit(limit);
				if (ondone != null) ondone.accept(param);
			});
		} else {
			task = file.readAsync(pos - maxSizeInMemory, buffer, ondone);
		}
		return operation(task);
	}
	
	@Override
	public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return readAsync(pos, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				pos += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}
	
	@Override
	public AsyncWork<Integer,IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		if (pos < 0) pos = 0;
		if (pos > size) pos = size;
		if (pos == size) return IOUtil.success(Integer.valueOf(0), ondone);
		int len = buffer.remaining();
		if (pos + len > size) len = (int)(size - pos);
		ReadInMemory mem = null;
		if (pos < maxSizeInMemory) {
			// some data are in memory
			if (pos + len > maxSizeInMemory) {
				// not all
				mem = new ReadInMemory((int)pos, (int)(maxSizeInMemory - pos), buffer, null);
				pos = maxSizeInMemory;
				len -= (int)(maxSizeInMemory - pos);
				operation(mem.start());
			} else {
				mem = operation(new ReadInMemory((int)pos, len, buffer, ondone));
				mem.start();
				return mem.getOutput();
			}
		}
		// data in file
		if (mem == null) return readFromFile(pos - maxSizeInMemory, len, buffer, ondone);
		AsyncWork<Integer,IOException> sp = new AsyncWork<>();
		final int l = len;
		Mutable<AsyncWork<Integer,IOException>> fil = new Mutable<>(null);
		IOUtil.listenOnDone(mem.getOutput(), result -> {
			fil.set(operation(readFromFile(0, l, buffer, null)));
			IOUtil.listenOnDone(fil.get(), result2 -> {
				Integer r = Integer.valueOf(result.intValue() + result2.intValue());
				if (ondone != null) ondone.accept(new Pair<>(r, null));
				sp.unblockSuccess(r);
			}, sp, ondone);
		}, sp, ondone);
		ReadInMemory mm = mem;
		sp.onCancel(event -> {
			mm.cancel(event);
			if (fil.get() != null) fil.get().unblockCancel(event);
		});
		return sp;
	}
	
	@Override
	public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return readFullyAsync(pos, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				pos += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}
	
	private AsyncWork<Integer,IOException> readFromFile(
		long pos, int len, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		AsyncWork<Integer,IOException> task;
		if (len < buffer.remaining()) {
			int limit = buffer.limit();
			buffer.limit(buffer.position() + len);
			task = file.readFullyAsync(pos, buffer, param -> {
				buffer.limit(limit);
				if (ondone != null) ondone.accept(param);
			});
		} else {
			task = file.readFullyAsync(pos, buffer, ondone);
		}
		return operation(task);
	}

	private class ReadInMemory extends Task.Cpu<Integer,IOException> {
		private ReadInMemory(int pos, int len, ByteBuffer buf, Consumer<Pair<Integer,IOException>> ondone) {
			super("IOInMemoryOrFile: reading in memory", priority, ondone);
			this.pos = pos;
			this.len = len;
			this.buf = buf;
		}
		
		private int pos;
		private int len;
		private ByteBuffer buf;
		
		@Override
		public Integer run() throws IOException {
			if (memory == null) throw new IOException("IOInMemoryOrFile is already closed: " + getSourceDescription());
			Integer result = Integer.valueOf(len);
			do {
				int i = pos / BUFFER_SIZE;
				int j = pos % BUFFER_SIZE;
				int l = len;
				if (l > BUFFER_SIZE - j) l = BUFFER_SIZE - j;
				if (memory[i] == null)
					throw new IOException("Try to read at " + pos + " but it was not written before !");
				buf.put(memory[i], j, l);
				len -= l;
				pos += l;
			} while (len > 0 && buf.remaining() > 0);
			return result;
		}
	}
	
	private class WriteInMemory extends Task.Cpu<Integer,IOException> {
		private WriteInMemory(int pos, ByteBuffer buf, int len, Consumer<Pair<Integer,IOException>> ondone) {
			super("IOInMemoryOrFile: writing in memory", priority, ondone);
			this.pos = pos;
			this.len = len;
			this.buf = buf;
		}
		
		private int pos;
		private int len;
		private ByteBuffer buf;
		
		@Override
		public Integer run() throws IOException {
			if (memory == null) throw new IOException("IOInMemoryOrFile is already closed: " + getSourceDescription());
			Integer result = Integer.valueOf(len);
			do {
				int i = pos / BUFFER_SIZE;
				int j = pos % BUFFER_SIZE;
				int l = len;
				if (l > BUFFER_SIZE - j) l = BUFFER_SIZE - j;
				if (memory[i] == null) memory[i] = new byte[BUFFER_SIZE];
				buf.get(memory[i], j, l);
				len -= l;
				pos += l;
			} while (len > 0);
			return result;
		}
	}
	
}
