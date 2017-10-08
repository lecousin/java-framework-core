package net.lecousin.framework.collections;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Allows to add elements, iterables and enumerations, then iterate on them like this is a single list.
 * Note that when adding an enumeration, the enumeration is converted into an Iterable so it may not be 
 * efficient if only one iteration is supposed to be done.
 * @param <T> type of elements
 */
public class CompoundCollection<T> implements Iterable<T> {

	private List<Iterable<T>> list = new LinkedList<>();
	
	public void add(Iterable<T> collection) { list.add(collection); }
	
	public void add(Enumeration<T> e) { list.add(CollectionsUtil.iterable(e)); }
	
	public void addSingleton(T element) { list.add(Collections.singletonList(element)); }
	
	public Enumeration<T> enumeration() { return CollectionsUtil.enumeration(new It()); }
	
	@Override
	public Iterator<T> iterator() {
		return new It();
	}
	
	private class It implements Iterator<T> {
		private It() {
			it = list.iterator();
			while (it.hasNext()) {
				it2 = it.next().iterator();
				if (it2.hasNext()) break;
			}
		}
		
		private Iterator<Iterable<T>> it;
		private Iterator<T> it2;
		
		@Override
		public boolean hasNext() {
			return it2 != null && it2.hasNext();
		}
		
		@Override
		public T next() {
			T e = it2.next();
			if (!it2.hasNext()) {
				while (it.hasNext()) {
					it2 = it.next().iterator();
					if (it2.hasNext()) break;
				}
			}
			return e;
		}
	}
	
}
