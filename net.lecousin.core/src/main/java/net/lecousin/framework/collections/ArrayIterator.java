package net.lecousin.framework.collections;

import java.util.NoSuchElementException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Iterate on an array. It does not support the remove method.
 * @param <T> type of elements
 */
public class ArrayIterator<T> implements java.util.Iterator<T> {
	
	/** Creates an iterator from an array.
	 * @param array the array
	 * @param max number of elements in the array
	 */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public ArrayIterator(T[] array, int max) {
		this.array = array;
		this.max = max;
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

}
