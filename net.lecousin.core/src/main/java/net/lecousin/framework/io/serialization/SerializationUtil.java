package net.lecousin.framework.io.serialization;

/** Utilities for serialization. */
public final class SerializationUtil {
	
	private SerializationUtil() {
		// no instance
	}

	/** Used to serialize a Map entry.
	 * @param <Key> type of key
	 * @param <Value> type of value
	 */
	@SuppressWarnings("squid:ClassVariableVisibilityCheck")
	public static class MapEntry<Key, Value> {
		public Key key;
		public Value value;
	}
	
}
