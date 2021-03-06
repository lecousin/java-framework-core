package net.lecousin.framework.io.buffering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.collections.map.LongMap;
import net.lecousin.framework.collections.map.LongMapRBT;
import net.lecousin.framework.collections.sort.OldestList;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.async.ReadWriteLockPoint;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.TaskManager;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.memory.IMemoryManageable;
import net.lecousin.framework.memory.MemoryManager;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.text.StringUtil;
import net.lecousin.framework.util.ConcurrentCloseable;
import net.lecousin.framework.util.Pair;

/**
 * BufferedIO is often the best buffering implementation when we need both bufferization and seek operations.
 * <br/>
 * Basically, it keeps a list of buffers corresponding to the different parts of the IO.
 * On first usage, a part of the IO is loaded into memory, and so read and write operations can be done quickly.
 * After some time without being used, a buffer can be unloaded to free memory.
 * <br/>
 * For a BufferedIO.ReadWrite, write operations are delayed: the data is kept in memory, so can be modified again or read quickly.
 * After 30 seconds without new write operation on a buffer, or if too much data has been written in memory and not yet flushed on
 * the underlying IO, a buffer is automatically flushed.
 * <br/>
 * <br/>
 * BufferedIOs are managed by the BufferedIO.MemoryManagement class.
 * It is responsible for the background tasks: write operations and free buffer no longer used.
 * <br/>
 * All BufferedIOs are managed together, so the MemoryManagement can decide of operations based on memory usage
 * considering all instances of BufferedIO.
 * <br/>
 * It checks regularly what is used and not used and which write operations need to be done. In case a maximum memory
 * is reached, and new memory is needed, it will free memory first. But it will try to do operations in background
 * as much as possible, and will decide to free memory when it detects we are close to reach the maximum memory usage.
 * <br/>
 * Using a single MemoryManagement for all BufferedIOs allows to manage memory globally for the application, so
 * many BufferedIOs can be used together while controlling the memory usage, and giving priority to buffers recently used.
 */
public class BufferedIO extends ConcurrentCloseable<IOException> implements IO.Readable.Seekable, IO.Readable.Buffered, IO.KnownSize {

	/** Manages memory usage and delayed write operations of BufferedIO instances. */
	public static class MemoryManagement implements IMemoryManageable {
		
		/** By default set to 72 MB. */
		public static final int DEFAULT_MEMORY_THRESHOLD = 72 * 1024 * 1024;
		/** By default set to 96 MB. */
		public static final int DEFAULT_MAX_MEMORY = 96 * 1024 * 1024;
		/** By default set to 12 MB. */
		public static final int DEFAULT_TO_BE_WRITTEN_THRESHOLD = 12 * 1024 * 1024;
		
		public static long getMemoryThreshold() { return get().memoryThreshold; }
		
		public static long getMaxMemory() { return get().maxMemory; }
		
		public static long getToBeWrittenThreshold() { return get().toBeWrittenThreshold; }
		
		/** Set the memory limits.
		 * @param memoryThreshold when this threshold is reached, it accelerates freeing some memory
		 * @param maxMemory maximum memory to use
		 * @param toBeWrittenThreshold when this threshold is reached, pending writes are executed
		 */
		public static void setMemoryLimits(long memoryThreshold, long maxMemory, long toBeWrittenThreshold) {
			MemoryManagement instance = get();
			instance.maxMemory = maxMemory;
			instance.memoryThreshold = memoryThreshold;
			instance.toBeWrittenThreshold = toBeWrittenThreshold;
		}

		/** Return the instance associated with the current Application. */
		private static synchronized MemoryManagement get() {
			Application app = LCCore.getApplication();
			MemoryManagement instance = app.getInstance(MemoryManagement.class);
			if (instance == null) {
				instance = new MemoryManagement();
				app.setInstance(MemoryManagement.class, instance);
			}
			return instance;
		}
		
		private MemoryManagement() {
			background = Task.cpu("BufferedIO Memory Management", Priority.LOW, new Background());
			background.executeEvery(30000, 45000);
			background.start();
			MemoryManager.register(this);
		}
		
		private Task<Void, NoException> background;
		private Buffer head = null;
		private Buffer tail = null;
		private long usedMemory = 0;
		private long maxMemory = DEFAULT_MAX_MEMORY;
		private long memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
		private long toBeWritten = 0;
		private long toBeWrittenThreshold = DEFAULT_TO_BE_WRITTEN_THRESHOLD;
		private long lastFree = 0;
		
		private void newBuffer(Buffer b) {
			synchronized (this) {
				if (head == null)
					head = tail = b;
				else {
					b.previous = tail;
					tail.next = b;
					tail = b;
				}
				
				usedMemory += b.data.length;
				if (usedMemory > maxMemory) {
					long now = System.currentTimeMillis();
					if (now - lastFree < 5000)
						freeBuffers(50, true);
					else if (now - lastFree < 15000)
						freeBuffers(25, true);
					else
						freeBuffers(10, true);
					background.executeNextOccurenceNow(Priority.RATHER_IMPORTANT);
				} else if (usedMemory > memoryThreshold) {
					background.executeNextOccurenceNow(Priority.NORMAL);
				}
			}
		}
		
		private void toWrite(Buffer b) {
			synchronized (this) {
				if (b.lastWrite == 0) {
					b.lastWrite = System.currentTimeMillis();
					toBeWritten += b.data.length;
					if (toBeWritten >= toBeWrittenThreshold)
						background.executeNextOccurenceNow(Priority.RATHER_IMPORTANT);
				} else {
					b.lastWrite = System.currentTimeMillis();
				}
			}
		}
		
		private IAsync<IOException> close(BufferedIO owner) {
			JoinPoint<IOException> jp = new JoinPoint<>();
			synchronized (this) {
				Buffer b = head;
				while (b != null) {
					if (b.owner != owner) {
						b = b.next;
						continue;
					}
					if (b.previous == null)
						head = b.next;
					else
						b.previous.next = b.next;
					if (b.next == null)
						tail = b.previous;
					else
						b.next.previous = b.previous;
					jp.addToJoin(b.loaded);
					Async<NoException> write = b.usage.startWriteAsync();
					if (write == null) {
						if (b.lastWrite == 0) {
							b.usage.endWrite();
							usedMemory -= b.data.length;
						} else {
							jp.addToJoin(1);
							Buffer bb = b;
							b.owner.flush(b).onDone(() -> {
								bb.usage.endWrite();
								synchronized (MemoryManagement.this) {
									usedMemory -= bb.data.length;
								}
								jp.joined();
							});
						}
					} else {
						jp.addToJoin(1);
						Buffer bb = b;
						write.onDone(() -> {
							if (bb.lastWrite == 0) {
								bb.usage.endWrite();
								synchronized (MemoryManagement.this) {
									usedMemory -= bb.data.length;
								}
								jp.joined();
							} else {
								bb.owner.flush(bb).onDone(() -> {
									bb.usage.endWrite();
									synchronized (MemoryManagement.this) {
										usedMemory -= bb.data.length;
									}
									jp.joined();
								});
							}
						});
					}
					b = b.next;
				}
			}
			jp.start();
			return jp;
		}

