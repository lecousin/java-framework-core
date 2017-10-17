package net.lecousin.framework.collections.map;

import net.lecousin.framework.collections.sort.RedBlackTreeInteger;

/**
 * Implements an IntegerMap, with a fixed number of buckets, each bucket containing a Red-Black tree to order elements and find them quickly.<br/>
 * The hash method uses by default a modulus operation, but may be overridden if needed.
 * @param <T> type of values
 */
public class IntegerMapRBT<T> implements IntegerMap<T> {

	/** Constructor. */
	@SuppressWarnings("unchecked")
	public IntegerMapRBT(int nbBuckets) {
		buckets = new RedBlackTreeInteger[nbBuckets];
	}
	
	private RedBlackTreeInteger<T>[] buckets;
	private int size = 0;
	
	protected int hash(int key) {
		int h = key % buckets.length;
		if (h < 0) return -h;
		return h;
	}
	
	@Override
	public T put(int key, T entry) {
		int h = hash(key);
		RedBlackTreeInteger<T> bucket = buckets[h];
		if (bucket == null) {
			buckets[h] = bucket = new RedBlackTreeInteger<T>();
			bucket.add(key, entry);
			size++;
			return null;
		}
		RedBlackTreeInteger.Node<T> node = bucket.get(key);
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
	public T get(int key) {
		int h = hash(key);
		RedBlackTreeInteger<T> bucket = buckets[h];
		if (bucket == null)
			return null;
		RedBlackTreeInteger.Node<T> node = bucket.get(key);
		if (node == null)
			return null;
		return node.getElement();
	}

	@Override
	public T remove(int key) {
		int h = hash(key);
		RedBlackTreeInteger<T> bucket = buckets[h];
		if (bucket == null)
			return null;
		RedBlackTreeInteger.Node<T> node = bucket.get(key);
		if (node == null)
			return null;
		bucket.remove(node);
		size--;
		return node.getElement();
	}

	@Override
	public boolean containsKey(int key) {
		int h = hash(key);
		RedBlackTreeInteger<T> bucket = buckets[h];
		if (bucket == null)
			return false;
		return bucket.containsKey(key);
	}

	@Override
	public int size() {
		return size;
	}

}
