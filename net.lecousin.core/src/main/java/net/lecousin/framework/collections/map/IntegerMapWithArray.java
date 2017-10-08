package net.lecousin.framework.collections.map;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of IntegerMap, using a 2-dimensions array:
 * the first dimension (buckets) is used for the hashing value, which is the integer value modulo the number of buckets,
 * the second dimension is an array storing elements corresponding to the hash value.
 * The array may grow if many elements have the same hash code.
 * @param <ValueType> type of values
 */
public class IntegerMapWithArray<ValueType> implements IntegerMap<ValueType>, Map<Integer,ValueType> {

	private static class Element<T> implements Map.Entry<Integer, T> {
		Element(int k, T e) {
			key = k;
			element = e;
		}
		
		int key;
		T element;
		
		@Override
		public Integer getKey() { return Integer.valueOf(key); }
		
		@Override
		public T getValue() { return element; }
		
		@Override
		public T setValue(T v) {
			T e = element;
			element = v;
			return e;
		}
	}

	/** Create a map with the specified number of buckets, and in each bucket an array of the given initial size. */
	@SuppressWarnings("unchecked")
	public IntegerMapWithArray(int nbBuckets, int initialBucketSize) {
		buckets = new Element[nbBuckets][initialBucketSize];
	}
	
	/** Equivalent to IntegerMapWithArray(20, 10). */
	public IntegerMapWithArray() {
		this(20,10);
	}

	private Element<ValueType>[][] buckets;
	private int size = 0;
	
	@Override
	public ValueType put(int key, ValueType entry) {
		size++;
		int hc = key % buckets.length;
		Element<ValueType>[] bucket = buckets[hc];
		int index = -1;
		for (int i = bucket.length - 1; i >= 0; --i)
			if (bucket[i] == null) {
				if (index == -1) index = i;
			} else if (bucket[i].equals(entry)) {
				index = i;
				break;
			}
		if (index != -1) {
			ValueType prev = bucket[index] == null ? null : bucket[index].element;
			bucket[index] = new Element<>(key, entry);
			return prev;
		}
		int l = bucket.length;
		buckets[hc] = bucket = Arrays.copyOf(bucket, l * 2);
		bucket[l] = new Element<>(key, entry);
		return null;
	}
	
	@Override
	public ValueType put(Integer key, ValueType value) { return put(key.intValue(), value); }

	@Override
	public ValueType get(int key) {
		int hc = key % buckets.length;
		Element<ValueType>[] bucket = buckets[hc];
		for (int i = bucket.length - 1; i >= 0; --i)
			if (bucket[i] != null && bucket[i].key == key)
				return bucket[i].element;
		return null;
	}
	
	@Override
	public ValueType get(Object key) { return get(((Integer)key).intValue()); }
	
	@Override
	@SuppressWarnings("unchecked")
	public void clear() {
		for (int i = 0; i < buckets.length; ++i)
			buckets[i] = new Element[5];
		size = 0;
	}

	@Override
	public ValueType remove(int key) {
		int hc = key % buckets.length;
		Element<ValueType>[] bucket = buckets[hc];
		for (int i = bucket.length - 1; i >= 0; --i)
			if (bucket[i] != null && bucket[i].key == key) {
				ValueType e = bucket[i].element;
				bucket[i] = null;
				return e;
			}
		return null;
	}
	
	@Override
	public ValueType remove(Object key) { return remove(((Integer)key).intValue()); }
	
	@Override
	public boolean containsKey(int key) {
		int hc = key % buckets.length;
		Element<ValueType>[] bucket = buckets[hc];
		for (int i = bucket.length - 1; i >= 0; --i)
			if (bucket[i] != null && bucket[i].key == key)
				return true;
		return false;
	}

	@Override
	public boolean containsKey(Object key) { return containsKey(((Integer)key).intValue()); }
	
	@Override
	public boolean containsValue(Object value) {
		for (int i = buckets.length - 1; i >= 0; --i) {
			Element<ValueType>[] b = buckets[i];
			for (int j = b.length - 1; j >= 0; --j) {
				Element<ValueType> e = b[j];
				if (e != null && e.element.equals(value)) return true;
			}
		}
		return false;
	}
	
	
	@Override
	public boolean isEmpty() { return size == 0; }
	
	@Override
	public int size() { return size; }
	
	@Override
	public void putAll(Map<? extends Integer, ? extends ValueType> m) {
		for (Map.Entry<? extends Integer, ? extends ValueType> e : m.entrySet()) put(e.getKey(), e.getValue());
	}
	
	@Override
	public Set<Map.Entry<Integer, ValueType>> entrySet() {
		return new EntrySet();
	}
	
	@Override
	public Set<Integer> keySet() {
		return new KeySet();
	}
	
	@Override
	public Collection<ValueType> values() {
		return new Values();
	}
	
