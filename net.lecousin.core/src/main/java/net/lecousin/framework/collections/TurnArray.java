package net.lecousin.framework.collections;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.exception.NoException;

/**
 * Implementation of Deque using an array in an efficient way.
 * It keeps the index of the first and last element.
 * When removing the first element, the other elements are not moved, and the
 * first element is now at index 1 in the internal array.
 * When reaching the end of the array, if the first element is not at the index 0,
 * it can use the index 0 as the last index, such as the element at index 0 in 
 * the array becomes the element after the element at index array.length - 1.
 * <br/>
 * This allows removing/adding an element at the head to be as efficient as removing/adding at the tail
 * (no need to copy elements as in a classical array).
 * When full, its size is increased by half of the current size.
 * When removing an element and the size used becomes below half of the array,
 * the array size is divided by 2, but this operation is done in a separate task (priority LOW)
 * such as the remove operation is not slow down. When the task is executed, if the used size
 * increased in the meantime, the task does nothing.
 * @param <T> type of elements
 */
public class TurnArray<T> implements Deque<T> {

	/** Initialize with a size of 5. */
	public TurnArray() {
		this(5, 5);
	}

	/** Initialize with the given initial size. */
	public TurnArray(int initSize) {
		this(initSize, initSize);
	}
	
	/** Initialize with the given initial size. */
	public TurnArray(int initSize, int minSize) {
		if (initSize <= 0) throw new IllegalArgumentException("initSize must be positive, given: " + initSize);
		if (minSize < 5) minSize = 5;
		this.minSize = minSize;
		array = new Object[initSize];
	}
	
	private Object[] array;
	/** First element index (except if empty: start == end). */
	private int start = 0;
	/** Index of next element to insert, or -1 if the array is full. */
	private int end = 0;
	private int minSize;
	private DecreaseTask decreaseTask = null;
	
	public synchronized boolean isFull() {
		return end == -1;
	}
	
	@Override
	public synchronized void addLast(T element) {
		if (end == -1) increase();
		array[end++] = element;
		if (end == array.length) end = 0;
		if (end == start)
			end = -1;
	}
	
	@Override
	public synchronized void addFirst(T e) {
		if (end == -1) increase();
		if (start == 0) {
			start = array.length - 1;
			array[start] = e;
		} else {
			array[--start] = e;
		}
		if (end == start)
			end = -1;
	}
	
	@Override
	public synchronized boolean addAll(Collection<? extends T> elements) {
		int nb = elements.size();
		if (nb == 0) return false;
		int a = end == -1 ? 0 : end < start ? start - end : start + (array.length - end);
		if (a < nb) increase(array.length + (nb - a) + 5);
		for (T e : elements) {
			array[end++] = e;
			if (end == array.length) end = 0;
			if (end == start)
				end = -1;
		}
		return true;
	}
	
