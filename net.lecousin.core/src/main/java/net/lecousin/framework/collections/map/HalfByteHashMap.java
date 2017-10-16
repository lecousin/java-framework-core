package net.lecousin.framework.collections.map;

/**
 * This class uses the 4 lower bits of a byte as a hash code, then for each hash code
 * associates an ordered array.
 * The insert and remove operations are costly because we need to reallocate and order the arrays,
 * but the get operation is fast, while using reasonable amount of memory.
 * @param <T> type of elements
 */
public class HalfByteHashMap<T> implements ByteMap<T> {

	private static class HalfByteArray<T> {
		
		private byte[] bytes;
		private T[] elements;
		
	}
	
	private int size = 0;
	private HalfByteArray<T>[] hashmap;
	
	/** Constructor. */
	@SuppressWarnings("unchecked")
	public HalfByteHashMap() {
		hashmap = new HalfByteArray[16];
	}
	
	@Override
	public T get(byte key) {
		HalfByteArray<T> array = hashmap[key & 0xF];
		if (array == null) return null;
		// dichotomy to find the key
		int b = array.bytes.length;
		if (b == 1) {
			if (array.bytes[0] != key) return null;
			return array.elements[0];
		}
		int a = 0;
		do {
			int m = a + (b - a) / 2;
			if (key == array.bytes[m]) return array.elements[m];
			if (key < array.bytes[m]) {
				if (m == a) return null;
				b = m;
				continue;
			}
			if (m == b - 1) return null;
			a = m + 1;
		} while (true);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T put(byte key, T element) {
		HalfByteArray<T> array = hashmap[key & 0xF];
		if (array == null) {
			array = new HalfByteArray<T>();
			array.bytes = new byte[] { key };
			array.elements = (T[])new Object[] { element };
			hashmap[key & 0xF] = array;
			size++;
			return null;
		}
		// dichotomy to insert
		int b = array.bytes.length;
		if (b == 1) {
			byte[] nb;
			T[] ne;
			if (key < array.bytes[0]) {
				nb = new byte[] { key, array.bytes[0] };
				ne = (T[])new Object[] { element, array.elements[0] };
			} else if (key > array.bytes[0]) {
				nb = new byte[] { array.bytes[0], key };
				ne = (T[])new Object[] { array.elements[0], element };
			} else {
				T previous = array.elements[0];
				array.elements[0] = element;
				size++;
				return previous;
			}
			array.bytes = nb;
			array.elements = ne;
			size++;
			return null;
		}
		int a = 0;
		int index;
		do {
			int m = a + (b - a) / 2;
			if (key == array.bytes[m]) {
				T previous = array.elements[m];
				array.elements[m] = element;
				size++;
				return previous;
			}
			if (key < array.bytes[m]) {
				if (m == a) {
					index = a;
					break;
				}
				b = m;
				continue;
			}
			if (m == b - 1) {
				index = b;
				break;
			}
			a = m + 1;
		} while (true);
		byte[] nb = new byte[array.bytes.length + 1];
		T[] ne = (T[])new Object[array.bytes.length + 1];
		if (index > 0) {
			System.arraycopy(array.bytes, 0, nb, 0, index);
			System.arraycopy(array.elements, 0, ne, 0, index);
		}
		nb[index] = key;
		ne[index] = element;
		if (index < array.bytes.length) {
			System.arraycopy(array.bytes, index, nb, index + 1, array.bytes.length - index);
			System.arraycopy(array.elements, index, ne, index + 1, array.bytes.length - index);
		}
		array.bytes = nb;
		array.elements = ne;
		size++;
		return null;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public boolean containsKey(byte key) {
		HalfByteArray<T> array = hashmap[key & 0xF];
		if (array == null) return false;
		// dichotomy to find the key
		int b = array.bytes.length;
		if (b == 1) {
			if (array.bytes[0] != key) return false;
			return true;
		}
		int a = 0;
		do {
			int m = a + (b - a) / 2;
			if (key == array.bytes[m]) return true;
			if (key < array.bytes[m]) {
				if (m == a) return false;
				b = m;
				continue;
			}
			if (m == b - 1) return false;
			a = m + 1;
		} while (true);
	}
	
	// skip checkstyle: VariableDeclarationUsageDistance
	@Override
	public T remove(byte key) {
		HalfByteArray<T> array = hashmap[key & 0xF];
		if (array == null) return null;
		// dichotomy to find the key
		int b = array.bytes.length;
		if (b == 1) {
			if (array.bytes[0] != key) return null;
			T e = array.elements[0];
			hashmap[key & 0xF] = null;
			size--;
			return e;
		}
		int a = 0;
		int index = -1;
		do {
			int m = a + (b - a) / 2;
			if (key == array.bytes[m]) {
				index = m;
				break;
			}
			if (key < array.bytes[m]) {
				if (m == a) return null;
				b = m;
				continue;
			}
			if (m == b - 1) return null;
			a = m + 1;
		} while (true);
		T e = array.elements[index];
		byte[] nb = new byte[array.bytes.length - 1];
		@SuppressWarnings("unchecked")
		T[] ne = (T[])new Object[array.bytes.length - 1];
		// copy before index
		if (index > 0) {
			System.arraycopy(array.bytes, 0, nb, 0, index);
			System.arraycopy(array.elements, 0, ne, 0, index);
		}
		// copy after index
		if (index < nb.length) {
			System.arraycopy(array.bytes, index + 1, nb, index, nb.length - index);
			System.arraycopy(array.elements, index + 1, ne, index, ne.length - index);
		}
		array.bytes = nb;
		array.elements = ne;
		size--;
		return e;
	}
	
}
