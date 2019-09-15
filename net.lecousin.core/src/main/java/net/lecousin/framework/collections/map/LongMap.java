package net.lecousin.framework.collections.map;

/**
 * Interface for a Map that accept long keys (primitive type instead of Long).
 * @param <ValueType> type of values
 */
public interface LongMap<ValueType> extends PrimitiveMap<ValueType> {

	/** Put a value and return the previous value associated with this key, or null. */
	ValueType put(long key, ValueType entry);

	/** Get a value. */
	ValueType get(long key);
	
	/** Remove a value. */
	ValueType remove(long key);
	
	/** Return true if the map contains the given key. */
	boolean containsKey(long key);
	
}
