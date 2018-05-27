package net.lecousin.framework.collections.map;

/**
 * Interface for a Map that accept long keys (primitive type instead of Long).
 * @param <ValueType> type of values
 */
public interface LongMap<ValueType> extends PrimitiveMap<ValueType> {

	/** Put a value and return the previous value associated with this key, or null. */
	public ValueType put(long key, ValueType entry);

	/** Get a value. */
	public ValueType get(long key);
	
	/** Remove a value. */
	public ValueType remove(long key);
	
	/** Return true if the map contains the given key. */
	public boolean containsKey(long key);
	
}
