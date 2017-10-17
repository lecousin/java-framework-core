package net.lecousin.framework.collections.map;

import net.lecousin.framework.collections.sort.RedBlackTreeLong;

/**
 * Implements a LongMap, with a fixed number of buckets, each bucket containing a Red-Black tree to order elements and find them quickly.<br/>
 * The hash method uses by default a modulus operation, but may be overridden if needed.
 * @param <T> type of values
 */
public class LongMapRBT<T> implements LongMap<T> {

	/** Constructor. */
	@SuppressWarnings("unchecked")
	public LongMapRBT(int nbBuckets) {
		buckets = new RedBlackTreeLong[nbBuckets];
	}
	
	private RedBlackTreeLong<T>[] buckets;
	private int size = 0;
	
	protected int hash(long key) {
		int h = (int)(key % buckets.length);
		if (h < 0) return -h;
		return h;
	}
	
	@Override
	public T put(long key, T entry) {
		int h = hash(key);
		RedBlackTreeLong<T> bucket = buckets[h];
		if (bucket == null) {
			buckets[h] = bucket = new RedBlackTreeLong<T>();
			bucket.add(key, entry);
			size++;
			return null;
		}
		RedBlackTreeLong.Node<T> node = bucket.get(key);
		if (node == null) {
			bucket.add(key, entry);
			size++;
			return null;
		}
		T previous = node.getElement();
		node.setElement(entry);
		return previous;
	}

	@Override
	public T get(long key) {
		int h = hash(key);
		RedBlackTreeLong<T> bucket = buckets[h];
		if (bucket == null)
			return null;
		RedBlackTreeLong.Node<T> node = bucket.get(key);
		if (node == null)
			return null;
		return node.getElement();
	}

	@Override
	public T remove(long key) {
		int h = hash(key);
		RedBlackTreeLong<T> bucket = buckets[h];
		if (bucket == null)
			return null;
		RedBlackTreeLong.Node<T> node = bucket.get(key);
		if (node == null)
			return null;
		bucket.remove(node);
		size--;
		return node.getElement();
	}

	@Override
	public boolean containsKey(long key) {
		int h = hash(key);
		RedBlackTreeLong<T> bucket = buckets[h];
		if (bucket == null)
			return false;
		return bucket.containsKey(key);
	}

	@Override
	public int size() {
		return size;
	}

}
