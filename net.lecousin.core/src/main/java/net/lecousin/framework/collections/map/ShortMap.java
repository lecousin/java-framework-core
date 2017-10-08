package net.lecousin.framework.collections.map;

/**
 * Interface for a Map that accept short keys (primitive type instead of Short).
 * @param <ValueType> type of values
 */
public interface ShortMap<ValueType> {

	/** Put a value and return the previous value associated with this key, or null. */
	public ValueType put(short key, ValueType entry);

	/** Get a value. */
	public ValueType get(short key);
	
	/** Remove a value. */
	public ValueType remove(short key);
	
	/** Return true if the map contains the given key. */
	public boolean containsKey(short key);
	
	/** Return the number of elements. */
	public int size();
	
	/** Return true if this map is empty. */
	public default boolean isEmpty() { return size() == 0; }

}
