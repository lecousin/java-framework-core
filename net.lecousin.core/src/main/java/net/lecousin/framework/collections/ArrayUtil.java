package net.lecousin.framework.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.util.ObjectUtil;

/** Utility methods for arrays. */
public final class ArrayUtil {
	
	private ArrayUtil() { /* no instance */ }

	/** Return true if the array contains the value.
	 * Comparison is done using the equals method.
	 * The array must not contain null values.
	 */
	public static <T> boolean contains(T[] array, T value) {
		if (array == null) return false;
		for (int i = 0; i < array.length; ++i)
			if (array[i].equals(value))
				return true;
		return false;
	}
	
	/** Return true if the array contains the value. */
	public static boolean contains(char[] array, char value) {
		if (array == null) return false;
		for (int i = 0; i < array.length; ++i)
			if (array[i] == value)
				return true;
		return false;
	}
	
	/** Return true if the array contains the value. */
	public static boolean contains(byte[] array, byte value) {
		if (array == null) return false;
		for (int i = 0; i < array.length; ++i)
			if (array[i] == value)
				return true;
		return false;
	}
	
	/** Return true if the array contains the value. */
	public static boolean contains(short[] array, short value) {
		if (array == null) return false;
		for (int i = 0; i < array.length; ++i)
			if (array[i] == value)
				return true;
		return false;
	}
	
	/** Return true if the array contains the value. */
	public static boolean contains(int[] array, int value) {
		if (array == null) return false;
		for (int i = 0; i < array.length; ++i)
			if (array[i] == value)
				return true;
		return false;
	}
	
	/** Return true if the array contains the value. */
	public static boolean contains(long[] array, long value) {
		if (array == null) return false;
		for (int i = 0; i < array.length; ++i)
			if (array[i] == value)
				return true;
		return false;
	}
	
	/** Return the index of the element in the array, or -1 if not found.
	 * Comparison is done using the equals method.
	 * The array must not contain null values.
	 */
	public static <T> int indexOf(T element, T[] array) {
		for (int i = array.length - 1; i >= 0; --i)
			if (array[i].equals(element))
				return i;
		return -1;
	}
	
	/** Return the index of the element in the array, or -1 if not found. */
	public static int indexOf(byte element, byte[] array) {
		for (int i = array.length - 1; i >= 0; --i)
			if (array[i] == element)
				return i;
		return -1;
	}
	
	/** Return the index of the element in the array, or -1 if not found. */
	public static int indexOf(short element, short[] array) {
		for (int i = array.length - 1; i >= 0; --i)
			if (array[i] == element)
				return i;
		return -1;
	}
	
	/** Return the index of the element in the array, or -1 if not found. */
	public static int indexOf(int element, int[] array) {
		for (int i = array.length - 1; i >= 0; --i)
			if (array[i] == element)
				return i;
		return -1;
	}
	
	/** Return the index of the element in the array, or -1 if not found. */
	public static int indexOf(long element, long[] array) {
		for (int i = array.length - 1; i >= 0; --i)
			if (array[i] == element)
				return i;
		return -1;
	}
	
	/** Return the index of the element in the array, or -1 if not found. */
	public static int indexOf(char element, char[] array) {
		for (int i = array.length - 1; i >= 0; --i)
			if (array[i] == element)
				return i;
		return -1;
	}
	
	/** Return the index of the element in the array, or -1 if not found.
	 * Comparison is done using the == operator.
	 * The array may contain null values.
	 */
	public static <T> int indexOfInstance(T element, T[] array) {
		for (int i = array.length - 1; i >= 0; --i)
			if (array[i] == element)
				return i;
		return -1;
	}
	
