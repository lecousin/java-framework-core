package net.lecousin.framework.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Allows to add arrays, then iterate on them like this is a single list.
 * @param <T> type of elements
 */
public class ListOfArrays<T> implements Iterable<T> {

	/** Constructor. */
	public ListOfArrays() {
		// nothing to do
	}
	
	private ArrayList<T[]> arrays = new ArrayList<>();
	
	public void add(T[] array) { arrays.add(array); }
	
	public List<T[]> getArrays() { return arrays; }
	
	@Override
	public Iterator<T> iterator() {
		return new It();
	}
	
	private class It implements Iterator<T> {
		private It() {
			pos1 = 0;
			if (!arrays.isEmpty())
				do {
					array = arrays.get(pos1);
					if (array.length > 0) {
						pos2 = 0;
						break;
					}
					pos1++;
				} while (pos1 < arrays.size());
		}
		
		private int pos1;
		private int pos2;
		private T[] array;
		
		@Override
		public boolean hasNext() {
			return pos1 < arrays.size();
		}
		
		@Override
		public T next() {
			T e;
			try { e = array[pos2++]; }
			catch (ArrayIndexOutOfBoundsException err) {
				throw new NoSuchElementException();
			}
			if (pos2 == array.length) {
				pos2 = 0;
				pos1++;
				while (pos1 < arrays.size()) {
					array = arrays.get(pos1);
					if (array.length > 0) break;
					pos1++;
				}
			}
			return e;
		}
	}
	
}