	@Override
	public synchronized T removeFirst() {
		if (end == start) throw new NoSuchElementException("Collection is empty");
		if (end == -1) end = start;
		@SuppressWarnings("unchecked")
		final T e = (T)array[start];
		array[start++] = null;
		if (start == array.length) start = 0;
		checkDecrease();
		return e;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized T removeLast() {
		if (end == start) throw new NoSuchElementException("Collection is empty");
		if (end == -1) end = start;
		T e;
		if (end == 0) {
			end = array.length - 1;
			e = (T)array[end];
		} else {
			e = (T)array[--end];
		}
		array[end] = null;
		checkDecrease();
		return e;
	}
	
	@Override
	public boolean isEmpty() {
		return start == end;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized T getFirst() {
		if (end == start) throw new NoSuchElementException("Collection is empty");
		return (T)array[start];
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized T getLast() {
		// empty
		if (end == start) throw new NoSuchElementException("Collection is empty");
		// full
		if (end == -1) {
			if (start == 0)
				return (T)array[array.length - 1];
			return (T)array[start - 1];
		}
		if (end == 0)
			return (T)array[array.length - 1];
		return (T)array[end - 1];
	}
	
	@Override
	public synchronized int size() {
		if (end == -1) return array.length;
		if (start == end) return 0;
		if (end > start) return end - start;
		return array.length - start + end;
	}
	
	@Override
	public synchronized void clear() {
		if (end == start) return; // empty
		if (end == -1) {
			// full
			for (int i = array.length - 1; i >= 0; --i)
				array[i] = null;
		} else if (end < start) {
			while (start < array.length) array[start++] = null;
			for (start = 0; start < end; ++start) array[start] = null;
		} else {
			while (start < end) array[start++] = null;
		}
		start = end = 0;
		checkDecrease();
	}
	
	@Override
	public boolean add(T e) {
		addLast(e);
		return true;
	}
	
	@Override
	public boolean offer(T e) {
		addLast(e);
		return false;
	}
	
	@Override
	public boolean offerFirst(T e) {
		addFirst(e);
		return true;
	}
	
	@Override
	public boolean offerLast(T e) {
		addLast(e);
		return true;
	}
	
	@Override
	public T peek() {
		return peekFirst();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized T peekFirst() {
		if (end == start) return null; // empty
		return (T)array[start];
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized T peekLast() {
		// empty
		if (end == start) return null;
		// full
		if (end == -1) {
			if (start == 0)
				return (T)array[array.length - 1];
			return (T)array[start - 1];
		}
		if (end == 0)
			return (T)array[array.length - 1];
		return (T)array[end - 1];
	}
	
	@Override
	public T poll() {
		return pollFirst();
	}
	
	@Override
	public synchronized T pollFirst() {
		if (end == start) return null;
		if (end == -1) end = start;
		@SuppressWarnings("unchecked")
		final T e = (T)array[start];
		array[start++] = null;
		if (start == array.length) start = 0;
		checkDecrease();
		return e;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized T pollLast() {
		if (end == start) return null;
		if (end == -1) end = start;
		T e;
		if (end == 0) {
			end = array.length - 1;
			e = (T)array[end];
		} else {
			e = (T)array[--end];
		}
		array[end] = null;
		checkDecrease();
		return e;
	}
	
	@Override
	public void push(T e) {
		addFirst(e);
	}
	
	@Override
	public T pop() {
		return removeFirst();
	}
	
	@Override
	public T element() {
		return getFirst();
	}
	
	@Override
	public T remove() {
		return removeFirst();
	}
	
	private void increase() {
		int newSize = array.length;
		newSize = newSize + (newSize >> 1);
		if (newSize < array.length + 5) newSize = array.length + 5;
		increase(newSize);
	}
	
	private void increase(int newSize) {
		if (decreaseTask != null)
			decreaseTask = null;
		Object[] a = new Object[newSize];
		if (end == -1) {
			System.arraycopy(array, start, a, 0, array.length - start);
			if (start > 0)
				System.arraycopy(array, 0, a, array.length - start, start);
			start = 0;
			end = array.length;
		} else if (end < start) {
			System.arraycopy(array, start, a, 0, array.length - start);
			System.arraycopy(array, 0, a, array.length - start, end + 1);
			end = array.length - start + end;
			start = 0;
		} else {
			System.arraycopy(array, start, a, 0, end - start + 1);
			end -= start;
			start = 0;
		}
		array = a;
	}
	
	private void checkDecrease() {
		if (decreaseTask != null) return;
		if (!Threading.isInitialized()) return;
		if (array.length > minSize && size() < array.length - (array.length >> 1))
			decreaseTask = new DecreaseTask();
	}
	
	private void decrease() {
		int newSize = array.length - (array.length >> 1);
		if (newSize < minSize) newSize = minSize;
		if (newSize >= array.length) return;
		if (size() >= newSize) return;
		if (end == -1) return;
		Object[] a = new Object[newSize];
		if (end > start) {
			System.arraycopy(array, start, a, 0, end - start);
			end = end - start;
			start = 0;
		} else if (end < start) {
			System.arraycopy(array, start, a, 0, array.length - start);
			if (end > 0)
				System.arraycopy(array, 0, a, array.length - start, end);
			end = array.length - start + end;
			start = 0;
		} else {
			start = end = 0;
		}
		array = a;
	}
	
	private class DecreaseTask extends Task.Cpu<Void,NoException> {
		public DecreaseTask() {
			super("Decrease Queue_ArrayRound size", Task.PRIORITY_LOW);
			start();
		}
		
		@Override
		public Void run() {
			synchronized (TurnArray.this) {
				if (decreaseTask != this) return null;
				decreaseTask = null;
				decrease();
			}
			return null;
		}
	}
	
	// other operation which may cost
	
	@Override
	public synchronized boolean contains(Object o) {
		if (start == end) return false; // empty
		if (end == -1) {
			// full
			for (int i = array.length - 1; i >= 0; --i)
				if (array[i].equals(o))
					return true;
			return false;
		}
		if (end < start) {
			for (int i = array.length - 1; i >= start; --i)
				if (array[i].equals(o))
					return true;
			for (int i = 0; i < end; ++i)
				if (array[i].equals(o))
					return true;
			return false;
		}
		for (int i = start; i < end; ++i)
			if (array[i].equals(o))
				return true;
		return false;
	}

	/** The implementation of this operation has not been optimised. */
	@Override
	public synchronized boolean containsAll(Collection<?> c) {
		for (Object o : c) if (!contains(o)) return false;
		return true;
	}

	/** remove this element, not necessarily the first or last occurrence. */
	public synchronized boolean removeAny(Object element) {
		if (end == start) return false;
		if (end == -1) {
			for (int i = array.length - 1; i >= 0; --i)
				if (array[i].equals(element)) {
					removeAt(i);
					return true;
				}
			return false;
		}
		if (end < start) {
			for (int i = array.length - 1; i >= start; --i)
				if (array[i].equals(element)) {
					removeAt(i);
					return true;
				}
			for (int i = 0; i < end; ++i)
				if (array[i].equals(element)) {
					removeAt(i);
					return true;
				}
			return false;
		}
		for (int i = start; i < end; ++i)
			if (array[i].equals(element)) {
				removeAt(i);
				return true;
			}
		return false;
	}
	
	@Override
	public synchronized boolean removeFirstOccurrence(Object element) {
		if (end == start) return false;
		if (end == -1) {
			for (int i = start; i < array.length; ++i)
				if (array[i].equals(element)) {
					removeAt(i);
					return true;
				}
			for (int i = 0; i < start; ++i)
				if (array[i].equals(element)) {
					removeAt(i);
					return true;
				}
			return false;
		}
		if (end < start) {
			for (int i = start; i < array.length; ++i)
				if (array[i].equals(element)) {
					removeAt(i);
					return true;
				}
			for (int i = 0; i < end; ++i)
				if (array[i].equals(element)) {
					removeAt(i);
					return true;
				}
			return false;
		}
		for (int i = start; i < end; ++i)
			if (array[i].equals(element)) {
				removeAt(i);
				return true;
			}
		return false;
	}
	
	@Override
	public synchronized boolean removeLastOccurrence(Object element) {
		if (end == start) return false;
		if (end == -1) {
			for (int i = start - 1; i >= 0; --i)
				if (array[i].equals(element)) {
					removeAt(i);
					return true;
				}
			for (int i = array.length - 1; i >= start; --i)
				if (array[i].equals(element)) {
					removeAt(i);
					return true;
				}
			return false;
		}
		if (end < start) {
			for (int i = end - 1; i >= 0; --i)
				if (array[i].equals(element)) {
					removeAt(i);
					return true;
				}
			for (int i = array.length - 1; i >= start; --i)
				if (array[i].equals(element)) {
					removeAt(i);
					return true;
				}
			return false;
		}
		for (int i = end - 1; i >= start; --i)
			if (array[i].equals(element)) {
				removeAt(i);
				return true;
			}
		return false;
	}

	/** Remove the given instance. */
	public synchronized boolean removeInstance(T element) {
		if (end == start) return false;
		if (end == -1) {
			for (int i = array.length - 1; i >= 0; --i)
				if (array[i] == element) {
					removeAt(i);
					return true;
				}
			return false;
		}
		if (end < start) {
			for (int i = array.length - 1; i >= start; --i)
				if (array[i] == element) {
					removeAt(i);
					return true;
				}
			for (int i = 0; i < end; ++i)
				if (array[i] == element) {
					removeAt(i);
					return true;
				}
			return false;
		}
		for (int i = start; i < end; ++i)
			if (array[i] == element) {
				removeAt(i);
				return true;
			}
		return false;
	}
	
	// skip checkstyle: OverloadMethodsDeclarationOrder
	@Override
	public boolean remove(Object o) {
		return removeFirstOccurrence(o);
	}
	
	/** The implementation of this operation has not been optimised. */
	@Override
	public synchronized boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object o : c) changed |= remove(o);
		return changed;
	}
	
	private void removeAt(int index) {
		try {
			if (index >= start) {
				if (end == -1) {
					// array is full
					if (index < array.length - 1)
						System.arraycopy(array, index + 1, array, index, array.length - index - 1);
					if (start == 0) {
						end = array.length - 1;
						array[end] = null;
						return;
					}
					array[array.length - 1] = array[0];
					if (start == 1) {
						array[0] = null;
						end = 0;
						return;
					}
					System.arraycopy(array, 1, array, 0, start - 1);
					end = start - 1;
					array[end] = null;
					return;
				}
				if (index == end - 1) {
					// last element, just null it and decrement end
					array[index] = null;
					end--;
					return;
				}
				if (end > start) {
					// we just need to shift elements and decrease end
					System.arraycopy(array, index + 1, array, index, end - index - 1);
					array[--end] = null;
					return;
				}
				// end < start
				if (index < array.length - 1)
					System.arraycopy(array, index + 1, array, index, array.length - index - 1);
				if (end == 0) {
					end = array.length - 1;
					array[end] = null;
					return;
				}
				array[array.length - 1] = array[0];
				if (end == 1) {
					array[0] = null;
					end = 0;
					return;
				}
				System.arraycopy(array, 1, array, 0, end - 1);
				array[--end] = null;
				return;
			}
			// index < start
			if (end == -1) {
				// array is full
				if (index == start - 1) {
					// last element
					array[index] = null;
					end = index;
					return;
				}
				System.arraycopy(array, index + 1, array, index, start - index - 1);
				array[start - 1] = null;
				end = start - 1;
				return;
			}
			if (index == end - 1) {
				// last element
				array[index] = null;
				end--;
				return;
			}
			System.arraycopy(array, index + 1, array, index, end - index);
			array[--end] = null;
			return;
		} finally {
			checkDecrease();
		}
	}
	
	/** Not implemented. */
	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("retainAll");
	}
	
	@Override
	public Iterator<T> iterator() {
		return new It();
	}
	
	private class It implements Iterator<T> {
		public It() {
			pos = 0;
		}
		
		private int pos;
		
		@Override
		public boolean hasNext() {
			if (pos >= size()) return false;
			return true;
		}
		
		@Override
		public T next() {
			synchronized (TurnArray.this) {
				if (pos >= size()) throw new NoSuchElementException();
				int i = start + pos;
				if (i >= array.length) i -= array.length;
				@SuppressWarnings("unchecked")
				T e = (T)array[i];
				pos++;
				return e;
			}
		}
	}

	@Override
	public Iterator<T> descendingIterator() {
		return new DIt();
	}
	
	private class DIt implements Iterator<T> {
		public DIt() {
			pos = size() - 1;
		}
		
		private int pos;
		
		@Override
		public boolean hasNext() {
			return pos >= 0;
		}
		
		@Override
		public T next() {
			if (pos < 0) throw new NoSuchElementException();
			synchronized (TurnArray.this) {
				int i = start + pos;
				if (i >= array.length) i -= array.length;
				@SuppressWarnings("unchecked")
				T e = (T)array[i];
				pos--;
				return e;
			}
		}
	}
	
	/** Remove all elements and return them, but the returned list is not ordered.
	 * The reason it is not ordered is for performance, when the order is not important.
	 */
	@SuppressWarnings("unchecked")
	public synchronized List<T> removeAllNoOrder() {
		if (end == -1) {
			List<Object> a = Arrays.asList(array);
			array = new Object[array.length];
			start = end = 0;
			return (List<T>)a;
		}
		if (start == end) return Collections.EMPTY_LIST;
		if (end > start) {
			Object[] a = new Object[end - start];
			System.arraycopy(array, start, a, 0, end - start);
			for (int i = start; i < end; ++i) array[i] = null;
			start = end = 0;
			return (List<T>)Arrays.asList(a);
		}
		Object[] a = new Object[array.length - start + end];
		System.arraycopy(array, start, a, 0, array.length - start);
		if (end > 0)
			System.arraycopy(array, 0, a, array.length - start, end);
		for (int i = array.length - 1; i >= 0; --i) array[i] = null;
		start = end = 0;
		return (List<T>)Arrays.asList(a);
	}
	
	@Override
	public synchronized Object[] toArray() {
		if (end == start) return new Object[0];
		if (end == -1) {
			Object[] a = new Object[array.length];
			System.arraycopy(array, start, a, 0, array.length - start);
			if (start > 0)
				System.arraycopy(array, 0, a, array.length - start, start);
			return a;
		}
		Object[] a = new Object[size()];
		if (end < start) {
			System.arraycopy(array, start, a, 0, array.length - start);
			if (end > 0) System.arraycopy(array, 0, a, array.length - start, end);
		} else {
			System.arraycopy(array, start, a, 0, end - start);
		}
		return a;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized <U extends Object> U[] toArray(U[] a) {
		if (end == start) return a;
		int nb = size();
		if (a.length < nb)
			a = (U[])Array.newInstance(a.getClass().getComponentType(), nb);
		if (end == -1) {
			System.arraycopy(array, start, a, 0, array.length - start);
			if (start > 0)
				System.arraycopy(array, 0, a, array.length - start, start);
			return a;
		}
		if (end < start) {
			System.arraycopy(array, start, a, 0, array.length - start);
			if (end > 0) System.arraycopy(array, 0, a, array.length - start, end);
		} else {
			System.arraycopy(array, start, a, 0, end - start);
		}
		return a;
	}
	
}
