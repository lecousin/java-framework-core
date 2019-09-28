package net.lecousin.framework.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Collection where elements come asynchronously.
 * While new elements come, the method newElements is called, then the method done must be called when
 * no more elements will come.
 * @param <T> type of elements
 */
public interface AsyncCollection<T> {
	
	/** Called when new elements are ready. */
	void newElements(Collection<T> elements);
	
	/** Called when no more elements will come, and no error was encountered. */
	void done();
	
	/** Return true if the method done has been called already. */
	boolean isDone();
	
	/** Called if an error occured before all elements have been retrieved. */
	void error(Exception error);
	
	/** Return true if the method error has been called. */
	boolean hasError();
	
	/**
	 * Simple implementation with a listener on both operations: newElements and done.
	 * @param <T> type of elements
	 */
	public static class Listen<T> implements AsyncCollection<T> {
		/** Constructor. */
		public Listen(Consumer<Collection<T>> onElementsReady, Runnable onDone, Consumer<Exception> onError) {
			this.onElementsReady = onElementsReady;
			this.onDone = onDone;
			this.onError = onError;
		}
		
		private Consumer<Collection<T>> onElementsReady;
		private Runnable onDone;
		private boolean isDone = false;
		private Consumer<Exception> onError;
		private boolean hasError = false;
		
		@Override
		public void newElements(Collection<T> elements) {
			onElementsReady.accept(elements);
		}
		
		@Override
		public void done() {
			isDone = true;
			onDone.run();
		}
		
		@Override
		public boolean isDone() {
			return isDone;
		}
		
		@Override
		public void error(Exception error) {
			if (onError != null) onError.accept(error);
			hasError = true;
		}
		
		@Override
		public boolean hasError() {
			return hasError;
		}
	}
	
	/**
	 * Aggregates several AsyncCollection into a single one:
	 * each time new elements come, the newElements method is called into the main collection,
	 * then the done method is called on the main collection only after the done method
	 * has been called <i>nbSubCollection</i> times.
	 * @param <T> type of elements
	 */
	public static class Aggregator<T> implements AsyncCollection<T> {
		
		/** Constructor. */
		public Aggregator(int nbSubCollection, AsyncCollection<T> mainCollection) {
			remainingDone = nbSubCollection;
			collection = mainCollection;
		}
		
		private int remainingDone;
		private AsyncCollection<T> collection;
		
		@Override
		public synchronized void newElements(Collection<T> elements) {
			collection.newElements(elements);
		}
		
		@Override
		public synchronized void done() {
			if (--remainingDone == 0 && !collection.hasError())
				collection.done();
		}
		
		@Override
		public boolean isDone() {
			return remainingDone == 0 && !collection.hasError();
		}
		
		@Override
		public void error(Exception error) {
			collection.error(error);
		}
		
		@Override
		public boolean hasError() {
			return collection.hasError();
		}
	}
	
	/**
	 * AsyncCollection but where elements are expected to come only one by one instead of by bunch.
	 * @param <T> type of elements
	 */
	public static interface OneByOne<T> {
		/** Called when a new element is ready. */
		void newElement(T element);

		/** Called when no more element will come. */
		void done();
		
		/** Called when an error occured while retrieving elements. */
		void error(Exception error);
		
		/** A refreshable collection is a collection that can be restarted, with new elements coming and replacing the previous ones.
		 * @param <T> type of elements
		 */
		public static interface Refreshable<T> extends OneByOne<T> {
			/** Called when new elements will come to replace the previous ones. */
			void start();
		}
	}
	
	/** Implementation that keeps the elements in a list.
	 * @param <T> type of elements
	 */
	public static class Keep<T> implements AsyncCollection<T> {
		private LinkedArrayList<T> list = new LinkedArrayList<>(10);
		private boolean done = false;
		private Exception error = null;
		private ArrayList<AsyncCollection<T>> listeners = new ArrayList<>(5);
		private ArrayList<Runnable> doneListeners = new ArrayList<>(2);
		private ArrayList<Consumer<Exception>> errorListeners = new ArrayList<>(2);
		
		@Override
		public void newElements(Collection<T> elements) {
			ArrayList<AsyncCollection<T>> toCall;
			synchronized (this) {
				list.addAll(elements);
				toCall = new ArrayList<>(listeners);
			}
			for (AsyncCollection<T> col : toCall)
				col.newElements(elements);
		}
		
		@Override
		public void done() {
			synchronized (this) {
				done = true;
			}
			for (AsyncCollection<T> col : listeners)
				col.done();
			listeners = null;
			for (Runnable r : doneListeners)
				r.run();
			doneListeners = null;
		}
		
		@Override
		public boolean isDone() {
			return done;
		}
		
		/** Add a listener to be called when the done method is called.
		 * If the done method has been already called, the listener is immediately called.
		 */
		public void ondone(Runnable listener) {
			synchronized (this) {
				if (!done) {
					doneListeners.add(listener);
					return;
				}
			}
			listener.run();
		}
		
		@Override
		public void error(Exception error) {
			synchronized (this) {
				this.error = error;
			}
			for (AsyncCollection<T> col : listeners)
				col.error(error);
			listeners = null;
			for (Consumer<Exception> r : errorListeners)
				r.accept(error);
			errorListeners = null;
		}
		
		@Override
		public boolean hasError() {
			return error != null;
		}
		
		/** Add a listener to be called when the error method is called.
		 * If the error method has been already called, the listener is immediately called.
		 */
		public void onerror(Consumer<Exception> listener) {
			synchronized (this) {
				if (error == null) {
					errorListeners.add(listener);
					return;
				}
			}
			listener.accept(error);
		}
				
		/** Send the elements of this collection to the given collection.
		 * All elements already present in this collection are immediately given by calling the method newElements.
		 * If the method done has been called already on this collection, the method done is also called
		 * on the given collection.
		 */
		public void provideTo(AsyncCollection<T> col) {
			ArrayList<T> already = null;
			synchronized (this) {
				if (!done && error == null) {
					already = new ArrayList<>(list);
					listeners.add(col);
				}
			}
			if (already != null) {
				col.newElements(list);
				return;
			}
			col.newElements(list);
			if (error == null)
				col.done();
			else
				col.error(error);
		}
		
		/** Return the list containing the elements kept so far. */
		public List<T> getCurrentElements() {
			return list;
		}
	}
	
}
