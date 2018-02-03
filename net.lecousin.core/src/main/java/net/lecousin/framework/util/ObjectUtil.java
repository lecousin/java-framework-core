package net.lecousin.framework.util;

/** Utility methods on objects. */
public final class ObjectUtil {
	
	private ObjectUtil() { /* no instance */ }

	/** Return true if both are null, or if they are equals. */
	public static boolean equalsOrNull(Object o1, Object o2) {
		if (o1 == null) return o2 == null;
		return o1.equals(o2);
	}
	
	/** Return "null" if o is null, else call the method toString on the object. */
	public static String toString(Object o) {
		if (o == null) return "null";
		return o.toString();
	}
	
}
