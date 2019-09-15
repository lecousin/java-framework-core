package net.lecousin.framework.collections.map;

import java.util.Iterator;

/**
 * Map with a primitive as key.
 * @param <ValueType> type of values
 */
public interface PrimitiveMap<ValueType> {

	/** Return the number of elements. */
	int size();
	
	/** Return true if this map is empty. */
	boolean isEmpty();
	
	/** Clear all elements of this map. */
	void clear();
	
	/** Iterates on values. */
	Iterator<ValueType> values();
	
}
