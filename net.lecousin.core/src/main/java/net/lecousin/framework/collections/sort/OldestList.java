package net.lecousin.framework.collections.sort;

import java.util.Iterator;

import net.lecousin.framework.collections.ArrayIterator;

/**
 * Sorted list of elements, each being associated with a date (timestamp).
 * The list is sorted such as the oldest comes first.
 * The list has a maximum number of elements, given in the constructor: when already full,
 * if a new element comes with a timestamp more recent than the last element, it is ignored,
 * else it is inserted and the more recent element is removed, such as this list keeps the
 * oldest elements.
 * An example of usage can be a cache, where we want to quickly access to the oldest
 * items to free some space.
 * @param <T> type of elements
 */
public class OldestList<T> implements Iterable<T> {

	/** Initialize with the given capacity. */
	public OldestList(int nb) {
		elements = new Object[nb];
		dates = new long[nb];
		size = 0;
	}
	
	private Object[] elements;
	private long[] dates;
	private int size;
	private int oldestIndex = -1;
	private int newestIndex = -1;
	
	/** Add an elements with the given timestamp. */
	public void add(long date, T element) {
		if (size == 0) {
			elements[0] = element;
			dates[0] = date;
			size = 1;
			oldestIndex = 0;
			newestIndex = 0;
			return;
		}
		if (size < elements.length) {
			elements[size] = element;
			dates[size] = date;
			if (date < dates[oldestIndex]) oldestIndex = size;
			if (date > dates[newestIndex]) newestIndex = size;
			size++;
			return;
		}
		if (newestIndex >= 0 && dates[newestIndex] < date) return; // newer than newest
		if (oldestIndex >= 0 && dates[oldestIndex] > date) {
			// older than oldest
			if (newestIndex < 0) refreshIndexes();
			elements[newestIndex] = element;
			dates[newestIndex] = date;
			oldestIndex = newestIndex;
			newestIndex = -1;
			return;
		}
		if (newestIndex < 0 || oldestIndex < 0) refreshIndexes();
		if (dates[newestIndex] < date) return; // newer than newest
		// replace the newest
		elements[newestIndex] = element;
		dates[newestIndex] = date;
		oldestIndex = newestIndex;
		newestIndex = -1;
		return;
	}
	
	private void refreshIndexes() {
		for (int i = elements.length - 1; i >= 0; --i) {
			if (oldestIndex == -1 || dates[i] < dates[oldestIndex]) oldestIndex = i;
			if (newestIndex == -1 || dates[i] > dates[newestIndex]) newestIndex = i;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Iterator<T> iterator() {
		return new ArrayIterator<T>((T[])elements, size);
	}
	
}
