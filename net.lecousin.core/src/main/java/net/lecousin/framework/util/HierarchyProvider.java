package net.lecousin.framework.util;

import java.util.List;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.threads.Task.Priority;

/**
 * Generic interface to navigate in a hierarchy.
 */
public interface HierarchyProvider {

	/**
	 * Generic interface to navigate in a hierarchy with synchronous operations.
	 * @param <ElementType> type of element
	 */
	public static interface Synchronous<ElementType> {
		/** Return the parent of the given element, or null if this is a root element. */
		ElementType getParent(ElementType element);
		
		/** Return true if the given element MAY have children, false if it is not possible. */
		boolean mayHaveChildren(ElementType element);
		
		/** Return true if the given element has children. */
		boolean hasChildren(ElementType element, boolean refresh);
		
		/** Return the children of the given element. */
		List<? extends ElementType> getChildren(ElementType element, boolean refresh);
		
		/**
		 * Go through the descendants of the given element, and call the listener for each element.
		 */
		default void goThrough(ElementType element, Consumer<ElementType> listener) {
			listener.accept(element);
			for (ElementType child : getChildren(element, true))
				goThrough(child, listener);
		}
	}

	/**
	 * Generic interface to navigate in a hierarchy with asynchronous operations.
	 * @param <ElementType> type of element
	 */
	public static interface Asynchronous<ElementType> {
		/** Return the parent of the given element, or null if this is a root element. */
		ElementType getParent(ElementType element);

		/** Return true if the given element MAY have children, false if it is not possible. */
		boolean mayHaveChildren(ElementType element);
		
		/** Return true if the given element has children. */
		AsyncSupplier<Boolean,Exception> hasChildren(ElementType element, boolean refresh, Priority priority);
		
		/** Return the children of the given element. */
		AsyncSupplier<List<? extends ElementType>,Exception> getChildren(ElementType element, boolean refresh, Priority priority);
	}
	
}
