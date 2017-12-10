package net.lecousin.framework.collections;

import java.lang.reflect.Array;
import java.util.NoSuchElementException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Iterate on an array. It does not support the remove method.
 * @param <T> type of elements
 */
public class ArrayIterator<T> implements java.util.Iterator<T> {
	
	/** Creates an iterator from an array.
	 * @param array the array
	 * @param max number of elements in the array to iterate on
	 */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public ArrayIterator(T[] array, int max) {
		this.array = array;
		this.max = max;
	}

	/** Constructor. */
	public ArrayIterator(T[] array) {
		this.array = array;
		this.max = array.length;
	}

	private T[] array;
	private int max;
	private int cursor = 0;

	@Override
	public boolean hasNext() { return max > cursor; }
	
	@Override
	public T next() {
		if (cursor >= max) throw new NoSuchElementException();
		return array[cursor++];
	}
	
	/**
	 * Iterate on an array of any type by using the reflection methods (java.lang.reflect.Array).
	 */
	@SuppressWarnings("rawtypes")
	public static class Generic implements java.util.Iterator {

		/** Constructor. */
		public Generic(Object array) {
			this.array = array;
		}
		
		private Object array;
		private int pos = 0;
		
		@Override
		public boolean hasNext() {
			if (array == null) return false;
			if (pos >= Array.getLength(array)) return false;
			return true;
		}
		
		@Override
		public Object next() {
			return Array.get(array, pos++);
		}
		
	}

}
