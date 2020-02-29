package net.lecousin.framework.memory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.collections.sort.RedBlackTreeInteger;
import net.lecousin.framework.collections.sort.RedBlackTreeInteger.Node;

/**
 * Cache of array useful when arrays of same size may be needed, to avoid memory allocation and garbage collection.
 * @param <T> type of array
 */
public abstract class ArrayCache<T> implements IMemoryManageable {
	
	/** Maximum number of buffers of the same size to keep, when size is under 128K, default to 20. */
	private int maxBuffersBySizeUnder128KB = 20;
	/** Maximum number of buffers of the same size to keep, when size is above 128K, default to 8. */
	private int maxBuffersBySizeAbove128KB = 8;
	/** Maximum total size of buffers to keep in this cache, default to 64MB. */
	private int maxTotalSize = 64 * 1024 * 1024;
	/** Time before a cached buffer can be removed to free memory, default 5 minutes. */
	private long timeBeforeToRemove = 5L * 60 * 1000;

	/** Maximum number of buffers of the same size to keep, when size is under 128K, default to 20. */
	public int getMaxBuffersBySizeUnder128KB() {
		return maxBuffersBySizeUnder128KB;
	}

	/** Maximum number of buffers of the same size to keep, when size is under 128K, default to 20. */
	public void setMaxBuffersBySizeUnder128KB(int maxBuffersBySizeUnder128KB) {
		this.maxBuffersBySizeUnder128KB = maxBuffersBySizeUnder128KB;
	}

	/** Maximum number of buffers of the same size to keep, when size is above 128K, default to 8. */
	public int getMaxBuffersBySizeAbove128KB() {
		return maxBuffersBySizeAbove128KB;
	}

	/** Maximum number of buffers of the same size to keep, when size is above 128K, default to 8. */
	public void setMaxBuffersBySizeAbove128KB(int maxBuffersBySizeAbove128KB) {
		this.maxBuffersBySizeAbove128KB = maxBuffersBySizeAbove128KB;
	}

	/** Maximum total size of buffers to keep in this cache. */
	public int getMaxTotalSize() {
		return maxTotalSize;
	}

	/** Maximum total size of buffers to keep in this cache. */
	public void setMaxTotalSize(int maxTotalSize) {
		this.maxTotalSize = maxTotalSize;
	}

	/** Time before a cached buffer can be removed to free memory. */
	public long getTimeBeforeToRemove() {
		return timeBeforeToRemove;
	}

	/** Time before a cached buffer can be removed to free memory. */
	public void setTimeBeforeToRemove(long timeBeforeToRemove) {
		this.timeBeforeToRemove = timeBeforeToRemove;
	}

	private static class ArraysBySize<T> {
		private TurnArray<T> arrays;
		private long lastCachedTime;
		private long lastUsageTime;
	}
	
	private RedBlackTreeInteger<ArraysBySize<T>> arraysBySize = new RedBlackTreeInteger<>();
	private int totalSize = 0;
	
	protected ArrayCache() {
		MemoryManager.register(this);
	}
	
	protected abstract T allocate(int size);
	
	protected abstract int length(T buf);
	
	/** Get an array. */
	public T get(int size, boolean acceptGreater) {
		Node<ArraysBySize<T>> node;
		T buf;
		synchronized (arraysBySize) {
			if (acceptGreater)
				node = arraysBySize.searchNearestHigher(size, true);
			else
				node = arraysBySize.get(size);
			// limit to 3/2 of the size
			if (node != null && acceptGreater && node.getValue() > size * 3 / 2)
				node = null;
			if (node == null)
				buf = null;
			else {
				ArraysBySize<T> arrays = node.getElement();
				arrays.lastUsageTime = System.currentTimeMillis();
				buf = arrays.arrays.poll();
				if (arrays.arrays.isEmpty()) arraysBySize.remove(node);
			}
		}
		if (buf == null)
			try {
				return allocate(size);
			} catch (OutOfMemoryError e) {
				MemoryManager.freeMemory(FreeMemoryLevel.URGENT);
				return allocate(size);
			}
		totalSize -= length(buf);
		return buf;
	}
	
	/** Put an array in the cache. */
	public void free(T array) {
		int len = length(array);
		if (len < 16) return;
		synchronized (arraysBySize) {
			if (totalSize + len > maxTotalSize) return;
			Node<ArraysBySize<T>> node = arraysBySize.get(len);
			if (node == null) {
				ArraysBySize<T> arrays = new ArraysBySize<>();
				arrays.arrays = new TurnArray<>(
					len >= 128 * 1024 ? maxBuffersBySizeAbove128KB : maxBuffersBySizeUnder128KB);
				arrays.arrays.add(array);
				arrays.lastCachedTime = System.currentTimeMillis();
				arraysBySize.add(len, arrays);
				totalSize += len;
				return;
			}
			ArraysBySize<T> arrays = node.getElement();
			arrays.lastCachedTime = System.currentTimeMillis();
			if (arrays.arrays.isFull()) return;
			totalSize += len;
			arrays.arrays.add(array);
		}
	}

	@Override
	public List<String> getItemsDescription() {
		return Arrays.asList("Total cached size: " + totalSize);
	}

	@Override
	public void freeMemory(FreeMemoryLevel level) {
		long now = System.currentTimeMillis();
		switch (level) {
		default:
		case EXPIRED_ONLY:
			freeMemory(now - timeBeforeToRemove, 2);
			break;
		case LOW:
			freeMemory(now - timeBeforeToRemove / 2, 4);
			break;
		case MEDIUM:
			freeMemory(now - timeBeforeToRemove / 3, 10);
			break;
		case URGENT:
			synchronized (arraysBySize) {
				arraysBySize.clear();
				totalSize = 0;
			}
			break;
		}
	}
	
	private void freeMemory(long beforeTime, int nbToRemove) {
		List<Node<ArraysBySize<T>>> toRemove = new LinkedList<>();
		synchronized (arraysBySize) {
			for (Iterator<Node<ArraysBySize<T>>> it = arraysBySize.nodeIterator(); it.hasNext(); ) {
				Node<ArraysBySize<T>> node = it.next();
				ArraysBySize<T> arrays = node.getElement();
				if (arrays.lastUsageTime < beforeTime) {
					if (arrays.lastCachedTime > beforeTime && arrays.arrays.size() <= 2)
						continue;
					for (int i = 0; i < nbToRemove && !arrays.arrays.isEmpty(); ++i) {
						arrays.arrays.poll();
						totalSize -= node.getValue();
					}
					if (arrays.arrays.isEmpty())
						toRemove.add(node);
				}
			}
			for (Node<ArraysBySize<T>> node : toRemove)
				arraysBySize.remove(node);
		}
	}
	
}
