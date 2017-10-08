package net.lecousin.framework.collections;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.util.ObjectUtil;

/** Utility methods on collections. */
public final class CollectionsUtil {
	
	private CollectionsUtil() { /* no instance */ }

	/** Instantiate an IteratorToEnumeration on the given iterator. */
	public static <T> Enumeration<T> enumeration(Iterator<T> iterator) {
		return new IteratorToEnumeration<>(iterator);
	}
	
	/** Convert an iterator to an enumeration.
	 * @param <T> type of elements
	 */
	public static class IteratorToEnumeration<T> implements Enumeration<T> {
		/** Constructor. */
		public IteratorToEnumeration(Iterator<T> it) {
			this.it = it;
		}
		
		private Iterator<T> it;
		
		@Override
		public boolean hasMoreElements() { return it.hasNext(); }
		
		@Override
		public T nextElement() { return it.next(); }
	}
	
	/** Create an Iterable from the enumeration, by filling a LinkedList with the elements in the enumeration. */
	public static <T> Iterable<T> iterable(Enumeration<T> enumeration) {
		LinkedList<T> list = new LinkedList<>();
		while (enumeration.hasMoreElements()) list.add(enumeration.nextElement());
		return list;
	}
	
	/** Return true of the 2 lists are identical. To be identical, the 2 lists must have the same size,
	 * and each element must be equals and in the same order. To compare elements, the method
	 * {@link ObjectUtil#equalsOrNull(Object, Object)} is used.
	 */
	public static boolean equals(List<?> list1, List<?> list2) {
		if (list1.size() != list2.size()) return false;
		Iterator<?> it1 = list1.iterator();
		Iterator<?> it2 = list2.iterator();
		while (it1.hasNext())
			if (!ObjectUtil.equalsOrNull(it1.next(), it2.next()))
				return false;
		return true;
	}
	
	/** Add all elements of the given enumeration into the given collection. */
	public static <T> void addAll(Collection<T> col, Enumeration<T> e) {
		while (e.hasMoreElements())
			col.add(e.nextElement());
	}
	
}
