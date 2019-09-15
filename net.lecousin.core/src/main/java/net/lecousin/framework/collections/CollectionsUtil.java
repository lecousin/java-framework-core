package net.lecousin.framework.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

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
	@SuppressWarnings("squid:S1150") // we want an Enumeration in this case
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
	
	/** Create an Iterator from an Enumeration. */
	public static <T> Iterator<T> iterator(Enumeration<T> enumeration) {
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return enumeration.hasMoreElements();
			}
			
			@Override
			@SuppressWarnings("squid:S2272") // false positive: nextElement already throws NoSuchElementException
			public T next() {
				return enumeration.nextElement();
			}
		};
	}
	
	/** Create an Iterable from an Enumeration, but avoiding to create a list, so the Iterable can be iterated only once. */
	public static <T> Iterable<T> singleTimeIterable(Enumeration<T> enumeration) {
		return () -> CollectionsUtil.iterator(enumeration);
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
	
	/** Convert a list of Input into a list of Output using the given mapper. */
	public static <Input, Output> List<Output> map(List<Input> inputs, Function<Input, Output> mapper) {
		ArrayList<Output> outputs = new ArrayList<>(inputs.size());
		for (Input input : inputs)
			outputs.add(mapper.apply(input));
		return outputs;
	}
	
	/** Search an element and return it, ot null if not found. */
	public static <T> T findElement(Collection<T> collection, Predicate<T> predicate) {
		for (T element : collection)
			if (predicate.test(element))
				return element;
		return null;
	}
	
	/** Return a function that search an element and return it, ot null if not found. */
	public static <T> Function<Collection<T>, T> filterSingle(Predicate<T> predicate) {
		return collection -> {
			for (T element : collection)
				if (predicate.test(element))
					return element;
			return null;
		};
	}

}