	private class EntrySet implements Set<Map.Entry<Integer, ValueType>> {
		@Override
		public boolean add(java.util.Map.Entry<Integer, ValueType> e) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean addAll(Collection<? extends java.util.Map.Entry<Integer, ValueType>> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public void clear() { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean contains(Object o) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean containsAll(java.util.Collection<?> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean isEmpty() { return size != 0; }
		
		@Override
		public boolean remove(Object o) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public int size() { return size; }
		
		@Override
		public Iterator<java.util.Map.Entry<Integer, ValueType>> iterator() { return new EntryIterator(); }
		
		@Override
		public Object[] toArray() { throw new UnsupportedOperationException(); }
		
		@Override
		public <T extends Object> T[] toArray(T[] a) { throw new UnsupportedOperationException(); }
	}
	
	private class KeySet implements Set<Integer> {
		@Override
		public boolean add(Integer e) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean addAll(Collection<? extends Integer> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public void clear() { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean contains(Object o) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean containsAll(java.util.Collection<?> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean isEmpty() { return size != 0; }
		
		@Override
		public boolean remove(Object o) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public int size() { return size; }
		
		@Override
		public Iterator<Integer> iterator() { return new KeyIterator(); }
		
		@Override
		public Object[] toArray() { throw new UnsupportedOperationException(); }
		
		@Override
		public <T extends Object> T[] toArray(T[] a) { throw new UnsupportedOperationException(); }
	}
	
	private class Values implements Collection<ValueType> {
		@Override
		public boolean add(ValueType e) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean addAll(Collection<? extends ValueType> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public void clear() { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean contains(Object o) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean containsAll(java.util.Collection<?> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean isEmpty() { return size != 0; }
		
		@Override
		public java.util.Iterator<ValueType> iterator() { return new ValueIterator(); }
		
		@Override
		public boolean remove(Object o) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean removeAll(java.util.Collection<?> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean retainAll(java.util.Collection<?> c) { throw new UnsupportedOperationException(); }
		
		@Override
		public int size() { return size; }
		
		@Override
		public Object[] toArray() { throw new UnsupportedOperationException(); }
		
		@Override
		public <T extends Object> T[] toArray(T[] a) { throw new UnsupportedOperationException(); }
	}
	
	private class KeyIterator implements Iterator<Integer> {
		int bucketIndex = 0;
		int bucketPos = 0;
		
		KeyIterator() {
			go_next();
		}
		
		private void go_next() {
			if (bucketIndex == -1) return;
			do {
				Element<ValueType>[] b = buckets[bucketIndex];
				while (bucketPos < b.length && b[bucketPos] == null) bucketPos++;
				if (bucketPos >= b.length) {
					bucketIndex++;
					if (bucketIndex >= buckets.length) {
						bucketIndex = -1;
						return;
					}
				} else break;
			} while (true);
		}
		
		@Override
		public boolean hasNext() { return bucketIndex != -1; }
		
		@Override
		public Integer next() {
			if (bucketIndex == -1) return null;
			Element<ValueType> e = buckets[bucketIndex][bucketPos];
			go_next();
			return e.getKey();
		}
		
		@Override
		public void remove() { throw new UnsupportedOperationException(); }
	}

	private class ValueIterator implements Iterator<ValueType> {
		int bucketIndex = 0;
		int bucketPos = 0;
		
		ValueIterator() {
			go_next();
		}
		
		private void go_next() {
			if (bucketIndex == -1) return;
			do {
				Element<ValueType>[] b = buckets[bucketIndex];
				while (bucketPos < b.length && b[bucketPos] == null) bucketPos++;
				if (bucketPos >= b.length) {
					bucketIndex++;
					if (bucketIndex >= buckets.length) {
						bucketIndex = -1;
						return;
					}
				} else break;
			} while (true);
		}
		
		@Override
		public boolean hasNext() { return bucketIndex != -1; }
		
		@Override
		public ValueType next() {
			if (bucketIndex == -1) return null;
			Element<ValueType> e = buckets[bucketIndex][bucketPos];
			go_next();
			return e.element;
		}
		
		@Override
		public void remove() { throw new UnsupportedOperationException(); }
	}

	private class EntryIterator implements Iterator<Map.Entry<Integer, ValueType>> {
		int bucketIndex = 0;
		int bucketPos = 0;
		
		EntryIterator() {
			go_next();
		}
		
		private void go_next() {
			if (bucketIndex == -1) return;
			do {
				Element<ValueType>[] b = buckets[bucketIndex];
				while (bucketPos < b.length && b[bucketPos] == null) bucketPos++;
				if (bucketPos >= b.length) {
					bucketIndex++;
					if (bucketIndex >= buckets.length) {
						bucketIndex = -1;
						return;
					}
				} else break;
			} while (true);
		}
		
		@Override
		public boolean hasNext() { return bucketIndex != -1; }
		
		@Override
		public Map.Entry<Integer,ValueType> next() {
			if (bucketIndex == -1) return null;
			Element<ValueType> e = buckets[bucketIndex][bucketPos];
			go_next();
			return e;
		}
		
		@Override
		public void remove() { throw new UnsupportedOperationException(); }
	}
}