		private void freeBuffers(int nb, boolean urgent) {
			long now = System.currentTimeMillis();
			OldestList<Buffer> oldest = new OldestList<>(nb);
			synchronized (this) {
				for (Buffer b = head; b != null; b = b.next) {
					if (b.owner.closing) continue;
					if (b.usage.isUsed()) continue;
					if (b.lastWrite > 0) continue;
					if (!urgent && now - b.lastRead < 2000) continue;
					oldest.add(b.lastRead, b);
				}
				for (Buffer b : oldest) {
					if (b.owner.closing) continue;
					if (b.usage.isUsed()) continue;
					if (b.lastWrite > 0) continue;
					if (!b.owner.bufferTable.remove(b, false)) continue;
					if (b.previous == null)
						head = b.next;
					else
						b.previous.next = b.next;
					if (b.next == null)
						tail = b.previous;
					else
						b.next.previous = b.previous;
					usedMemory -= b.data.length;
					b.data = null;
				}
				lastFree = System.currentTimeMillis();
			}
		}
		
		private void removeReference(Buffer b) {
			synchronized (this) {
				if (b.previous == null)
					head = b.next;
				else
					b.previous.next = b.next;
				if (b.next == null)
					tail = b.previous;
				else
					b.next.previous = b.previous;
				usedMemory -= b.data.length;
				b.data = null;
			}
		}
		
		@Override
		public String getDescription() {
			return "BufferedIO Memory Management";
		}
		
		@Override
		public List<String> getItemsDescription() {
			List<String> list = new ArrayList<>(1);
			list.add("Buffers: " + StringUtil.size(usedMemory));
			return list;
		}
		
		@Override
		public void freeMemory(FreeMemoryLevel level) {
			background.executeNextOccurenceNow(Priority.RATHER_IMPORTANT);
			switch (level) {
			default:
			case EXPIRED_ONLY:
				freeBuffers(5, false);
				break;
			case LOW:
				freeBuffers(10, false);
				break;
			case MEDIUM:
				freeBuffers(25, false);
				break;
			case URGENT:
				freeBuffers(200, true);
				break;
			}
		}
		
		private class Background implements Executable<Void,NoException> {
			@Override
			public Void execute(Task<Void, NoException> taskContext) {
				background.setPriority(Task.Priority.LOW);
				long now = System.currentTimeMillis();
				int old1 = 0;
				int old2 = 0;
				OldestList<Buffer> oldestToBeWritten = null;
				if (toBeWritten >= toBeWrittenThreshold)
					oldestToBeWritten = new OldestList<>(32);
				synchronized (MemoryManagement.this) {
					for (Buffer b = head; b != null; b = b.next) {
						if (b.owner.closing ||
							b.usage.isUsed()) continue;
						if (b.lastWrite > 0) {
							if (now - b.lastWrite >= 30000)
								flush(b);
							else if (oldestToBeWritten != null)
								oldestToBeWritten.add(b.lastWrite, b);
						} else if (now - b.lastRead > 5L * 60 * 1000) {
							old1++;
						} else if (now - b.lastRead > 15L * 60 * 1000) {
							old2++;
						}
					}
				}
				if (oldestToBeWritten != null)
					flushOldest(oldestToBeWritten);
				if (now - lastFree > 5 * 60 * 1000)
					freeBuffers(10, false);
				else if (old2 > 0)
					freeBuffers(old2 > 10 ? old2 / 2 : 5, false);
				else if (old1 > 5)
					freeBuffers(old1 / 2, false);
				return null;
			}
			
			private void flushOldest(OldestList<Buffer> oldestToBeWritten) {
				long target = toBeWritten - toBeWrittenThreshold + toBeWrittenThreshold / 10;
				synchronized (MemoryManagement.this) {
					for (Buffer b : oldestToBeWritten) {
						if (b.owner.closing ||
							b.usage.isUsed() ||
							b.lastWrite == 0) continue;
						if (flush(b))
							target -= b.data.length;
						if (target < 0) break;
					}
				}
			}
			
			private boolean flush(Buffer b) {
				Async<NoException> ready = b.usage.startWriteAsync();
				if (ready != null) {
					ready.onDone(() -> b.usage.endWrite());
					return false;
				}
				if (b.lastWrite == 0) {
					b.usage.endWrite();
					return false;
				}
				IAsync<IOException> flushing = b.owner.flush(b);
				flushing.onDone(() -> {
					if (b.lastRead < b.lastWrite)
						b.lastRead = b.lastWrite;
					if (flushing.isSuccessful()) {
						flushed(b);
					}
					b.usage.endWrite();
				});
				return true;
			}
			
		}

		private void flushed(Buffer b) {
			b.lastWrite = 0;
			synchronized (MemoryManagement.this) {
				toBeWritten -= b.data.length;
			}
		}
		
	}
	
	private static class Buffer {
		private BufferedIO owner;
		private long index;
		private byte[] data;
		private long lastRead = 0;
		private long lastWrite = 0;
		private Async<IOException> loaded = new Async<>();
		private ReadWriteLockPoint usage = new ReadWriteLockPoint();
		private Buffer next;
		private Buffer previous;
	}
	
	private interface BufferTable {
		Buffer needBufferSync(long index, boolean newAtTheEnd);
		
		AsyncSupplier<Buffer, NoException> needBufferAsync(long index, boolean newAtTheEnd);
		
		boolean remove(Buffer buffer, boolean force);
		
		void setSize(long nbBuffersAfter);
		
		long getNbBuffers();
		
		IAsync<IOException> flush();
	}
	
	private class ArrayBufferTable implements BufferTable {
		private ArrayBufferTable(int nbBuffers) {
			buffers = new Buffer[nbBuffers];
		}
		
		private Buffer[] buffers;
		
		private Buffer initNeedBuffer(int i, boolean newAtTheEnd, MutableBoolean isNew) {
			Buffer b;
			synchronized (this) {
				if (newAtTheEnd && i == buffers.length) {
					Buffer[] newArray = new Buffer[i + 1];
					System.arraycopy(buffers, 0, newArray, 0, buffers.length);
					buffers = newArray;
				}
				if ((b = buffers[i]) == null) {
					b = new Buffer();
					b.owner = BufferedIO.this;
					b.index = i;
					b.data = new byte[i == 0 ? firstBufferSize : bufferSize];
					b.usage.startRead();
					buffers[i] = b;
					isNew.set(true);
				} else {
					b.lastRead = System.currentTimeMillis();
				}
			}
			if (isNew.get()) {
				memory.newBuffer(b);
				if (newAtTheEnd)
					b.loaded.unblock();
				else
					load(b);
			}
			return b;
		}
		
