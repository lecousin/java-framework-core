package net.lecousin.framework.collections.sort;

/**
 * This interface marks a list as being sorted.
 * It does not add any constraint except the it must be iterable.
 * Sub-interfaces may be used to force the way elements are added, removed...
 * @param <T> type of elements
 */
public interface Sorted<T> extends Iterable<T> {

	/** Number of elements in this collection. */
	public int size();
	
	/** Return true if this collection is empty. */
	public boolean isEmpty();
	
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
