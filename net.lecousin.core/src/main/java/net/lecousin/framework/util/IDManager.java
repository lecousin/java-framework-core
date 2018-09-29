package net.lecousin.framework.util;

/**
 * An ID manager can allocate new identifiers, and free identifiers which are not used anymore.
 * An identifier which has been freed may be returned again when allocating a new identifier.
 */
public interface IDManager {

	/** Allocate a new unique identifier. */
	public String allocate();
	
	/** Free the given identifier. */
	public void free(String id);
	
	/** Declare an identifier as used so it cannot be allocated. */
	public void used(String id);
	
}