		@Override
		public Buffer needBufferSync(long index, boolean newAtTheEnd) {
			int i = (int)index;
			MutableBoolean isNew = new MutableBoolean(false);
			Buffer b = initNeedBuffer(i, newAtTheEnd, isNew);
			if (isNew.get())
				return b;
			b.usage.startRead();
			// in case it was been removed while not in the synchronized section, we need to check it is still the correct buffer
			synchronized (this) {
				if (buffers[i] == b)
					return b;
			}
			b.usage.endRead();
			return needBufferSync(index, newAtTheEnd);
		}
		
		@Override
		public AsyncSupplier<Buffer, NoException> needBufferAsync(long index, boolean newAtTheEnd) {
			int i = (int)index;
			MutableBoolean isNew = new MutableBoolean(false);
			Buffer b = initNeedBuffer(i, newAtTheEnd, isNew);
			if (isNew.get())
				return new AsyncSupplier<>(b, null);
			Async<NoException> sp = b.usage.startReadAsync();
			if (sp == null || sp.isDone()) {
				synchronized (this) {
					if (buffers[i] == b)
						return new AsyncSupplier<>(b, null);
				}
				b.usage.endRead();
				return needBufferAsync(index, newAtTheEnd);
			}
			AsyncSupplier<Buffer, NoException> result = new AsyncSupplier<>();
			sp.onDone(() -> {
				synchronized (this) {
					if (buffers[i] == b) {
						result.unblockSuccess(b);
						return;
					}
				}
				b.usage.endRead();
				needBufferAsync(index, newAtTheEnd).forward(result);
			});
			return result;
		}
		
		@Override
		public boolean remove(Buffer buffer, boolean force) {
			do {
				Async<?> sp;
				synchronized (this) {
					int i = (int)buffer.index;
					if (buffers[i] != buffer)
						return true;
					sp = buffer.usage.startWriteAsync();
					if (sp == null) {
						buffer.usage.endWrite();
						sp = buffer.loaded;
						if (sp.isDone()) {
							if (buffer.lastWrite > 0 && !force)
								return false;
							buffers[i] = null;
							return true;
						}
						if (!force)
							return false;
					} else {
						sp.onDone(() -> buffer.usage.endWrite());
						if (!force)
							return false;
					}
				}
				sp.block(0);
			} while (true);
		}
		
		@Override
		public long getNbBuffers() {
			return buffers.length;
		}
		
		@Override
		public void setSize(long nbBuffersAfter) {
			int nbBuffersBefore = buffers.length;
			if (nbBuffersAfter > nbBuffersBefore) {
				// only need to increase array size
				synchronized (this) {
					Buffer[] newArray = new Buffer[(int)nbBuffersAfter];
					System.arraycopy(buffers, 0, newArray, 0, buffers.length);
					buffers = newArray;
				}
				return;
			}
			// we need to ensure there is no flush in progress on the removed buffers and the last buffer
			synchronized (this) {
				// first, we remove the reference to those buffers from MemoryManagement so it won't do any new operation on them
				for (int i = (int)nbBuffersAfter; i < nbBuffersBefore; ++i)
					if (buffers[i] != null) {
						Buffer b = buffers[i];
						// the remove calls startWrite so we make sure it is not accessed anymore
						remove(b, true);
						memory.removeReference(b);
					}
				if (nbBuffersAfter > 0 && buffers[(int)(nbBuffersAfter - 1)] != null)
					buffers[(int)(nbBuffersAfter - 1)].usage.startWrite();
				Buffer[] newArray = new Buffer[(int)nbBuffersAfter];
				System.arraycopy(buffers, 0, newArray, 0, newArray.length);
				buffers = newArray;
				if (nbBuffersAfter > 0 && buffers[(int)(nbBuffersAfter - 1)] != null)
					buffers[(int)(nbBuffersAfter - 1)].usage.endWrite();
			}
		}
		
		@Override
		public IAsync<IOException> flush() {
			LinkedList<Buffer> toFlush = new LinkedList<>();
			synchronized (this) {
				for (Buffer b : buffers)
					if (b != null && b.lastWrite > 0)
						toFlush.add(b);
			}
			return BufferedIO.this.flush(toFlush, b -> {
				synchronized (ArrayBufferTable.this) {
					return buffers[(int)b.index] == b;
				}
			});
		}
		
	}
	
	private class MapBufferTable implements BufferTable {
		private LongMap<Buffer> map = new LongMapRBT<>(2500);
		private long nbBuffers = 0;

		private Buffer initNeedBuffer(long index, boolean newAtTheEnd, MutableBoolean isNew) {
			Buffer b;
			synchronized (this) {
				if ((b = map.get(index)) == null) {
					b = new Buffer();
					b.owner = BufferedIO.this;
					b.index = index;
					b.data = new byte[index == 0 ? firstBufferSize : bufferSize];
					b.usage.startRead();
					map.put(index, b);
					if (index + 1 > nbBuffers) nbBuffers = index + 1;
					isNew.set(true);
				} else {
					b.lastRead = System.currentTimeMillis();
				}
			}
			if (isNew.get()) {
				memory.newBuffer(b);
				if (newAtTheEnd)
					b.loaded.unblock();
				else
					load(b);
			}
			return b;
		}
		
		@Override
		public Buffer needBufferSync(long index, boolean newAtTheEnd) {
			MutableBoolean isNew = new MutableBoolean(false);
			Buffer b = initNeedBuffer(index, newAtTheEnd, isNew);
			if (isNew.get())
				return b;
			b.usage.startRead();
			// in case it was been removed while not in the synchronized section, we need to check it is still the correct buffer
			synchronized (this) {
				if (map.get(index) == b)
					return b;
			}
			b.usage.endRead();
			return needBufferSync(index, newAtTheEnd);
		}
		
		@Override
		public AsyncSupplier<Buffer, NoException> needBufferAsync(long index, boolean newAtTheEnd) {
			MutableBoolean isNew = new MutableBoolean(false);
			Buffer b = initNeedBuffer(index, newAtTheEnd, isNew);
			if (isNew.get())
				return new AsyncSupplier<>(b, null);
			Async<NoException> sp = b.usage.startReadAsync();
			if (sp == null || sp.isDone()) {
				synchronized (this) {
					if (map.get(index) == b)
						return new AsyncSupplier<>(b, null);
				}
				b.usage.endRead();
				return needBufferAsync(index, newAtTheEnd);
			}
			AsyncSupplier<Buffer, NoException> result = new AsyncSupplier<>();
			sp.onDone(() -> {
				synchronized (this) {
					if (map.get(index) == b) {
						result.unblockSuccess(b);
						return;
					}
				}
				b.usage.endRead();
				needBufferAsync(index, newAtTheEnd).forward(result);
			});
			return result;
		}
			
