package net.lecousin.framework.memory;

import java.util.List;

/**
 * Object that can free some memory on demand.<br/>
 * This is typically a cache, or some data in memory that can be re-created again if necessary.<br/>
 * Such an object can be ask to free some memory at regular interval or in case the system does have enough memory.
 */
public interface IMemoryManageable {

	/** Describe the criticity level of a free memory request. */
	public enum FreeMemoryLevel {
		/** Only data that expired, this is the lowest level. */
		EXPIRED_ONLY,
		/** Low criticity, the implementation can just free some objects. */
		LOW,
		/** Medium criticity, the implementation is not requested to free everything, but as much as possible. */
		MEDIUM,
		/** Urgent criticity, all possible memory should be freed. */
		URGENT
	}
	
	/** Desciption. */
	String getDescription();
	
	/** Describe each item that can be freed. */
	List<String> getItemsDescription();
	
	/** Request to free some memory. */
	void freeMemory(FreeMemoryLevel level);
	
}
