package net.lecousin.framework.collections.map;

/**
 * Interface for a Map that accept short keys (primitive type instead of Short).
 * @param <ValueType> type of values
 */
public interface ShortMap<ValueType> extends PrimitiveMap<ValueType> {

	/** Put a value and return the previous value associated with this key, or null. */
	ValueType put(short key, ValueType entry);

	/** Get a value. */
	ValueType get(short key);
	
	/** Remove a value. */
	ValueType remove(short key);
	
	/** Return true if the map contains the given key. */
	boolean containsKey(short key);
	
}
