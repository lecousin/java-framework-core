package net.lecousin.framework.util;

/**
 * Interface for objects which can store attributes.
 */
public interface AttributesContainer {

	/** Set an attribute. */
	void setAttribute(String name, Object value);
	
	/** Return the requested attribute, or null if no such attribute exists. */
	Object getAttribute(String name);
	
	/** Remove and return an attribute. */
	Object removeAttribute(String name);
	
	/** Return true if the given attribute is present. */
	boolean hasAttribute(String name);
	
}
