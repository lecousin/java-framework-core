package net.lecousin.framework.io.buffering;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.TaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.AsyncWork.AsyncWorkListener;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.RunnableWithParameter;

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
public class IOInMemoryOrFile extends IO.AbstractIO implements IO.Readable.Seekable, IO.Writable.Seekable, IO.KnownSize, IO.Resizable {

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
	
	private void createFile() throws IOException {
		File file = File.createTempFile("net.lecousin.framework", "tempIO");
		file.deleteOnExit();
		this.file = new FileIO.ReadWrite(file, priority);
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
	protected SynchronizationPoint<IOException> closeIO() {
		if (file != null) {
			SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
			file.closeAsync().listenInline(new Runnable() {
				@Override
				public void run() {
					if (!file.getFile().delete())
						LCCore.getApplication().getDefaultLogger()
							.warn("Unable to remove temporary file: " + file.getFile().getAbsolutePath());
					file = null;
					sp.unblock();
				}
			});
			memory = null;
			return sp;
		}
		memory = null;
		return new SynchronizationPoint<>(true);
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
		// skip checkstyle: VariableDeclarationUsageDistance
		long prevPos = pos;
		this.pos += n;
		if (this.pos < 0) this.pos = 0;
		if (this.pos > size) this.pos = this.size;
		return this.pos - prevPos;
	}
	
	@Override
	public AsyncWork<Long,IOException> seekAsync(SeekType type, long move, RunnableWithParameter<Pair<Long,IOException>> ondone) {
		seekSync(type, move);
		if (ondone != null) ondone.run(new Pair<>(Long.valueOf(pos), null));
		return new AsyncWork<Long,IOException>(Long.valueOf(pos), null);
	}
	
	@Override
	public AsyncWork<Long, IOException> skipAsync(long n, RunnableWithParameter<Pair<Long, IOException>> ondone) {
		long skipped = skipSync(n);
		if (ondone != null) ondone.run(new Pair<>(Long.valueOf(skipped), null));
		return new AsyncWork<Long,IOException>(Long.valueOf(skipped), null);
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
		AsyncWork<Long, IOException> sp = new AsyncWork<Long, IOException>();
		sp.unblockSuccess(Long.valueOf(size));
		return sp;
	}

	@Override
	public int writeSync(ByteBuffer buffer) throws IOException {
		return writeSync(pos, buffer);
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
				this.pos = p;
				if (p + inMem > size) size = p + inMem;
				return len;
			}

			// some other will go to file
			if (file == null)
				createFile();
			
			int inFil = ((IO.Writable.Seekable)file).writeSync(0, buffer);
			if (inFil < len - inMem) len = inMem + inFil;
			this.pos = pos + len;
			if (pos + len > size) size = pos + len;
			return len;
		}

