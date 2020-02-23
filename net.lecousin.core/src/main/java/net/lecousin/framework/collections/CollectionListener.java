package net.lecousin.framework.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.Pair;

/**
 * A collection listener allows to signal the following events:
 * new elements, elements removed, and elements changed.
 * 
 * <p>
 * At initialization, the method elementsReady must be called once to set the collection's
 * initial elements. Then the other methods are used to signal modifications.
 * </p>
 *
 * @param <T> type of elements
 */
public interface CollectionListener<T> {
	
	/** First elements are ready. */
	default void elementsReady(Collection<? extends T> elements) {
		elementsAdded(elements);
	}

	/** Elements have been added. */
	void elementsAdded(Collection<? extends T> elements);
	
	/** Elements have been removed. */
	void elementsRemoved(Collection<? extends T> elements);
	
	/** Elements have changed. */
	void elementsChanged(Collection<? extends T> elements);
	
	/** An error occured while getting elements. */
	void error(Throwable error);
	
	/** Keeps a collection of elements, such as listeners can be added asynchronously.
	 * It implements CollectionListener to get collection changes events, and allows listeners
	 * to be added and removed even some events already occurred.
 * @param <T> type of elements
	 */
	public static class Keep<T> implements CollectionListener<T> {
		
		/** Constructor. */
		public Keep(Collection<T> implementation, Priority listenersCallPriority) {
			col = implementation;
			priority = listenersCallPriority;
		}
		
		protected Collection<T> col;
		protected Priority priority;
		protected Throwable error = null;
		protected LinkedList<Pair<CollectionListener<T>, Task<Void,NoException>>> listeners = new LinkedList<>();
		
		@Override
		public void elementsReady(Collection<? extends T> elements) {
			ArrayList<Pair<CollectionListener<T>, Task<Void,NoException>>> list;
			synchronized (col) {
				col.clear();
				col.addAll(elements);
				list = new ArrayList<>(listeners);
			}
			for (Pair<CollectionListener<T>, Task<Void,NoException>> l : list) {
				synchronized (l) {
					Task<Void, NoException> task = Task.cpu(
						"Call CollectionListener.elementsReady", priority,
						new Executable.FromRunnable(() -> l.getValue1().elementsReady(elements))
					);
					task.startAfter(l.getValue2());
					l.setValue2(task);
				}
			}
		}
		
		@Override
		public void elementsAdded(Collection<? extends T> elements) {
			ArrayList<Pair<CollectionListener<T>, Task<Void,NoException>>> list;
			synchronized (col) {
				col.addAll(elements);
				list = new ArrayList<>(listeners);
			}
			for (Pair<CollectionListener<T>, Task<Void,NoException>> l : list) {
				synchronized (l) {
					Task<Void, NoException> task = Task.cpu(
						"Call CollectionListener.elementsAdded", priority,
						new Executable.FromRunnable(() -> l.getValue1().elementsAdded(elements))
					);
					task.startAfter(l.getValue2());
					l.setValue2(task);
				}
			}
		}
		
		@Override
		public void elementsRemoved(Collection<? extends T> elements) {
			ArrayList<Pair<CollectionListener<T>, Task<Void,NoException>>> list;
			synchronized (col) {
				col.removeAll(elements);
				list = new ArrayList<>(listeners);
			}
			for (Pair<CollectionListener<T>, Task<Void,NoException>> l : list) {
				synchronized (l) {
					Task<Void, NoException> task = Task.cpu(
						"Call CollectionListener.elementsRemoved", priority,
						new Executable.FromRunnable(() -> l.getValue1().elementsRemoved(elements))
					);
					task.startAfter(l.getValue2());
					l.setValue2(task);
				}
			}
		}
		
		@Override
		public void elementsChanged(Collection<? extends T> elements) {
			ArrayList<Pair<CollectionListener<T>, Task<Void,NoException>>> list;
			synchronized (col) {
				list = new ArrayList<>(listeners);
			}
			for (Pair<CollectionListener<T>, Task<Void,NoException>> l : list) {
				synchronized (l) {
					Task<Void, NoException> task = Task.cpu(
						"Call CollectionListener.elementsChanged", priority,
						new Executable.FromRunnable(() -> l.getValue1().elementsChanged(elements))
					);
					task.startAfter(l.getValue2());
					l.setValue2(task);
				}
			}
		}
		
		@Override
		public void error(Throwable error) {
			ArrayList<Pair<CollectionListener<T>, Task<Void,NoException>>> list;
			synchronized (col) {
				this.error = error;
				list = new ArrayList<>(listeners);
			}
			for (Pair<CollectionListener<T>, Task<Void,NoException>> l : list) {
				synchronized (l) {
					Task<Void, NoException> task = Task.cpu("Call CollectionListener.error", priority,
						new Executable.FromRunnable(() -> l.getValue1().error(error))
					);
					task.startAfter(l.getValue2());
					l.setValue2(task);
				}
			}
		}
		
		/** Add a listener. */
		public void addListener(CollectionListener<T> listener) {
			synchronized (col) {
				if (error != null)
					listener.error(error);
				else
					listener.elementsReady(col);
				listeners.add(new Pair<>(listener, null));
			}
		}
		
		/**
		 * Remove a listener.
		 * Note that the listener may still be called if some calls are pending in tasks. 
		 */
		public void removeListener(CollectionListener<T> listener) {
			synchronized (col) {
				for (Iterator<Pair<CollectionListener<T>, Task<Void,NoException>>> it = listeners.iterator(); it.hasNext(); ) {
					if (it.next().getValue1() == listener) {
						it.remove();
						break;
					}
				}
			}
		}
		
	}
	
}