		@Override
		public boolean remove(Buffer buffer, boolean force) {
			do {
				Async<?> sp;
				synchronized (this) {
					if (map.get(buffer.index) != buffer)
						return true;
					sp = buffer.usage.startWriteAsync();
					if (sp == null) {
						buffer.usage.endWrite();
						sp = buffer.loaded;
						if (sp.isDone()) {
							if (buffer.lastWrite > 0 && !force)
								return false;
							map.remove(buffer.index);
							return true;
						}
						if (!force)
							return false;
					} else {
						sp.onDone(() -> buffer.usage.endWrite());
						if (!force)
							return false;
					}
				}
				sp.block(0);
			} while (true);
		}
		
		@Override
		public long getNbBuffers() {
			return nbBuffers;
		}

		@Override
		public void setSize(long nbBuffersAfter) {
			Buffer lastPrevious;
			synchronized (this) {
				if (nbBuffersAfter > nbBuffers) {
					// only new buffers, nothing to do
					nbBuffers = nbBuffersAfter;
					return;
				}
				// we need to ensure there is no flush in progress on the removed buffers and the last buffer
				// this is done by the remove function in which we do a startWrite
				lastPrevious = null;
				for (Iterator<Buffer> it = map.values(); it.hasNext(); ) {
					Buffer b = it.next();
					if (b.index == nbBuffersAfter - 1) {
						lastPrevious = b;
						continue;
					}
					if (b.index < nbBuffersAfter) continue;
					remove(b, true);
					memory.removeReference(b);
				}
				nbBuffers = nbBuffersAfter;
			}
			// make sure the last buffer is not flushing
			if (lastPrevious != null) {
				lastPrevious.usage.startWrite();
				lastPrevious.usage.endWrite();
			}
		}
		
		@Override
		public IAsync<IOException> flush() {
			LinkedList<Buffer> toFlush = new LinkedList<>();
			synchronized (this) {
				for (Iterator<Buffer> it = map.values(); it.hasNext(); ) {
					Buffer b = it.next();
					if (b.lastWrite > 0)
						toFlush.add(b);
				}
			}
			return BufferedIO.this.flush(toFlush, b -> {
				synchronized (MapBufferTable.this) {
					return map.get(b.index) == b;
				}
			});
		}
		
	}
	
	/** Constructor. */
	public BufferedIO(IO.Readable.Seekable io, long size, int firstBufferSize, int nextBuffersSize, boolean preLoadNextBuffer) {
		this.io = io;
		try { position = io.getPosition(); }
		catch (IOException e) { position = 0; }
		this.memory = MemoryManagement.get();
		this.size = size;
		this.firstBufferSize = firstBufferSize;
		this.bufferSize = nextBuffersSize;
		this.preLoadNextBuffer = preLoadNextBuffer;
		long nbBuffers = size <= firstBufferSize ? 1 : ((size - firstBufferSize) / bufferSize) + 2;
		if (nbBuffers < 10000)
			bufferTable = new ArrayBufferTable((int)nbBuffers);
		else
			bufferTable = new MapBufferTable();
		if (preLoadNextBuffer)
			preLoadBuffer(position);
		LCCore.getApplication().toClose(0, this);
		this.addCloseListener(() -> LCCore.getApplication().closed(this));
	}

	/** Constructor. */
	public BufferedIO(IO.Readable.Seekable io, int firstBufferSize, int nextBuffersSize, boolean preLoadNextBuffer) throws IOException {
		this(io, IOUtil.getSizeSync(io), firstBufferSize, nextBuffersSize, preLoadNextBuffer);
	}
	
	protected MemoryManagement memory;
	protected IO.Readable.Seekable io;
	protected long position = 0;
	protected long size;
	protected boolean closing = false;
	protected BufferTable bufferTable;
	protected boolean preLoadNextBuffer;
	protected int firstBufferSize;
	protected int bufferSize;
	
	@Override
	public String toString() {
		return "BufferedIO[" + (io != null ? io.getSourceDescription() : null) + "]";
	}
	
	private void load(Buffer b) {
		long pos = b.index == 0 ? 0 : (firstBufferSize + bufferSize * (b.index - 1));
		int len = size - pos < b.data.length ? (int)(size - pos) : b.data.length;
		if (len == 0) {
			b.loaded.unblock();
			return;
		}
		AsyncSupplier<Integer, IOException> read = io.readFullyAsync(pos, ByteBuffer.wrap(b.data, 0, len));
		read.onDone(nb -> {
			if (nb.intValue() != len)
				b.loaded.error(new IOException("Only " + nb + " bytes read at " + pos + " but expected was " + len));
			else
				b.loaded.unblock();
		}, b.loaded);
	}
	
	protected long getBufferIndex(long pos) {
		if (pos < firstBufferSize)
			return 0;
		return ((pos - firstBufferSize) / bufferSize) + 1;
	}
	
	protected int getBufferOffset(long pos) {
		if (pos < firstBufferSize)
			return (int)pos;
		return (int)((pos - firstBufferSize) % bufferSize);
	}
	
	protected long getBufferPosition(long index) {
		if (index == 0)
			return 0;
		return firstBufferSize + (index - 1) * bufferSize;
	}
	
	private IAsync<IOException> flush(Buffer b) {
		long pos = getBufferPosition(b.index);
		int len = b.data.length;
		if (pos + len > size) len = (int)(size - pos);
		if (len == 0)
			return new Async<>(true);
		return ((IO.Writable.Seekable)io).writeAsync(pos, ByteBuffer.wrap(b.data, 0, len));
	}
	
	private IAsync<IOException> flush(LinkedList<Buffer> list, Predicate<Buffer> filter) {
		JoinPoint<IOException> jp = new JoinPoint<>();
		jp.addToJoin(list.size());
		flushNext(list, filter, jp, 0);
		jp.start();
		return jp;
	}
	
	private void flushNext(LinkedList<Buffer> list, Predicate<Buffer> filter, JoinPoint<IOException> jp, int recurs) {
		if (list.isEmpty()) return;
		Buffer b = list.removeFirst();
		Async<NoException> sp = b.usage.startWriteAsync();
		if (sp == null)
			doFlush(b, jp, list, filter, recurs);
		else
			sp.thenStart("BufferedIO.flush", getPriority(), () -> doFlush(b, jp, list, filter, 0), true);
	}
	
