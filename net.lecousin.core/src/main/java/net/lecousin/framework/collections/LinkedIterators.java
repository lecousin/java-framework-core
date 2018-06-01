package net.lecousin.framework.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Makes an iterator from a list of iterators.
 * @param <T> type of elements
 */
public class LinkedIterators<T> implements Iterator<T> {

	/** Constructor. */
	public LinkedIterators() {
	}

	/** Constructor. */
	public LinkedIterators(Collection<Iterator<T>> iterators) {
		this.iterators.addAll(iterators);
	}
	
	/** Constructor. */
	@SafeVarargs
	public LinkedIterators(Iterator<T>... iterators) {
		for (int i = 0; i < iterators.length; ++i)
			this.iterators.add(iterators[i]);
	}
	
	private LinkedList<Iterator<T>> iterators = new LinkedList<>();
	private Iterator<T> current = null;
	
	/** Append an iterator. */
	public void addIterator(Iterator<T> it) {
		iterators.add(it);
	}
	
	@Override
	public boolean hasNext() {
		while (current == null || !current.hasNext()) {
			if (iterators.isEmpty()) return false;
			current = iterators.removeFirst();
		}
		return true;
	}
	
	@Override
	public T next() {
		while (current == null || !current.hasNext()) {
			if (iterators.isEmpty()) throw new NoSuchElementException();
			current = iterators.removeFirst();
		}
		return current.next();
	}
	
	@Override
	public void remove() {
		if (current == null) throw new NoSuchElementException();
		current.remove();
	}
	
}
