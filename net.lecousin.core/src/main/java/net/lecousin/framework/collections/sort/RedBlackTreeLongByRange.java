package net.lecousin.framework.collections.sort;

import java.util.Iterator;

import net.lecousin.framework.collections.sort.RedBlackTreeLong.Node;

/**
 * Similar as RedBlackTreeLong, but keeps performance with a large number of elements.
 * More a RedBlackTree has elements, more it becomes deep, and performance are decreasing.
 * This implementation uses a RedBlackTree, and each node contains another RedBlackTree.
 * Each node is responsible for a range of Long values.
 * For example, if the given rangeSize is 100, the first RedBlackTree will contain values
 * from 0 to 99, the second from 100 to 199, etc...
 * All those trees being themselves sorted by a RedBlackTree.
 * So when searching for a value, we first search for the range containing it, to
 * retrieve the RedBlackTree that contains the element associated with the searched long value.
 * @param <T> type of elements
 */
public class RedBlackTreeLongByRange<T> implements Sorted.AssociatedWithLong<T> {

	/** Constructor. */
	public RedBlackTreeLongByRange(long rangeSize) {
		this.rangeSize = rangeSize;
	}
	
	private long rangeSize;
	private int size = 0;
	private RedBlackTreeLong<RedBlackTreeLong<T>> ranges = new RedBlackTreeLong<>();
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public boolean isEmpty() {
		return size == 0;
	}
	
	@Override
	public void add(long value, T element) {
		long r = value / rangeSize;
		Node<RedBlackTreeLong<T>> e = ranges.get(r);
		if (e == null) {
			RedBlackTreeLong<T> rbt = new RedBlackTreeLong<>();
			rbt.add(value, element);
			ranges.add(r, rbt);
		} else {
			e.getElement().add(value, element);
		}
		size++;
	}

	@Override
	public boolean contains(long value, T element) {
		long r = value / rangeSize;
		Node<RedBlackTreeLong<T>> e = ranges.get(r);
		if (e == null) return false;
		return e.getElement().contains(value, element);
	}
	
	@Override
	public boolean containsInstance(long value, T element) {
		long r = value / rangeSize;
		Node<RedBlackTreeLong<T>> e = ranges.get(r);
		if (e == null) return false;
		return e.getElement().containsInstance(value, element);
	}
	
	@Override
	public void remove(long value, T element) {
		long r = value / rangeSize;
		Node<RedBlackTreeLong<T>> e = ranges.get(r);
		e.getElement().remove(value, element);
		size--;
		if (e.getElement().isEmpty())
			ranges.removeInstance(r, e.getElement());
	}

	@Override
	public void removeInstance(long value, T element) {
		long r = value / rangeSize;
		Node<RedBlackTreeLong<T>> e = ranges.get(r);
		e.getElement().removeInstance(value, element);
		size--;
		if (e.getElement().isEmpty())
			ranges.removeInstance(r, e.getElement());
	}
	
	/** Return the first node. */
	public Node<T> getMin() {
		if (size == 0) return null;
		Node<RedBlackTreeLong<T>> e = ranges.getMin();
		return e.getElement().getMin();
	}
	
	/** Remove the first node. */
	public void removeMin() {
		Node<RedBlackTreeLong<T>> e = ranges.getMin();
		e.getElement().removeMin();
		if (e.getElement().isEmpty())
			ranges.removeInstance(e.getValue(), e.getElement());
		size--;
	}
	
	@Override
	public Iterator<T> iterator() {
		return new It();
	}
	
	private class It implements Iterator<T> {
		public It() {
			itRange = ranges.iterator();
			subIt = null;
			forward();
		}
		
		private Iterator<RedBlackTreeLong<T>> itRange;
		private Iterator<T> subIt;
		
		private void forward() {
			do {
				if (subIt != null) {
					if (subIt.hasNext()) return;
					subIt = null;
				}
				if (itRange.hasNext()) {
					RedBlackTreeLong<T> rbt = itRange.next();
					subIt = rbt.iterator();
					continue;
				}
				break;
			} while (true);
		}
		
		@Override
		public boolean hasNext() {
			return subIt != null;
		}
		
		@Override
		public T next() {
			T e = subIt.next();
			if (!subIt.hasNext()) forward();
			return e;
		}
	}
}
