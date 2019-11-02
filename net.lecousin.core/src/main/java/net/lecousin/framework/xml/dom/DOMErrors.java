package net.lecousin.framework.xml.dom;

import org.w3c.dom.DOMException;

/** Create DOMException. */
public final class DOMErrors {

	private DOMErrors() {
		// no instance
	}
	
	/** Operation not allowed. */
	public static DOMException operationNotAllowed() {
		return new DOMException(DOMException.NOT_SUPPORTED_ERR, "Operation not allowed");
	}
	
	/** Operation not supported. */
	public static DOMException operationNotSupported() {
		return new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported");
	}
	
	/** A child must be an XMLNode. */
	public static DOMException invalidChildType(Object child) {
		return new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "a child must implement XMLNode: " + child);
	}
	
	/** Cannot add a node inside itself. */
	public static DOMException cannotBeAChildOfItself() {
		return new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot add a node inside itself");
	}
	
	/** Cannot insert an ancestor into a descendent. */
	public static DOMException cannotAddAnAncestor() {
		return new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot insert an ancestor into a descendent");
	}
	
	/** Attribute does not exist on this element. */
	public static DOMException attributeDoesNotExist(String attributeName) {
		return new DOMException(DOMException.NOT_FOUND_ERR, "Attribute " + attributeName + " does not exist on this element");
	}
	
}
