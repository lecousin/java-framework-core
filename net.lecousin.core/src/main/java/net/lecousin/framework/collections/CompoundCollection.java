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
			itCol = list.iterator();
			while (itCol.hasNext()) {
				itInner = itCol.next().iterator();
				if (itInner.hasNext()) break;
			}
		}
		
		private Iterator<Iterable<T>> itCol;
		private Iterator<T> itInner;
		
		@Override
		public boolean hasNext() {
			return itInner != null && itInner.hasNext();
		}
		
		@Override
		public T next() {
			T e = itInner.next();
			if (!itInner.hasNext()) {
				while (itCol.hasNext()) {
					itInner = itCol.next().iterator();
					if (itInner.hasNext()) break;
				}
			}
			return e;
		}
	}
	
}
