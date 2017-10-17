package net.lecousin.framework.io.buffering;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.collections.sort.OldestList;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.memory.IMemoryManageable;
import net.lecousin.framework.util.StringUtil;

/**
 * Manage instances of BufferedIO to monitor memory usage.
 * @see BufferedIO
 */
public class BufferingManager implements Closeable, IMemoryManageable {

	private Background background;
	
	private BufferingManager() {
		background = new Background();
		background.start();
	}
	
	static synchronized BufferingManager get() {
		Application app = LCCore.getApplication();
		BufferingManager bm = app.getInstance(BufferingManager.class);
		if (bm == null) {
			bm = new BufferingManager();
			app.setInstance(BufferingManager.class, bm);
			app.toClose(bm);
		}
		return bm;
	}
	
	static class Buffer {
		BufferingManaged owner;
		byte[] buffer;
		int len;
		long lastRead;
		long lastWrite;
		int inUse = 0;
		boolean markedAsToBeWritten = false;
		LinkedList<AsyncWork<?,IOException>> flushing = null;
	}
	
	private LinkedArrayList<Buffer> buffers = new LinkedArrayList<>(25);
	private long totalMemory = 0;
	private long maxMemory = 64 * 1024 * 1024;
	private long memoryThreshold = 55 * 1024 * 1024;
	private long lastFree = 0;
	private long toBeWritten = 0;
	private long toBeWrittenThreshold = 12 * 1024 * 1024;
	
	public long getMemoryThreshold() { return memoryThreshold; }
	
	public long getMaxMemory() { return maxMemory; }
	
	public long getToBeWrittenThreshold() { return toBeWrittenThreshold; }
	
	/** Set the memory limits.
	 * @param memoryThreshold when this threshold is reached, it accelerates freeing some memory
	 * @param maxMemory maximum memory to use
	 * @param toBeWrittenThreshold when this threshold is reached, pending writes are executed
	 */
	public void setMemoryLimits(long memoryThreshold, long maxMemory, long toBeWrittenThreshold) {
		this.maxMemory = maxMemory;
		this.memoryThreshold = memoryThreshold;
		this.toBeWrittenThreshold = toBeWrittenThreshold;
	}
	
	void newBuffer(Buffer buffer) {
		synchronized (buffers) {
			if (buffer.owner.closing) return;
			buffers.add(buffer);
			totalMemory += buffer.buffer.length;
		}
		if (totalMemory > maxMemory) {
			long now = System.currentTimeMillis();
			if (now - lastFree < 5000)
				freeBuffers(50);
			else if (now - lastFree < 15000)
				freeBuffers(25);
			else
				freeBuffers(10);
			background.executeNextOccurenceNow(Task.PRIORITY_RATHER_IMPORTANT);
		} else if (totalMemory > memoryThreshold) {
			background.executeNextOccurenceNow(Task.PRIORITY_NORMAL);
		}
	}
	
	void removeBuffer(Buffer buffer) {
		synchronized (buffers) {
			if (!buffers.remove(buffer)) return;
			if (buffer.buffer != null) {
				totalMemory -= buffer.buffer.length;
				if (buffer.markedAsToBeWritten)
					toBeWritten -= buffer.buffer.length;
			}
		}
	}
	
	void toBeWritten(Buffer buffer) {
		synchronized (buffers) {
			if (!buffer.markedAsToBeWritten) {
				buffer.markedAsToBeWritten = true;
				toBeWritten += buffer.buffer.length;
			}
			buffer.lastWrite = System.currentTimeMillis();
			if (toBeWritten > toBeWrittenThreshold)
				background.executeNextOccurenceNow(Task.PRIORITY_RATHER_IMPORTANT);
		}
	}
	
	ISynchronizationPoint<IOException> close(BufferingManaged owner) {
		owner.closing = true;
		ArrayList<AsyncWork<?,?>> tasks = new ArrayList<>();
		synchronized (buffers) {
			for (Iterator<Buffer> it = buffers.iterator(); it.hasNext(); ) {
				Buffer b = it.next();
				if (b.owner != owner) continue;
				if (b.lastWrite > 0) {
					tasks.add(b.owner.flushWrite(b));
					toBeWritten -= b.buffer.length;
				}
				synchronized (b) {
					if (b.flushing != null) tasks.addAll(b.flushing);
				}
				it.remove();
				totalMemory -= b.buffer.length;
				b.owner = null;
			}
		}
		SynchronizationPoint<IOException> sp = new SynchronizationPoint<>();
		JoinPoint.fromSynchronizationPoints(tasks).listenInline(new Runnable() {
			@Override
			public void run() {
				owner.closed().listenInline(sp);
			}
		});
		return sp;
	}
	
	ISynchronizationPoint<IOException> fullFlush(BufferingManaged owner) {
		JoinPoint<IOException> jp = new JoinPoint<>();
		synchronized (buffers) {
			for (Iterator<Buffer> it = buffers.iterator(); it.hasNext(); ) {
				Buffer b = it.next();
				if (b.owner != owner) continue;
				if (b.lastWrite > 0) {
					jp.addToJoin(b.owner.flushWrite(b));
					b.lastWrite = 0;
					b.markedAsToBeWritten = false;
					toBeWritten -= b.buffer.length;
				}
				synchronized (b) {
					if (b.flushing != null) {
						while (!b.flushing.isEmpty() && b.flushing.getFirst().isUnblocked())
							b.flushing.removeFirst();
						if (b.flushing.isEmpty())
							b.flushing = null;
						else
							for (AsyncWork<?,IOException> flush : b.flushing)
								jp.addToJoin(flush);
					}
				}
			}
		}
		jp.start();
		return jp;
	}
	