	private void doFlush(Buffer b, JoinPoint<IOException> jp, LinkedList<Buffer> list, Predicate<Buffer> filter, int recurs) {
		if (!filter.test(b)) {
			b.usage.endWrite();
			jp.joined();
			flushNext(list, filter, jp, recurs + 1);
			return;
		}
		IAsync<IOException> flush = flush(b);
		flush.onDone(() -> {
			if (flush.isSuccessful())
				memory.flushed(b);
			b.usage.endWrite();
			jp.joined();
		});
		if (recurs > 250) {
			Task.cpu("BufferedIO.flush", getPriority(), new Executable.FromRunnable(() -> flushNext(list, filter, jp, 0))).start();
			return;
		}
		flushNext(list, filter, jp, recurs + 1);
	}
	
	protected void preLoadBuffer(long pos) {
		if (closing) return;
		if (pos == size) return;
		operation(Task.cpu("Pre-load next buffer in BufferedIO", io.getPriority(), task -> {
			if (closing) return null;
			long bufferIndex = getBufferIndex(pos);
			AsyncSupplier<Buffer, NoException> b = bufferTable.needBufferAsync(bufferIndex, false);
			b.onDone(() -> {
				b.getResult().lastRead = System.currentTimeMillis();
				b.getResult().usage.endRead();
			});
			return null;
		})).start();		
	}
	
	@Override
	protected IAsync<IOException> closeUnderlyingResources() {
		closing = true;
		Async<IOException> sp = new Async<>();
		memory.close(this).onDone(() -> io.closeAsync().onDone(sp), sp);
		return sp;
	}
	
	@Override
	protected void closeResources(Async<IOException> ondone) {
		io = null;
		bufferTable = null;
		memory = null;
		ondone.unblock();
	}
	
	@Override
	public Priority getPriority() {
		return io != null ? io.getPriority() : Task.Priority.NORMAL;
	}
	
	@Override
	public void setPriority(Priority priority) {
		io.setPriority(priority);
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
	public TaskManager getTaskManager() {
		return Threading.getCPUTaskManager();
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
		return position;
	}
	
	private static boolean waitFor(Buffer b) throws IOException {
		b.loaded.block(0);
		if (b.loaded.isCancelled()) {
			b.usage.endRead();
			return false;
		}
		if (b.loaded.hasError()) {
			b.usage.endRead();
			throw b.loaded.getError();
		}
		return true;
	}
	
	@Override
	public int read() throws IOException {
		if (closing) throw new ClosedChannelException();
		if (position == size) return -1;
		long bufferIndex = getBufferIndex(position);
		Buffer b = bufferTable.needBufferSync(bufferIndex, false);
		if (!waitFor(b)) return -1;
		int r = b.data[getBufferOffset(position)];
		position++;
		b.lastRead = System.currentTimeMillis();
		b.usage.endRead();
		if (preLoadNextBuffer && position < size && getBufferIndex(position) != bufferIndex)
			preLoadBuffer(position);
		return r;
	}
	
	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		if (closing) throw new ClosedChannelException();
		if (position == size) return -1;
		long bufferIndex = getBufferIndex(position);
		Buffer b = bufferTable.needBufferSync(bufferIndex, false);
		if (!waitFor(b)) return -1;
		int off = getBufferOffset(position);
		if (len > b.data.length - off) len = b.data.length - off;
		if (position + len > size) len = (int)(size - position);
		System.arraycopy(b.data, off, buffer, offset, len);
		position += len;
		b.lastRead = System.currentTimeMillis();
		b.usage.endRead();
		if (preLoadNextBuffer && position < size && getBufferIndex(position) != bufferIndex)
			preLoadBuffer(position);
		return len;
	}

	@Override
	public int readSync(long pos, ByteBuffer buffer) throws IOException {
		if (closing) throw new ClosedChannelException();
		if (pos >= size) return -1;
		long bufferIndex = getBufferIndex(pos);
		Buffer b = bufferTable.needBufferSync(bufferIndex, false);
		if (!waitFor(b)) return -1;
		int off = getBufferOffset(pos);
		int len = buffer.remaining();
		if (len > b.data.length - off) len = b.data.length - off;
		if (pos + len > size) len = (int)(size - pos);
		buffer.put(b.data, off, len);
		pos += len;
		b.lastRead = System.currentTimeMillis();
		b.usage.endRead();
		if (preLoadNextBuffer && pos < size && getBufferIndex(pos) != bufferIndex)
			preLoadBuffer(pos);
		return len;
	}
	
	@Override
	public int readSync(ByteBuffer buffer) throws IOException {
		int nb = readSync(position, buffer);
		if (nb > 0) position += nb;
		return nb;
	}
	
	@Override
	public int readFullySync(long pos, ByteBuffer buffer) throws IOException {
		if (closing) throw new ClosedChannelException();
		if (pos >= size) return -1;
		long bufferIndex = getBufferIndex(pos);
		Buffer b = bufferTable.needBufferSync(bufferIndex, false);
		if (!waitFor(b)) return -1;
		int len = buffer.remaining();
		if (pos + len > size) len = (int)(size - pos);
		for (long nextPos = pos + (bufferIndex == 0 ? firstBufferSize : bufferSize), count = 0;
			nextPos < pos + len && count < 5; nextPos = nextPos + bufferSize, ++count)
			preLoadBuffer(nextPos);
		int off = getBufferOffset(pos);
		if (len > b.data.length - off) len = b.data.length - off;
		buffer.put(b.data, off, len);
		pos += len;
		b.lastRead = System.currentTimeMillis();
		b.usage.endRead();
		if (!buffer.hasRemaining()) {
			if (preLoadNextBuffer && pos + len < size && getBufferIndex(pos + len) != bufferIndex)
				preLoadBuffer(pos + len);
			return len;
		}
		int total = len;
		do {
			if (pos == size) break;
			bufferIndex = getBufferIndex(pos);
			b = bufferTable.needBufferSync(bufferIndex, false);
			if (!waitFor(b)) return total;
			len = buffer.remaining();
			if (pos + len > size) len = (int)(size - pos);
			off = getBufferOffset(pos);
			if (len > b.data.length - off) len = b.data.length - off;
			buffer.put(b.data, off, len);
			total += len;
			pos += len;
			b.lastRead = System.currentTimeMillis();
			b.usage.endRead();
		} while (buffer.hasRemaining());
		if (preLoadNextBuffer && pos < size && getBufferIndex(pos) != bufferIndex)
			preLoadBuffer(pos);
		return total;
	}
	
	@Override
	public int readFullySync(ByteBuffer buffer) throws IOException {
		int nb = readFullySync(position, buffer);
		if (nb > 0) position += nb;
		return nb;
	}
	
	@Override
	public int readFully(byte[] buffer) throws IOException {
		int nb = readFullySync(position, ByteBuffer.wrap(buffer));
		if (nb > 0) position += nb;
		return nb;
	}
	
