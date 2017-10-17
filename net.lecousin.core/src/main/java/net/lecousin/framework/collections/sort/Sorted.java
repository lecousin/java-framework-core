package net.lecousin.framework.collections.sort;

import java.util.Iterator;

/**
 * This interface marks a list as being sorted.<br/>
 * It does not add any constraint except the it must be iterable.<br/>
 * The iterator returned by the method iterator() does not return elements in the sorted order, the method orderedIterator must be used instead.
 * Sub-interfaces may be used to force the way elements are added, removed...<br/>
 * @param <T> type of elements
 */
public interface Sorted<T> extends Iterable<T> {

	/** Number of elements in this collection. */
	public int size();
	
	/** Return true if this collection is empty. */
	public boolean isEmpty();
	
	/** Return an iterator that iterates on the correct order. */
	public Iterator<T> orderedIterator();
	
	/** Interface for sorted list where elements are associated with long values to sort them.
	 * @param <T> type of elements
	 */
	public interface AssociatedWithLong<T> extends Sorted<T> {
		
		/** Add an element. */
		public void add(long value, T element);

		/** Return true if the list contains the element. */
		public boolean contains(long value, T element);
		
		/** Return true if the list contains the given instance. */
		public boolean containsInstance(long value, T element);
		
		/** Remove the first occurrence of the element. */
		public void remove(long value, T element);

		/** Remove the first occurrence of the given instance. */
		public void removeInstance(long value, T element);
		
	}
	
	/** Interface for sorted list where elements are associated with integer values to sort them.
	 * @param <T> type of elements
	 */
	public interface AssociatedWithInteger<T> extends Sorted<T> {
		
		/** Add an element. */
		public void add(int value, T element);
		
		/** Return true if the list contains the element. */
		public boolean contains(int value, T element);

		/** Return true if the list contains the given instance. */
		public boolean containsInstance(int value, T element);
		
		/** Remove the first occurrence of the element. */
		public void remove(int value, T element);

		/** Remove the first occurrence of the given instance. */
		public void removeInstance(int value, T element);
		
	}
	
}
