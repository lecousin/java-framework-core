package net.lecousin.framework.io.buffering;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.io.AbstractIO;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.mutable.Mutable;
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
public class IOInMemoryOrFile extends AbstractIO
	implements IO.Readable.Seekable, IO.Writable.Seekable, IO.KnownSize, IO.Resizable {

	/** Constructor. */
	public IOInMemoryOrFile(int maxSizeInMemory, byte priority, String sourceDescription) {
		super(sourceDescription, priority);
		int nbBuffers = maxSizeInMemory / BUFFER_SIZE;
		if ((maxSizeInMemory % BUFFER_SIZE) > 0) nbBuffers++;
		this.maxSizeInMemory = nbBuffers * BUFFER_SIZE;
		memory = new byte[nbBuffers][];
	}
	
	private static final int BUFFER_SIZE = 8192;
	
	private int maxSizeInMemory;
	private long pos = 0;
	private long size = 0;
	private byte[][] memory;
	private FileIO.ReadWrite file = null;
	
	private void createFileSync() throws IOException {
		this.file = TemporaryFiles.get().createAndOpenFileSync("net.lecousin.framework", "tempIO");
	}
	
	private Async<IOException> createFileAsync() {
		Async<IOException> sp = new Async<>();
		TemporaryFiles.get().createAndOpenFileAsync("net.lecousin.framework", "tempIO").onDone(f -> {
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
	public IAsync<IOException> canStartReading() {
		return new Async<>(true);
	}
	
	@Override
	public IAsync<IOException> canStartWriting() {
		return new Async<>(true);
	}
	
	@Override
	@SuppressWarnings("squid:S4042") // use of File.delete
	protected IAsync<IOException> closeUnderlyingResources() {
		if (file != null) {
			Async<IOException> sp = new Async<>();
			file.closeAsync().onDone(() -> {
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
	protected void closeResources(Async<IOException> ondone) {
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
	public AsyncSupplier<Long,IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long,IOException>> ondone) {
		seekSync(type, move);
		return IOUtil.success(Long.valueOf(pos), ondone);
	}
	
	@Override
	public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
		return IOUtil.success(Long.valueOf(skipSync(n)), ondone);
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
	public AsyncSupplier<Integer,IOException> writeAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		if (pos < 0) pos = 0;
		if (pos > size) {
			AsyncSupplier<Void, IOException> resize = setSizeAsync(pos);
			AsyncSupplier<Integer,IOException> result = new AsyncSupplier<>();
			long p = pos;
			IOUtil.listenOnDone(resize, res -> writeAsync(p, buffer, ondone).forward(result), result, ondone);
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
			AsyncSupplier<Integer, IOException> memReady;
			if (file == null) {
				memReady = new AsyncSupplier<>();
				Async<IOException> createFile = createFileAsync();
				JoinPoint.fromSimilarError(mem.getOutput(), createFile).onDone(
					() -> memReady.unblockSuccess(mem.getOutput().getResult()),
					memReady
				);
			} else {
				memReady = mem.getOutput();
			}
			AsyncSupplier<Integer,IOException> sp = new AsyncSupplier<>();
			Mutable<AsyncSupplier<Integer,IOException>> fil = new Mutable<>(null);
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
			AsyncSupplier<Integer,IOException> task = file.writeAsync(pos - maxSizeInMemory, buffer, ondone);
			return operation(task);
		}
		AsyncSupplier<Integer,IOException> result = new AsyncSupplier<>();
		long p = pos;
		createFileAsync().onDone(() -> {
			if (p + len > size) size = p + len;
			AsyncSupplier<Integer,IOException> task = file.writeAsync(p - maxSizeInMemory, buffer, ondone);
			operation(task).forward(result);
		}, result);
		return result;
	}
	
	@Override
	public AsyncSupplier<Integer,IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return writeAsync(pos, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				pos += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}

	@Override
	@SuppressWarnings("squid:S4042") // use of File.delete
	public void setSizeSync(long newSize) throws IOException {
		if (newSize == size) return;
		if (newSize < size) {
			// shrink
			if (newSize <= maxSizeInMemory) {
				// only memory remaining
				if (file != null) {
					File f = file.getFile();
					file.closeAsync().onDone(() -> {
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
	public AsyncSupplier<Void, IOException> setSizeAsync(long newSize) {
		if (newSize == size) return new AsyncSupplier<>(null, null);
		if (newSize < size)
			return shrinkSizeAsync(newSize);
		return enlargeSizeAsync(newSize);
	}
	
	@SuppressWarnings("squid:S4042") // use of File.delete
	private AsyncSupplier<Void, IOException> shrinkSizeAsync(long newSize) {
		if (newSize <= maxSizeInMemory) {
			// only memory remaining
			if (file != null) {
				File f = file.getFile();
				file.closeAsync().onDone(() -> {
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
		AsyncSupplier<Void, IOException> result = new AsyncSupplier<>();
		AsyncSupplier<Void, IOException> resize = operation(file.setSizeAsync(newSize - maxSizeInMemory));
		resize.onDone(() -> {
			size = newSize;
			if (pos > size) pos = size;
			result.unblockSuccess(null);
		}, result);
		return result;
	}
	
	private AsyncSupplier<Void, IOException> enlargeSizeAsync(long newSize) {
		AsyncSupplier<Void, IOException> taskMemory = null;
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
		AsyncSupplier<Void, IOException> taskFile;
		if (newSize > maxSizeInMemory) {
			if (file != null)
				taskFile = operation(file.setSizeAsync(newSize - maxSizeInMemory));
			else {
				taskFile = new AsyncSupplier<>();
				createFileAsync().onDone(() -> operation(file.setSizeAsync(newSize - maxSizeInMemory)).forward(taskFile),
					taskFile);
			}
		} else {
			taskFile = null;
		}
		AsyncSupplier<Void, IOException> result = new AsyncSupplier<>();
		JoinPoint.fromSimilarError(taskMemory, taskFile).onDone(() -> {
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
	public AsyncSupplier<Integer,IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
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
		AsyncSupplier<Integer,IOException> task;
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
	public AsyncSupplier<Integer,IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return readAsync(pos, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				pos += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}
	
	@Override
	public AsyncSupplier<Integer,IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
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
		AsyncSupplier<Integer,IOException> sp = new AsyncSupplier<>();
		final int l = len;
		Mutable<AsyncSupplier<Integer,IOException>> fil = new Mutable<>(null);
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
	public AsyncSupplier<Integer,IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone) {
		return readFullyAsync(pos, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				pos += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}
	
	private AsyncSupplier<Integer,IOException> readFromFile(
		long pos, int len, ByteBuffer buffer, Consumer<Pair<Integer,IOException>> ondone
	) {
		AsyncSupplier<Integer,IOException> task;
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