	@Override
	public IAsync<IOException> canStartReading() {
		if (position == size) return new Async<>(true);
		if (closing) return new Async<>(new ClosedChannelException());
		long bufferIndex = getBufferIndex(position);
		AsyncSupplier<Buffer, NoException> getBuffer = bufferTable.needBufferAsync(bufferIndex, false);
		if (getBuffer.isDone()) {
			Buffer b = getBuffer.getResult();
			b.usage.endRead();
			return b.loaded;
		}
		Async<IOException> sp = new Async<>();
		getBuffer.onDone(() -> {
			Buffer b = getBuffer.getResult();
			b.lastRead = System.currentTimeMillis();
			b.loaded.onDone(sp);
			b.usage.endRead();
		});
		return sp;
	}
	
	private static <T> boolean checkLoaded(Buffer b, AsyncSupplier<T, IOException> result, Consumer<Pair<T, IOException>> ondone) {
		if (b.loaded.hasError()) {
			IOUtil.error(b.loaded.getError(), result, ondone);
			b.usage.endRead();
			return false;
		}
		if (b.loaded.isCancelled()) {
			result.cancel(b.loaded.getCancelEvent());
			b.usage.endRead();
			return false;
		}
		return true;
	}
	
	@Override
	public int readAsync() throws IOException {
		if (closing) throw new ClosedChannelException();
		if (position == size) return -1;
		long bufferIndex = getBufferIndex(position);
		AsyncSupplier<Buffer, NoException> getBuffer = bufferTable.needBufferAsync(bufferIndex, false);
		if (!getBuffer.isDone()) {
			getBuffer.onDone(() -> {
				Buffer b = getBuffer.getResult();
				b.lastRead = System.currentTimeMillis();
				b.usage.endRead();
			});
			return -2;
		}
		Buffer b = getBuffer.getResult();
		if (!b.loaded.isDone()) {
			b.lastRead = System.currentTimeMillis();
			b.usage.endRead();
			return -2;
		}
		if (b.loaded.isCancelled()) {
			b.usage.endRead();
			return -1;
		}
		if (b.loaded.hasError()) {
			b.usage.endRead();
			throw b.loaded.getError();
		}
		int r = b.data[getBufferOffset(position)];
		position++;
		b.lastRead = System.currentTimeMillis();
		b.usage.endRead();
		if (preLoadNextBuffer && position < size && getBufferIndex(position) != bufferIndex)
			preLoadBuffer(position);
		return r;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		if (closing) return IOUtil.error(new ClosedChannelException(), ondone);
		if (pos >= size) return IOUtil.success(Integer.valueOf(-1), ondone);
		long bufferIndex = getBufferIndex(pos);
		AsyncSupplier<Buffer, NoException> getBuffer = bufferTable.needBufferAsync(bufferIndex, false);
		AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
		readAsync(pos, buffer, getBuffer, ondone, result);
		return operation(result);
	}
	
	private void readAsync(
		long pos, ByteBuffer buffer, AsyncSupplier<Buffer, NoException> getBuffer,
		Consumer<Pair<Integer, IOException>> ondone, AsyncSupplier<Integer, IOException> result
	) {
		getBuffer.onDone(() -> {
			Buffer b = getBuffer.getResult();
			b.loaded.thenStart("BufferedIO.readAsync", io.getPriority(), task -> {
				if (!checkLoaded(b, result, ondone)) return null;
				int off = getBufferOffset(pos);
				int len = buffer.remaining();
				long s = size;
				try {
					if (len > b.data.length - off) len = b.data.length - off;
					if (pos + len > s) len = (int)(s - pos);
					buffer.put(b.data, off, len);
					b.lastRead = System.currentTimeMillis();
				} catch (Exception t) {
					result.error(new IOException("Error reading from BufferedIO buffer at " + pos + " offset " + off + " len="
						+ len + " size=" + size + " (" + s + ") buffer len="
						+ (b.data != null ? Integer.toString(b.data.length) : null) + " remaining=" + buffer.remaining(), t));
				} finally {
					b.usage.endRead();
				}
				if (preLoadNextBuffer && pos + len < size && getBufferIndex(pos + len) != b.index)
					preLoadBuffer(pos + len);
				Integer nb = Integer.valueOf(len);
				if (ondone != null) ondone.accept(new Pair<>(nb, null));
				result.unblockSuccess(nb);
				return null;
			}, true);
		});
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return readAsync(position, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				position += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}
	
	@Override
	public AsyncSupplier<ByteBuffer, IOException> readNextBufferAsync(Consumer<Pair<ByteBuffer, IOException>> ondone) {
		if (closing) return IOUtil.error(new ClosedChannelException(), ondone);
		if (position >= size) return IOUtil.success(null, ondone);
		long bufferIndex = getBufferIndex(position);
		AsyncSupplier<Buffer, NoException> getBuffer = bufferTable.needBufferAsync(bufferIndex, false);
		AsyncSupplier<ByteBuffer, IOException> result = new AsyncSupplier<>();
		readNextBufferAsync(getBuffer, ondone, result);
		return operation(result);
	}
	
	private void readNextBufferAsync(
		AsyncSupplier<Buffer, NoException> getBuffer,
		Consumer<Pair<ByteBuffer, IOException>> ondone, AsyncSupplier<ByteBuffer, IOException> result
	) {
		getBuffer.onDone(() -> {
			Buffer b = getBuffer.getResult();
			b.loaded.thenStart("BufferedIO.readNextBufferAsync", io.getPriority(), task -> {
				if (!checkLoaded(b, result, ondone)) return null;
				ByteBuffer res = getBufferContent(b);
				if (ondone != null) ondone.accept(new Pair<>(res, null));
				result.unblockSuccess(res);
				return null;
			}, true);
		});
	}
	
	@Override
	public ByteBuffer readNextBuffer() throws IOException {
		if (closing) throw new ClosedChannelException();
		if (position >= size) return null;
		long bufferIndex = getBufferIndex(position);
		Buffer b = bufferTable.needBufferSync(bufferIndex, false);
		try {
			b.loaded.blockThrow(0);
		} catch (CancelException e) {
			throw IO.errorCancelled(e);
		}
		return getBufferContent(b);
	}
	
	private ByteBuffer getBufferContent(Buffer b) {
		long pos = position;
		int off = getBufferOffset(pos);
		int len = b.data.length - off;
		if (pos + len > size) len = (int)(size - pos);
		ByteBuffer res = ByteBuffer.wrap(b.data, off, len).asReadOnlyBuffer();
		pos += len;
		position = pos;
		b.lastRead = System.currentTimeMillis();
		b.usage.endRead();
		if (preLoadNextBuffer && pos < size && getBufferIndex(pos) != b.index)
			preLoadBuffer(pos);
		return res;
	}

	@Override
	public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return readFullySyncIfPossible(position, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				position += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}
	
	/** While readAsync methods are supposed to do the job in a separate thread, this method
	 * fills the given buffer synchronously if enough data is already buffered, else it finishes asynchronously.
	 * The caller can check the returned AsyncSupplier by calling its method isDone to know if the
	 * read has been performed synchronously.
	 * This method may be useful for processes that hope to work synchronously because this IO is buffered,
	 * but support also to work asynchronously without blocking a thread.
	 */
	public AsyncSupplier<Integer, IOException> readFullySyncIfPossible(
		long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
	) {
		if (closing) return IOUtil.error(new ClosedChannelException(), ondone);
		if (pos >= size) return IOUtil.success(Integer.valueOf(-1), ondone);
		long bufferIndex = getBufferIndex(pos);
		AsyncSupplier<Buffer, NoException> getBuffer = bufferTable.needBufferAsync(bufferIndex, false);
		if (!getBuffer.isDone()) {
			AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
			long p = pos;
			getBuffer.onDone(() -> {
				readFullyAsync(p, buffer, ondone).forward(result);
				Buffer b = getBuffer.getResult();
				b.usage.endRead();
			});
			return result;
		}
		Buffer b = getBuffer.getResult();
		if (!b.loaded.isDone()) {
			AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
			long p = pos;
			b.loaded.onDone(() -> {
				readFullyAsync(p, buffer, ondone).forward(result);
				b.usage.endRead();
			});
			return result;
		}
		if (b.loaded.hasError()) {
			IOException err = b.loaded.getError();
			b.usage.endRead();
			return IOUtil.error(err, ondone);
		}
		if (b.loaded.isCancelled()) {
			AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>(null, null, b.loaded.getCancelEvent());
			b.usage.endRead();
			return result;
		}
		int len = buffer.remaining();
		if (pos + len > size) len = (int)(size - pos);
		for (long nextPos = pos + (bufferIndex == 0 ? firstBufferSize : bufferSize), count = 0;
			nextPos < pos + len && count < 5; nextPos = nextPos + bufferSize, ++count)
			preLoadBuffer(nextPos);
		int off = getBufferOffset(pos);
		if (len > b.data.length - off) len = b.data.length - off;
		buffer.put(b.data, off, len);
		pos += len;
		b.lastRead = System.currentTimeMillis();
		b.usage.endRead();
		if (!buffer.hasRemaining()) {
			if (preLoadNextBuffer && pos < size && getBufferIndex(pos) != bufferIndex)
				preLoadBuffer(pos);
			Integer nb = Integer.valueOf(len);
			if (ondone != null) ondone.accept(new Pair<>(nb, null));
			return new AsyncSupplier<>(nb, null);
		}
		AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
		int l = len;
		readFullySyncIfPossible(pos, buffer, null).onDone(
			nb -> IOUtil.success(Integer.valueOf(nb.intValue() > 0 ? nb.intValue() + l : l), result, ondone),
			error -> IOUtil.error(error, result, ondone),
			result::cancel
		);
		return result;
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return IOUtil.readFullyAsync(this, pos, buffer, ondone);
	}
	
	@Override
	public AsyncSupplier<Integer, IOException> readFullyAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
		return readFullyAsync(position, buffer, res -> {
			if (res.getValue1() != null && res.getValue1().intValue() > 0)
				position += res.getValue1().intValue();
			if (ondone != null) ondone.accept(res);
		});
	}
	
