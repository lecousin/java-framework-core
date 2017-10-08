package net.lecousin.framework.util;

/**
 * Provide an object of type T.
 * @param <T> type of provided object
 */
public interface Provider<T> {

	/** Provide an instance. */
	public T provide();
	
	/**
	 * Provide an instance from a given value.
	 *
	 * @param <ValueType> type of value to give as parameter
	 * @param <ProvidedType> type of object provided
	 */
	public static interface FromValue<ValueType,ProvidedType> {
		/** Provide an instance for the given value. */
		public ProvidedType provide(ValueType value);
	}
	
}
