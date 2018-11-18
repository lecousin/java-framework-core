package net.lecousin.framework.util;

/**
 * An ID manager can allocate new identifiers, and free identifiers which are not used anymore.
 * An identifier which has been freed may be returned again when allocating a new identifier.
 */
public interface IDManagerLong {

	/** Allocate a new unique identifier. */
	public long allocate();
	
	/** Free the given identifier. */
	public void free(long id);
	
	/** Declare an identifier as used so it cannot be allocated. */
	public void used(long id);
	
}
