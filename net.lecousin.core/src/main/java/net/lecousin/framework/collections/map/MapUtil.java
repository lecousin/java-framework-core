package net.lecousin.framework.collections.map;

import java.util.ArrayList;
import java.util.Map;

/**
 * Utility methods for Maps.
 */
public final class MapUtil {
	
	private MapUtil() {
		/* no instance */
	}

	/** Remove all keys associated with the given value. */
	public static <T> void removeValue(Map<?,T> map, T value) {
		ArrayList<Object> keys = new ArrayList<>();
		for (Map.Entry<?,T> e : map.entrySet())
			if (e.getValue().equals(value))
				keys.add(e.getKey());
		for (Object key : keys)
			map.remove(key);
	}
	
}