		// all go to file
		if (file == null)
			createFile();
		int nb = ((IO.Writable.Seekable)file).writeSync(pos - maxSizeInMemory, buffer);
		this.pos = pos + nb;
		if (pos + nb > size) size = pos + nb;
		return nb;
	}

	@Override
	public AsyncWork<Integer,IOException> writeAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		if (pos < 0) pos = 0;
		if (pos > size) {
			AsyncWork<Void, IOException> resize = setSizeAsync(pos);
			AsyncWork<Integer,IOException> result = new AsyncWork<>();
			long p = pos;
			resize.listenInline((res) -> {
				writeAsync(p, buffer, ondone).listenInline(result);
			}, (error) -> {
				if (ondone != null) ondone.run(new Pair<>(null, error));
				result.error(error);
			}, (cancel) -> {
				result.cancel(cancel);
			});
			return result;
		}
		int len = buffer.remaining();
		if (pos < maxSizeInMemory) {
			// some data will go to memory
			if (pos + len <= maxSizeInMemory) {
				// all go to memory
				Task<Integer,IOException> task = new WriteInMemory((int)pos, buffer, len, ondone);
				this.pos = pos + len;
				if (pos + len > size) size = pos + len;
				task.start();
				return task.getSynch();
			}
			// some other will go to file
			Task<Integer,IOException> mem = new WriteInMemory((int)pos, buffer, (int)(maxSizeInMemory - pos), null);
			mem.start();
			if (file == null)
				try { createFile(); }
				catch (IOException e) {
					if (ondone != null) ondone.run(new Pair<>(null, e));
					return new AsyncWork<Integer,IOException>(null, e);
				}
			AsyncWork<Integer,IOException> sp = new AsyncWork<>();
			Mutable<AsyncWork<Integer,IOException>> fil = new Mutable<>(null);
			this.pos = pos + len;
			if (pos + len > size) size = pos + len;
			mem.getSynch().listenInline(new Runnable() {
				@Override
				public void run() {
					if (mem.isCancelled()) {
						sp.unblockCancel(mem.getCancelEvent());
						return;
					}
					fil.set(((IO.Writable.Seekable)file).writeAsync(0, buffer));
					fil.get().listenInline(new AsyncWorkListener<Integer, IOException>() {
						@Override
						public void ready(Integer result) {
							Integer r = Integer.valueOf(mem.getResult().intValue() + result.intValue());
							if (ondone != null) ondone.run(new Pair<>(r, null));
							sp.unblockSuccess(r);
						}
						
						@Override
						public void error(IOException error) {
							if (ondone != null) ondone.run(new Pair<>(null, error));
							sp.unblockError(error);
						}
						
						@Override
						public void cancelled(CancelException event) {
							sp.unblockCancel(event);
						}
					});
				}
			});
			sp.listenCancel(new Listener<CancelException>() {
				@Override
				public void fire(CancelException event) {
					mem.cancel(event);
					if (fil.get() != null) fil.get().unblockCancel(event);
				}
			});
			return sp;
		}
		// all go to file
		if (file == null)
			try { createFile(); }
			catch (IOException e) {
				if (ondone != null) ondone.run(new Pair<>(null, e));
				return new AsyncWork<Integer,IOException>(null, e);
			}
		AsyncWork<Integer,IOException> task = ((IO.Writable.Seekable)file).writeAsync(pos - maxSizeInMemory, buffer, ondone);
		this.pos = pos + len;
		if (pos + len > size) size = pos + len;
		return task;
	}
	
	@Override
	public AsyncWork<Integer,IOException> writeAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return writeAsync(pos, buffer, ondone);
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
				int nbBuf = (int)(newSize / BUFFER_SIZE);
				if ((newSize % BUFFER_SIZE) != 0) nbBuf++;
				if (memory.length > nbBuf) {
					byte[][] newMem = new byte[nbBuf][];
					System.arraycopy(memory, 0, newMem, 0, nbBuf);
					memory = newMem;
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
				createFile();
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
				task.start();
				return task.getSynch();
			}
			AsyncWork<Void, IOException> result = new AsyncWork<>();
			AsyncWork<Void, IOException> resize = file.setSizeAsync(newSize - maxSizeInMemory);
			resize.listenInline(() -> {
				if (resize.isSuccessful()) {
					size = newSize;
					if (pos > size) pos = size;
					result.unblockSuccess(null);
				} else if (resize.hasError())
					result.unblockError(resize.getError());
				else
					result.unblockCancel(resize.getCancelEvent());
			});
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
			taskMemory = task.getSynch();
		}
		AsyncWork<Void, IOException> taskFile = null;
		if (newSize > maxSizeInMemory) {
			if (file == null)
				try { createFile(); }
				catch (IOException e) { return new AsyncWork<>(null, e); }
			taskFile = file.setSizeAsync(newSize - maxSizeInMemory);
		}
		AsyncWork<Void, IOException> result = new AsyncWork<>();
		JoinPoint.fromSynchronizationPointsSimilarError(taskMemory, taskFile).listenInline(() -> {
			size = newSize;
			result.unblockSuccess(null);
		}, (error) -> { result.error(error); }, (cancel) -> { result.cancel(cancel); });
		return result;
	}
	
	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		return readSync(pos, buffer);
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
			this.pos = pos + len;
			return len;
		}

		// data in file
		int nb;
		if (len < buffer.remaining()) {
			int limit = buffer.limit();
			buffer.limit(buffer.position() + len);
			nb = ((IO.Readable.Seekable)file).readSync(pos - maxSizeInMemory, buffer);
			buffer.limit(limit);
		} else
			nb = ((IO.Readable.Seekable)file).readSync(pos - maxSizeInMemory, buffer);
		
		if (nb > 0)
			this.pos = pos + nb;
		else
			this.pos = pos;
		return nb;
	}

	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		return readFullySync(pos, buffer);
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		return IOUtil.readFullySync(this, pos, buffer);
	}

	@Override
	public AsyncWork<Integer,IOException> readAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		if (pos < 0) pos = 0;
		if (pos > size) pos = size;
		if (pos == size) {
			if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(0), null));
			return new AsyncWork<Integer,IOException>(Integer.valueOf(0), null);
		}
		int len = buffer.remaining();
		if (pos + len > size) len = (int)(size - pos);
		if (pos < maxSizeInMemory) {
			// some data are in memory
			if (pos + len > maxSizeInMemory)
				len = (int)(maxSizeInMemory - pos);
			ReadInMemory task = new ReadInMemory((int)pos, len, buffer, ondone);
			this.pos = pos + len;
			task.start();
			return task.getSynch();
		}
		// data in file
		AsyncWork<Integer,IOException> task;
		final long p = pos;
		if (len < buffer.remaining()) {
			int limit = buffer.limit();
			buffer.limit(buffer.position() + len);
			task = ((IO.Readable.Seekable)file).readAsync(pos - maxSizeInMemory, buffer,
				new RunnableWithParameter<Pair<Integer,IOException>>() {
					@Override
					public void run(Pair<Integer, IOException> param) {
						buffer.limit(limit);
						if (param.getValue1() != null)
							IOInMemoryOrFile.this.pos = p + param.getValue1().intValue();
						if (ondone != null) ondone.run(param);
					}
				}
			);
		} else
			task = ((IO.Readable.Seekable)file).readAsync(pos - maxSizeInMemory, buffer,
				new RunnableWithParameter<Pair<Integer,IOException>>() {
					@Override
					public void run(Pair<Integer, IOException> param) {
						if (param.getValue1() != null)
							IOInMemoryOrFile.this.pos = p + param.getValue1().intValue();
						if (ondone != null) ondone.run(param);
					}
				}
			);
		return task;
	}
	
	@Override
	public AsyncWork<Integer,IOException> readAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return readAsync(pos, buffer, ondone);
	}
	
	@Override
	public AsyncWork<Integer,IOException> readFullyAsync(long pos, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		if (pos < 0) pos = 0;
		if (pos > size) pos = size;
		if (pos == size) {
			if (ondone != null) ondone.run(new Pair<>(Integer.valueOf(0), null));
			return new AsyncWork<Integer,IOException>(Integer.valueOf(0), null);
		}
		int len = buffer.remaining();
		if (pos + len > size) len = (int)(size - pos);
		ReadInMemory mem = null;
		if (pos < maxSizeInMemory) {
			// some data are in memory
			if (pos + len > maxSizeInMemory) {
				// not all
				mem = new ReadInMemory((int)pos, (int)(maxSizeInMemory - pos), buffer, null);
				pos = this.pos = maxSizeInMemory;
				len -= (int)(maxSizeInMemory - pos);
				mem.start();
			} else {
				mem = new ReadInMemory((int)pos, len, buffer, ondone);
				this.pos = pos + len;
				mem.start();
				return mem.getSynch();
			}
		}
		// data in file
		if (mem == null)
			return readFromFile(pos - maxSizeInMemory, len, buffer, ondone);
		AsyncWork<Integer,IOException> sp = new AsyncWork<Integer,IOException>();
		final int l = len;
		Mutable<AsyncWork<Integer,IOException>> fil = new Mutable<>(null);
		mem.getSynch().listenInline(new AsyncWorkListener<Integer, IOException>() {
			@Override
			public void cancelled(CancelException event) {
				sp.unblockCancel(event);
			}
			
			@Override
			public void error(IOException error) {
				if (ondone != null) ondone.run(new Pair<>(null, error));
				sp.unblockError(error);
			}
			
			@Override
			public void ready(Integer result) {
				fil.set(readFromFile(0, l, buffer, null));
				fil.get().listenInline(new AsyncWorkListener<Integer, IOException>() {
					@Override
					public void error(IOException error) {
						if (ondone != null) ondone.run(new Pair<>(null, error));
						sp.unblockError(error);
					}
					
					@Override
					public void ready(Integer result2) {
						Integer r = Integer.valueOf(result.intValue() + result2.intValue());
						if (ondone != null) ondone.run(new Pair<>(r, null));
						sp.unblockSuccess(r);
					}
					
					@Override
					public void cancelled(CancelException event) {
						sp.unblockCancel(event);
					}
				});
			}
		});
		ReadInMemory mm = mem;
		sp.listenCancel(new Listener<CancelException>() {
			@Override
			public void fire(CancelException event) {
				mm.cancel(event);
				if (fil.get() != null) fil.get().unblockCancel(event);
			}
		});
		return sp;
	}
	
	@Override
	public AsyncWork<Integer,IOException> readFullyAsync(ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
		return readFullyAsync(pos, buffer, ondone);
	}
	
	private AsyncWork<Integer,IOException> readFromFile(
		long pos, int len, ByteBuffer buffer, RunnableWithParameter<Pair<Integer,IOException>> ondone
	) {
		AsyncWork<Integer,IOException> task;
		if (len < buffer.remaining()) {
			int limit = buffer.limit();
			buffer.limit(buffer.position() + len);
			task = ((IO.Readable.Seekable)file).readFullyAsync(pos, buffer, new RunnableWithParameter<Pair<Integer,IOException>>() {
				@Override
				public void run(Pair<Integer, IOException> param) {
					buffer.limit(limit);
					if (param.getValue1() != null)
						IOInMemoryOrFile.this.pos = maxSizeInMemory + pos + param.getValue1().intValue();
					if (ondone != null) ondone.run(param);
				}
			});
		} else
			task = ((IO.Readable.Seekable)file).readFullyAsync(pos, buffer, new RunnableWithParameter<Pair<Integer,IOException>>() {
				@Override
				public void run(Pair<Integer, IOException> param) {
					if (param.getValue1() != null)
						IOInMemoryOrFile.this.pos = maxSizeInMemory + pos + param.getValue1().intValue();
					if (ondone != null) ondone.run(param);
				}
			});
		return task;
	}

	private class ReadInMemory extends Task.Cpu<Integer,IOException> {
		private ReadInMemory(int pos, int len, ByteBuffer buf, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
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
		private WriteInMemory(int pos, ByteBuffer buf, int len, RunnableWithParameter<Pair<Integer,IOException>> ondone) {
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