	@Override
	public long skipSync(long n) {
		long prev = position;
		position += n;
		if (position > size) position = size;
		if (position < 0) position = 0;
		return position - prev;
	}
	
	@Override
	public int skip(int skip) {
		return (int)skipSync(skip);
	}
	
	@Override
	public AsyncSupplier<Long, IOException> skipAsync(long n, Consumer<Pair<Long, IOException>> ondone) {
		return IOUtil.success(Long.valueOf(skipSync(n)), ondone);
	}
	
	@Override
	public long seekSync(SeekType type, long move) {
		switch (type) {
		case FROM_BEGINNING:
			position = move;
			break;
		case FROM_END:
			position = size - move;
			break;
		default: //case FROM_CURRENT:
			position += move;
			break;
		}
		if (position > size) position = size;
		if (position < 0) position = 0;
		return position;
	}
	
	@Override
	public AsyncSupplier<Long, IOException> seekAsync(SeekType type, long move, Consumer<Pair<Long, IOException>> ondone) {
		return IOUtil.success(Long.valueOf(seekSync(type, move)), ondone);
	}
	
	/** Writable BufferedIO. */
	public static class ReadWrite extends BufferedIO implements IO.Writable.Seekable, IO.Writable.Buffered, IO.Resizable {
		
		/** Constructor. */
		public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.Resizable> ReadWrite(
			T io, long size, int firstBufferSize, int nextBuffersSize, boolean preLoadNextBuffer
		) {
			super(io, size, firstBufferSize, nextBuffersSize, preLoadNextBuffer);
		}

		/** Constructor. */
		public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.Resizable> ReadWrite(
			T io, int firstBufferSize, int nextBuffersSize, boolean preLoadNextBuffer
		) throws IOException {
			this(io, IOUtil.getSizeSync(io), firstBufferSize, nextBuffersSize, preLoadNextBuffer);
		}

		@Override
		public void write(byte value) throws IOException {
			if (closing) throw new ClosedChannelException();
			long bufferIndex = getBufferIndex(position);
			int off = getBufferOffset(position);
			Buffer b;
			b = bufferTable.needBufferSync(bufferIndex, position == size && off == 0);
			b.loaded.block(0);
			if (b.loaded.hasError()) throw b.loaded.getError();
			b.data[off] = value;
			position++;
			if (position > size) size = position;
			memory.toWrite(b);
			b.usage.endRead();
			if (preLoadNextBuffer && position < size && getBufferIndex(position) != bufferIndex)
				preLoadBuffer(position);
		}
		
		@Override
		public void write(byte[] buffer, int offset, int length) throws IOException {
			do {
				if (closing) throw new ClosedChannelException();
				long bufferIndex = getBufferIndex(position);
				int off = getBufferOffset(position);
				Buffer b;
				b = bufferTable.needBufferSync(bufferIndex, position == size && off == 0);
				b.loaded.block(0);
				if (b.loaded.hasError()) throw b.loaded.getError();
				int len = b.data.length - off;
				if (len > length) len = length;
				System.arraycopy(buffer, offset, b.data, off, len);
				position += len;
				if (position > size) size = position;
				memory.toWrite(b);
				b.usage.endRead();
				if (len == length) {
					if (preLoadNextBuffer && position < size && getBufferIndex(position) != bufferIndex)
						preLoadBuffer(position);
					return;
				}
				offset += len;
				length -= len;
			} while (true);
		}
		
