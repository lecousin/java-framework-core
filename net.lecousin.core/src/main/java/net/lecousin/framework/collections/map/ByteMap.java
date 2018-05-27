package net.lecousin.framework.collections.map;

/**
 * Interface for a Map that accept byte keys (primitive type instead of Byte).
 * @param <ValueType> type of values
 */
public interface ByteMap<ValueType> extends PrimitiveMap<ValueType> {

	/** Put a value and return the previous value associated with this key, or null. */
	public ValueType put(byte key, ValueType entry);

	/** Get a value. */
	public ValueType get(byte key);
	
	/** Remove a value. */
	public ValueType remove(byte key);
	
	/** Return true if the map contains the given key. */
	public boolean containsKey(byte key);
	
}