	/** Return true if the 2 arrays are identical. The are identical if both are null or<ul>
	 * <li>They have the same length</li>
	 * <li>The elements are the same (using equals method) in the same order</li>
	 * </ul>
	 */
	public static <T> boolean equals(T[] a1, T[] a2) {
		if (a1 == null) return a2 == null;
		if (a2 == null) return false;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; ++i)
			if (!a1[i].equals(a2[i])) return false;
		return true;
	}
	
	/** Return true if the portions of the arrays are equal. */
	public static <T> boolean equals(T[] a1, int off1, T[] a2, int off2, int len) {
		for (int i = 0; i < len; ++i)
			if (!ObjectUtil.equalsOrNull(a1[off1 + i], a2[off2 + i]))
				return false;
		return true;
	}
	
	/** Return true if the 2 arrays are identical. The are identical if both are null or<ul>
	 * <li>They have the same length</li>
	 * <li>The values are the same in the same order</li>
	 * </ul>
	 */
	public static boolean equals(byte[] a1, byte[] a2) {
		if (a1 == null) return a2 == null;
		if (a2 == null) return false;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; ++i)
			if (a1[i] != a2[i]) return false;
		return true;
	}
	
	/** Return true if the 2 specified parts of the arrays are identical.
	 * The are identical if both array are null or if the values are the same and in the same order
	 * starting at their respective offset.
	 */
	public static boolean equals(byte[] a1, int off1, byte[] a2, int off2, int len) {
		if (a1 == null) return a2 == null;
		if (a2 == null) return false;
		for (int i = 0; i < len; ++i)
			if (a1[i + off1] != a2[i + off2]) return false;
		return true;
	}

	/** Return true if the 2 arrays are identical. The are identical if both are null or<ul>
	 * <li>They have the same length</li>
	 * <li>The values are the same in the same order</li>
	 * </ul>
	 */
	public static boolean equals(short[] a1, short[] a2) {
		if (a1 == null) return a2 == null;
		if (a2 == null) return false;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; ++i)
			if (a1[i] != a2[i]) return false;
		return true;
	}
	
	/** Return true if the 2 specified parts of the arrays are identical.
	 * The are identical if both array are null or if the values are the same and in the same order
	 * starting at their respective offset.
	 */
	public static boolean equals(short[] a1, int off1, short[] a2, int off2, int len) {
		if (a1 == null) return a2 == null;
		if (a2 == null) return false;
		for (int i = 0; i < len; ++i)
			if (a1[i + off1] != a2[i + off2]) return false;
		return true;
	}
	
	/** Return true if the 2 arrays are identical. The are identical if both are null or<ul>
	 * <li>They have the same length</li>
	 * <li>The values are the same in the same order</li>
	 * </ul>
	 */
	public static boolean equals(int[] a1, int[] a2) {
		if (a1 == null) return a2 == null;
		if (a2 == null) return false;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; ++i)
			if (a1[i] != a2[i]) return false;
		return true;
	}
	
	/** Return true if the 2 specified parts of the arrays are identical.
	 * The are identical if both array are null or if the values are the same and in the same order
	 * starting at their respective offset.
	 */
	public static boolean equals(int[] a1, int off1, int[] a2, int off2, int len) {
		if (a1 == null) return a2 == null;
		if (a2 == null) return false;
		for (int i = 0; i < len; ++i)
			if (a1[i + off1] != a2[i + off2]) return false;
		return true;
	}
	
	/** Return true if the 2 arrays are identical. The are identical if both are null or<ul>
	 * <li>They have the same length</li>
	 * <li>The values are the same in the same order</li>
	 * </ul>
	 */
	public static boolean equals(long[] a1, long[] a2) {
		if (a1 == null) return a2 == null;
		if (a2 == null) return false;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; ++i)
			if (a1[i] != a2[i]) return false;
		return true;
	}
	
	/** Return true if the 2 specified parts of the arrays are identical.
	 * The are identical if both array are null or if the values are the same and in the same order
	 * starting at their respective offset.
	 */
	public static boolean equals(long[] a1, int off1, long[] a2, int off2, int len) {
		if (a1 == null) return a2 == null;
		if (a2 == null) return false;
		for (int i = 0; i < len; ++i)
			if (a1[i + off1] != a2[i + off2]) return false;
		return true;
	}
	
	/** Return true if the 2 arrays are identical. The are identical if both are null or<ul>
	 * <li>They have the same length</li>
	 * <li>The values are the same in the same order</li>
	 * </ul>
	 */
	public static boolean equals(char[] a1, char[] a2) {
		if (a1 == null) return a2 == null;
		if (a2 == null) return false;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; ++i)
			if (a1[i] != a2[i]) return false;
		return true;
	}
	
	/** Return true if the 2 specified parts of the arrays are identical.
	 * The are identical if both array are null or if the values are the same and in the same order
	 * starting at their respective offset.
	 */
	public static boolean equals(char[] a1, int off1, char[] a2, int off2, int len) {
		if (a1 == null) return a2 == null;
		if (a2 == null) return false;
		for (int i = 0; i < len; ++i)
			if (a1[i + off1] != a2[i + off2]) return false;
		return true;
	}
	
	/** Creates an array of the given type and size. */
	@SuppressWarnings("unchecked")
	public static <T> T[] createGenericArray(Class<T[]> clazz, int size) {
		return (T[])java.lang.reflect.Array.newInstance(clazz.getComponentType(), size);
	}

	/** Creates an array of the given type, and put the given elements inside. */
	@SuppressWarnings("unchecked")
	public static <T> T[] createGenericArray(Class<T[]> clazz, Object[] content) {
		T[] array = createGenericArray(clazz, content.length);
		for (int i = 0; i < content.length; ++i)
			array[i] = (T)content[i];
		return array;
	}

	/** Creates an array of the given type and size. */
	@SuppressWarnings("unchecked")
	public static <T> T[] createGenericArrayOf(int size, Class<T> type) {
		return (T[])java.lang.reflect.Array.newInstance(type, size);
	}
	
	/** Creates a List containing instances of elements present in both arrays. */
	public static <T> List<T> intersectionIdentity(T[] a1, T[] a2) {
		List<T> result = new LinkedList<T>();
		for (T e1 : a1)
			for (T e2 : a2)
				if (e1 == e2) {
					result.add(e1);
					break;
				}
		return result;
	}

	/** Create a new array concatenating first array with second array. */
	@SuppressWarnings("unchecked")
	public static <T> T[] concatenate(T[] a, T[] b) {
		if (a.length == 0) return b;
		if (b.length == 0) return a;
		T[] array = createGenericArray((Class<T[]>)a.getClass().getComponentType(), a.length + b.length);
		for (int i = 0; i < a.length; ++i)
			array[i] = a[i];
		for (int i = 0; i < b.length; ++i)
			array[i + a.length] = b[i];
		return array;
	}
	
	/** Create a single byte array from the given arrays. */
	public static byte[] merge(List<byte[]> arrays) {
		if (arrays.size() == 0) return new byte[0];
		if (arrays.size() == 1) return arrays.get(0);
		int size = 0;
		for (byte[] b : arrays) size += b.length;
		byte[] a = new byte[size];
		size = 0;
		for (byte[] b : arrays) {
			System.arraycopy(b, 0, a, size, b.length);
			size += b.length;
		}
		return a;
	}
	
	/** Compare 2 byte arrays.
	 * Comparison is done byte by byte. On the first different byte,
	 * if the value of the byte on the first array is lower than the second array
	 * -1 is returned, else 1 is returned.
	 * If the first array contains less elements than the second, and the bytes
	 * are the same, -1 is returned.
	 * If the first array contains more elements than the second, and the bytes
	 * are the same, 1 is returned.
	 */
	public static int compare(byte[] b1, byte[] b2) {
		if (b1.length < b2.length) {
			int c = compare(b1, 0, b2, 0, b1.length);
			if (c != 0) return c;
			return -1;
		}
		if (b1.length == b2.length)
			return compare(b1, 0, b2, 0, b1.length);
		int c = compare(b1, 0, b2, 0, b2.length);
		if (c != 0) return c;
		return 1;
	}
	
	/** Compare 2 byte arrays. If both contain the same values,
	 * starting at their respective offset, 0 is returned.
	 * Else the first different byte is compared:
	 * if the value of the byte on the first array is lower than the second array
	 * -1 is returned, else 1 is returned.
	 */
	public static int compare(byte[] b1, int off1, byte[] b2, int off2, int len) {
		for (int i = 0; i < len; ++i) {
			if (b1[i + off1] < b2[i + off2]) return -1;
			if (b1[i + off1] > b2[i + off2]) return 1;
		}
		return 0;
	}

	/** Create a new array with one more element appended at the end. */
	public static <T> T[] add(T[] a, T elem) {
		T[] na = Arrays.copyOf(a, a.length + 1);
		na[a.length] = elem;
		return na; 
	}
	
	/** Create a new array with one more element inserted at the given position. */
	@SuppressWarnings("unchecked")
	public static <T> T[] add(T[] a, T elem, int pos) {
		T[] na = createGenericArray((Class<T[]>)a.getClass(), a.length + 1);
		if (pos > 0) System.arraycopy(a, 0, na, 0, pos);
		na[pos] = elem;
		if (pos < a.length) System.arraycopy(a, pos, na, pos + 1, a.length - pos);
		return na; 
	}
	
	/** Create a new array with new elements appended at the end. */
	public static <T> T[] add(T[] a, Collection<T> toAdd) {
		T[] na = Arrays.copyOf(a, a.length + toAdd.size());
		int i = a.length;
		for (T elem : toAdd) na[i++] = elem;
		return na; 
	}
	
	/** Remove an element from an array. If the element is not found, the original array is returned.
	 * If the element is found, a new array is created with the first occurence of the element removed from it.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] remove(T[] a, T elem) {
		if (a.length == 0) return a;
		T[] na = createGenericArray((Class<T[]>)a.getClass(), a.length - 1);
		int found = 0;
		for (int i = 0; i < a.length; ++i) {
			T e = a[i];
			if (found == 0 && e.equals(elem)) {
				found = 1;
				continue;
			}
			na[i - found] = e;
		}
		if (found == 0) return a;
		return na;
	}

	/** Create a new array with the element at the given position removed from it. */
	public static <T> T[] removeAt(T[] a, int i) {
		@SuppressWarnings("unchecked")
		T[] na = createGenericArray((Class<T[]>)a.getClass(), a.length - 1);
		if (i > 0)
			System.arraycopy(a, 0, na, 0, i);
		if (i < na.length)
			System.arraycopy(a, i + 1, na, i, na.length - i);
		return na;
	}

	/** Instantiate an ArrayIterator on the given array. */
	public static <T> Iterator<T> iterator(T[] a) {
		return new ArrayIterator<T>(a, a.length);
	}
	
	/** Search a sequence of bytes in a byte array. */
	public static int search(byte[] toFind, byte[] buf, int start, int len) {
		if (len < toFind.length) return -1;
		for (int i = 0; i <= len - toFind.length; ++i) {
			if (buf[start + i] != toFind[0]) continue;
			boolean ok = true;
			for (int j = 1; j < toFind.length; ++j)
				if (buf[start + i + j] != toFind[j]) {
					ok = false;
					break;
				}
			if (ok) return i;
		}
		return -1;
	}
	
	/** Create a copy of the array. */
	public static byte[] copy(byte[] a) {
		byte[] c = new byte[a.length];
		System.arraycopy(a, 0, c, 0, a.length);
		return c;
	}
	
	/** Create an array from the collection. */
	public static int[] toArray(Collection<Integer> col) {
		int[] a = new int[col.size()];
		int i = 0;
		for (Integer val : col) a[i++] = val.intValue();
		return a;
	}

	/** Create an ArrayList from the array. */
	public static <T> ArrayList<T> newArrayList(T[] items) {
		ArrayList<T> list = new ArrayList<>(items.length);
		for (int i = 0; i < items.length; ++i)
			list.add(items[i]);
		return list;
	}
}