		@Override
		public int writeSync(long pos, ByteBuffer buffer) throws IOException {
			if (pos > size) setSizeSync(pos);
			int total = 0;
			do {
				if (closing) throw new ClosedChannelException();
				long bufferIndex = getBufferIndex(pos);
				int off = getBufferOffset(pos);
				Buffer b;
				b = bufferTable.needBufferSync(bufferIndex, pos == size && off == 0);
				b.loaded.block(0);
				if (b.loaded.hasError()) throw b.loaded.getError();
				int len = buffer.remaining();
				if (len > b.data.length - off) len = b.data.length - off;
				buffer.get(b.data, off, len);
				pos += len;
				if (pos > size) size = pos;
				memory.toWrite(b);
				b.usage.endRead();
				total += len;
				if (!buffer.hasRemaining()) {
					if (preLoadNextBuffer && pos < size && getBufferIndex(pos) != bufferIndex)
						preLoadBuffer(pos);
					return total;
				}
			} while (true);
		}
		
		@Override
		public int writeSync(ByteBuffer buffer) throws IOException {
			int nb = writeSync(position, buffer);
			if (nb > 0) position += nb;
			return nb;
		}
		
		@Override
		public IAsync<IOException> canStartWriting() {
			return canStartReading();
		}
		
		@Override
		public AsyncSupplier<Integer, IOException> writeAsync(
			long pos, ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone
		) {
			if (closing) return IOUtil.error(new ClosedChannelException(), ondone);
			if (pos > size) {
				IAsync<IOException> resize = setSizeAsync(pos);
				AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
				resize.onDone(() -> {
					if (resize.hasError()) IOUtil.error(resize.getError(), result, ondone);
					else
						writeAsync(pos, buffer, ondone).forward(result);
				});
				return result;
			}
			long bufferIndex = getBufferIndex(pos);
			AsyncSupplier<Buffer, NoException> getBuffer =
				bufferTable.needBufferAsync(bufferIndex, pos == size && getBufferOffset(pos) == 0);
			AsyncSupplier<Integer, IOException> result = new AsyncSupplier<>();
			writeAsync(pos, buffer, getBuffer, ondone, result, 0);
			return operation(result);
		}
		
		private void writeAsync(
			long pos, ByteBuffer buffer, AsyncSupplier<Buffer, NoException> getBuffer,
			Consumer<Pair<Integer, IOException>> ondone, AsyncSupplier<Integer, IOException> result, int done
		) {
			getBuffer.onDone(() -> {
				Buffer b = getBuffer.getResult();
				b.loaded.thenStart("BufferedIO.writeAsync", io.getPriority(), task -> {
					if (!checkLoaded(b, result, ondone)) return null;
					int off = getBufferOffset(pos);
					int len = buffer.remaining();
					if (len > b.data.length - off) len = b.data.length - off;
					buffer.get(b.data, off, len);
					if (pos + len > size) size = pos + len;
					memory.toWrite(b);
					b.usage.endRead();
					if (preLoadNextBuffer && pos + len < size && getBufferIndex(pos + len) != b.index)
						preLoadBuffer(pos + len);
					if (!buffer.hasRemaining()) {
						IOUtil.success(Integer.valueOf(len + done), result, ondone);
					} else {
						long bufferIndex = getBufferIndex(pos + len);
						AsyncSupplier<Buffer, NoException> getNextBuffer = bufferTable.needBufferAsync(
							bufferIndex, pos + len == size && getBufferOffset(pos + len) == 0);
						writeAsync(pos + len, buffer, getNextBuffer, ondone, result, done + len);
					}
					return null;
				}, true);
			});
		}
		
		@Override
		public AsyncSupplier<Integer, IOException> writeAsync(ByteBuffer buffer, Consumer<Pair<Integer, IOException>> ondone) {
			return writeAsync(position, buffer, res -> {
				if (res.getValue1() != null && res.getValue1().intValue() > 0)
					position += res.getValue1().intValue();
				if (ondone != null) ondone.accept(res);
			});
		}
		
		@Override
		public void setSizeSync(long newSize) throws IOException {
			long nbBuffersBefore = bufferTable.getNbBuffers();
			long nbBuffersAfter = newSize <= firstBufferSize ? 1 : ((newSize - firstBufferSize) / bufferSize) + 2;
			if (nbBuffersBefore == nbBuffersAfter) {
				if (newSize < size) {
					Buffer b = bufferTable.needBufferSync(nbBuffersAfter - 1, false);
					b.usage.endRead();
					b.usage.startWrite(); // make sure we are not currently flushing after the new size
					size = newSize;
					b.usage.endWrite();
				} else {
					size = newSize;
				}
				if (position > size) position = size;
				((IO.Resizable)io).setSizeSync(newSize);
				return;
			}
			if (nbBuffersAfter > nbBuffersBefore) {
				((IO.Resizable)io).setSizeSync(newSize);
				size = newSize;
				bufferTable.setSize(nbBuffersAfter);
				if (position > size) position = size;
				return;
			}
			bufferTable.setSize(nbBuffersAfter);
			size = newSize;
			((IO.Resizable)io).setSizeSync(newSize);
			if (position > size) position = size;
		}
		
		@Override
		public IAsync<IOException> setSizeAsync(long newSize) {
			long nbBuffersBefore = bufferTable.getNbBuffers();
			long nbBuffersAfter = newSize <= firstBufferSize ? 1 : ((newSize - firstBufferSize) / bufferSize) + 2;
			if (nbBuffersBefore == nbBuffersAfter) {
				if (newSize < size) {
					Buffer b = bufferTable.needBufferSync(nbBuffersAfter - 1L, false);
					b.usage.endRead();
					b.usage.startWrite(); // make sure we are not currently flushing after the new size
					size = newSize;
					b.usage.endWrite();
					if (position > size) position = size;
					return ((IO.Resizable)io).setSizeAsync(newSize);
				}
				size = newSize;
				if (position > size) position = size;
				return ((IO.Resizable)io).setSizeAsync(newSize);
			}
			if (nbBuffersAfter > nbBuffersBefore) {
				Async<IOException> sp = new Async<>();
				((IO.Resizable)io).setSizeAsync(newSize).thenStart("BufferedIO.setSizeAsync", io.getPriority(), task -> {
					size = newSize;
					bufferTable.setSize(nbBuffersAfter);
					if (position > size) position = size;
					sp.unblock();
					return null;
				}, sp);
				return sp;
			}
			Async<IOException> sp = new Async<>();
			Task.cpu("BufferedIO.setSizeAsync", io.getPriority(), task -> {
				bufferTable.setSize(nbBuffersAfter);
				((IO.Resizable)io).setSizeAsync(newSize).onDone(sp);
				size = newSize;
				if (position > size) position = size;
				sp.unblock();
				return null;
			}).start();
			return sp;
		}
		
		@Override
		public IAsync<IOException> flush() {
			return bufferTable.flush();
		}
		
	}
	
}
