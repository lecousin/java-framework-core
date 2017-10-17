package net.lecousin.framework.collections.map;

/**
 * Interface for a Map that accept int keys (primitive type instead of Integer).
 * @param <ValueType> type of values
 */
public interface IntegerMap<ValueType> {

	/** Put a value and return the previous value associated with this key, or null. */
	public ValueType put(int key, ValueType entry);

	/** Get a value. */
	public ValueType get(int key);
	
	/** Remove a value. */
	public ValueType remove(int key);
	
	/** Return true if the map contains the given key. */
	public boolean containsKey(int key);
	
	/** Return the number of elements. */
	public int size();
	
	/** Return true if this map is empty. */
	public default boolean isEmpty() { return size() == 0; }
	
}