	private void freeBuffers(int nb) {
		OldestList<Buffer> oldest = new OldestList<>(nb);
		synchronized (buffers) {
			for (Buffer b : buffers) {
				if (b.inUse > 0) continue;
				if (b.lastWrite > 0) continue;
				synchronized (b) {
					if (b.flushing != null) {
						while (!b.flushing.isEmpty() && b.flushing.getFirst().isUnblocked())
							b.flushing.removeFirst();
						if (b.flushing.isEmpty())
							b.flushing = null;
						else
							continue;
					}
				}
				oldest.add(b.lastRead, b);
			}
			for (Buffer b : oldest) {
				synchronized (b) {
					if (b.inUse > 0) continue;
					if (b.lastWrite > 0) continue;
					b.owner.removed(b);
					buffers.remove(b);
					totalMemory -= b.buffer.length;
					b.buffer = null;
				}
			}
			lastFree = System.currentTimeMillis();
		}
	}
	
	private class Background extends Task.Cpu<Void,NoException> {
		public Background() {
			super("Buffering Manager", Task.PRIORITY_LOW);
			executeEvery(30000, 45000);
		}
		
		@Override
		public Void run() {
			setPriority(Task.PRIORITY_LOW);
			long now = System.currentTimeMillis();
			int old = 0;
			OldestList<Buffer> oldestToBeWritten = null;
			if (toBeWritten >= toBeWrittenThreshold)
				oldestToBeWritten = new OldestList<>(10);
			synchronized (buffers) {
				for (Iterator<Buffer> it = buffers.iterator(); it.hasNext(); ) {
					Buffer b = it.next();
					synchronized (b) {
						if (b.flushing != null) {
							while (!b.flushing.isEmpty() && b.flushing.getFirst().isUnblocked())
								b.flushing.removeFirst();
							if (b.flushing.isEmpty())
								b.flushing = null;
						}
						if (b.inUse > 0) continue;
						if (b.lastWrite > 0) {
							if (now - b.lastWrite >= 30000) {
								if (b.flushing == null) b.flushing = new LinkedList<>();
								b.flushing.add(b.owner.flushWrite(b));
								if (b.lastRead < b.lastWrite)
									b.lastRead = b.lastWrite;
								b.lastWrite = 0;
								b.markedAsToBeWritten = false;
								toBeWritten -= b.buffer.length;
							} else if (oldestToBeWritten != null) {
								oldestToBeWritten.add(b.lastWrite, b);
							}
						} else if (now - b.lastRead > 15 * 60 * 1000)
							old++;
					}
				}
			}
			if (oldestToBeWritten != null)
				for (Buffer b : oldestToBeWritten) {
					synchronized (b) {
						if (b.inUse > 0) continue;
						if (b.lastWrite <= 0) continue;
						if (b.flushing == null) b.flushing = new LinkedList<>();
						b.flushing.add(b.owner.flushWrite(b));
						if (b.lastRead < b.lastWrite)
							b.lastRead = b.lastWrite;
						b.lastWrite = 0;
						b.markedAsToBeWritten = false;
						toBeWritten -= b.buffer.length;
					}
				}
			if (now - lastFree > 5 * 60 * 1000)
				freeBuffers(10);
			else if (old > 0)
				freeBuffers(5);
			return null;
		}
	}
	
	// skip checkstyle: OverloadMethodsDeclarationOrder
	@Override
	public void close() {
		ArrayList<AsyncWork<?,IOException>> tasks = new ArrayList<>();
		synchronized (buffers) {
			for (Iterator<Buffer> it = buffers.iterator(); it.hasNext(); ) {
				Buffer b = it.next();
				if (b.lastWrite > 0)
					tasks.add(b.owner.flushWrite(b));
				if (b.flushing != null)
					tasks.addAll(b.flushing);
			}
			buffers.clear();
		}
		try { Threading.waitUnblockedWithError(tasks); }
		catch (Throwable t) {
			background.getApplication().getDefaultLogger().error("Error flushing remaining buffers", t);
		}
	}

	@Override
	public String getDescription() {
		return "BufferingManager";
	}

	@Override
	public List<String> getItemsDescription() {
		List<String> list = new ArrayList<>(1);
		list.add("" + buffers.size() + " buffers: " + StringUtil.size(totalMemory));
		return list;
	}

	@Override
	public void freeMemory(FreeMemoryLevel level) {
		background.executeNextOccurenceNow(Task.PRIORITY_RATHER_IMPORTANT);
		switch (level) {
		default:
		case EXPIRED_ONLY:
			freeBuffers(5);
			break;
		case LOW:
			freeBuffers(10);
			break;
		case MEDIUM:
			freeBuffers(25);
			break;
		case URGENT:
			freeBuffers(200);
			break;
		}
	}
	
}
