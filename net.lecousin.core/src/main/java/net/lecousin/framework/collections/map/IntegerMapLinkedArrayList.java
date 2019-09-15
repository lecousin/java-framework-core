package net.lecousin.framework.collections.map;

import java.util.Iterator;

import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.collections.LinkedIterators;

/** Implementation of an IntegerMap, with a specified number of buckets, each bucket being associated with a LinkedArrayList.
 * This implementation is not the fastest to find an element because the LinkedArrayList is not sorted.
 * For a faster implementation we can consider the IntegerMapRBT.
 * The advantage of this implementation is it can support many elements.
 *
 * @param <T> type of mapped object
 */
public class IntegerMapLinkedArrayList<T> implements IntegerMap<T> {

	/** Constructor. */
	@SuppressWarnings("unchecked")
	public IntegerMapLinkedArrayList(int nbBuckets, int arraySizeByBucket) {
		buckets = new LinkedArrayList[nbBuckets];
		for (int i = 0; i < nbBuckets; ++i)
			buckets[i] = new LinkedArrayList<>(arraySizeByBucket);
	}
	
	private static class Entry<T> {
		private Entry(int key, T value) {
			this.key = key;
			this.value = value;
		}
		
		private int key;
		private T value;
	}
	
	private LinkedArrayList<Entry<T>>[] buckets;
	private int size = 0;

	@Override
	public int size() { return size; }

	@Override
	public boolean isEmpty() { return size == 0; }

	@Override
	public void clear() {
		for (LinkedArrayList<Entry<T>> bucket : buckets)
			bucket.clear();
		size = 0;
	}

	@Override
	public T put(int key, T entry) {
		int b = key % buckets.length;
		if (b < 0) b = -b;
		for (Entry<T> e : buckets[b])
			if (e.key == key) {
				T previous = e.value;
				e.value = entry;
				return previous;
			}
		buckets[b].add(new Entry<T>(key, entry));
		size++;
		return null;
	}

	@Override
	public T get(int key) {
		int b = key % buckets.length;
		if (b < 0) b = -b;
		for (Entry<T> e : buckets[b])
			if (e.key == key)
				return e.value;
		return null;
	}

	@Override
	public T remove(int key) {
		int b = key % buckets.length;
		if (b < 0) b = -b;
		for (Iterator<Entry<T>> it = buckets[b].iterator(); it.hasNext(); ) {
			Entry<T> e = it.next();
			if (e.key == key) {
				it.remove();
				size--;
				return e.value;
			}
		}
		return null;
	}

	@Override
	public boolean containsKey(int key) {
		int b = key % buckets.length;
		if (b < 0) b = -b;
		for (Entry<T> e : buckets[b])
			if (e.key == key)
				return true;
		return false;
	}

	@Override
	public Iterator<T> values() {
		LinkedIterators<Entry<T>> it = new LinkedIterators<>();
		for (LinkedArrayList<Entry<T>> bucket : buckets)
			it.addIterator(bucket.iterator());
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}
			
			@Override
			public T next() {
				return it.next().value;
			}
			
			@Override
			public void remove() {
				it.remove();
			}
		};
	}
	
}
