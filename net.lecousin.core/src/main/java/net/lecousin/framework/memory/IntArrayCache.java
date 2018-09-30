package net.lecousin.framework.memory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.collections.sort.RedBlackTreeInteger;
import net.lecousin.framework.collections.sort.RedBlackTreeInteger.Node;

public class IntArrayCache implements IMemoryManageable {
	
	public static IntArrayCache getInstance() {
		Application app = LCCore.getApplication();
		synchronized (IntArrayCache.class) {
			IntArrayCache instance = app.getInstance(IntArrayCache.class);
			if (instance != null)
				return instance;
			app.setInstance(IntArrayCache.class, instance = new IntArrayCache());
			return instance;
		}
	}
	
	public int MAX_BUFFERS_BY_SIZE_UNDER_128KB = 20;
	public int MAX_BUFFERS_BY_SIZE_ABOVE_128KB = 8;
	public int MAX_TOTAL_SIZE = 16 * 1024 * 1024;
	public long TIME_BEFORE_TO_REMOVE_ONE = 5 * 60 * 1000; // every 5 minutes

	private static class ArraysBySize {
		private TurnArray<int[]> arrays;
		private long lastCachedTime;
		private long lastUsageTime;
	}
	
	private RedBlackTreeInteger<ArraysBySize> arraysBySize = new RedBlackTreeInteger<>();
	private int totalSize = 0;
	
	private IntArrayCache() {
		// TODO background task in addition to the memory manager ?
		MemoryManager.register(this);
	}
	
	public int[] get(int size, boolean acceptGreater) {
		Node<ArraysBySize> node;
		int[] buf;
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
				ArraysBySize arrays = node.getElement();
				arrays.lastUsageTime = System.currentTimeMillis();
				buf = arrays.arrays.poll();
			}
		}
		if (buf == null)
			return new int[size];
		totalSize -= buf.length;
		return buf;
	}
	
	public void free(int[] array) {
		int len = array.length;
		synchronized (arraysBySize) {
			if (totalSize + len > MAX_TOTAL_SIZE) return;
			Node<ArraysBySize> node = arraysBySize.get(len);
			if (node == null) {
				ArraysBySize arrays = new ArraysBySize();
				arrays.arrays = new TurnArray<>(
					len >= 128 * 1024 ? MAX_BUFFERS_BY_SIZE_ABOVE_128KB : MAX_BUFFERS_BY_SIZE_UNDER_128KB);
				arrays.arrays.add(array);
				arrays.lastCachedTime = System.currentTimeMillis();
				arraysBySize.add(len, arrays);
				totalSize += len;
				return;
			}
			ArraysBySize arrays = node.getElement();
			arrays.lastCachedTime = System.currentTimeMillis();
			if (arrays.arrays.isFull()) return;
			totalSize += len;
			arrays.arrays.add(array);
		}
	}

	@Override
	public String getDescription() {
		return "int arrays cache";
	}

	@Override
	public List<String> getItemsDescription() {
		return Arrays.asList("Total cached size: " + (totalSize * 4));
	}

	@Override
	public void freeMemory(FreeMemoryLevel level) {
		long now = System.currentTimeMillis();
		switch (level) {
		default:
		case EXPIRED_ONLY:
			freeMemory(now - TIME_BEFORE_TO_REMOVE_ONE, 1);
			break;
		case LOW:
			freeMemory(now - TIME_BEFORE_TO_REMOVE_ONE / 2, 2);
			break;
		case MEDIUM:
			freeMemory(now - TIME_BEFORE_TO_REMOVE_ONE / 3, 5);
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
		List<Node<ArraysBySize>> toRemove = new LinkedList<>();
		synchronized (arraysBySize) {
			for (Iterator<Node<ArraysBySize>> it = arraysBySize.nodeIterator(); it.hasNext(); ) {
				Node<ArraysBySize> node = it.next();
				ArraysBySize arrays = node.getElement();
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
			for (Node<ArraysBySize> node : toRemove)
				arraysBySize.remove(node);
		}
	}
	
}
