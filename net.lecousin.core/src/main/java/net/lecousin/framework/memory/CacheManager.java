package net.lecousin.framework.memory;

import java.util.Collection;

import net.lecousin.framework.collections.LinkedArrayList;

/** Interface for caching data. */
public interface CacheManager extends IMemoryManageable {

	/** A cached data. */
	public static interface CachedData {
		/** Return the number of <i>clients</i> currently using this data. */
		public int cachedDataCurrentUsage();
		
		/** Return the timestamp of the last time this data has been used. */
		public long cachedDataLastUsage();
	}
	
	/** Adds an expiration to a cahced data. */
	public static interface CachedDataCustomExpiration extends CachedData {
		/** Return the time this data will expire and should be freed if necessary. */
		public long cachedDataExpirationTime();
	}
	
	/** Return this list of cached data. */
	public Collection<? extends CachedData> getCachedData();
	
	/** Free the given data. */
	public boolean free(CachedData data);
	
	public default long getDefaultCachedDataExpiration() { return 15 * 60 * 1000; }
	
	/** Return the expiration time of the given data. */
	public default long getCachedDataExpiration(CachedData data) {
		if (data instanceof CachedDataCustomExpiration)
			return ((CachedDataCustomExpiration)data).cachedDataExpirationTime();
		return getDefaultCachedDataExpiration();
	}
	
	@Override
	public default void freeMemory(FreeMemoryLevel level) {
		if (FreeMemoryLevel.EXPIRED_ONLY.equals(level)) {
			LinkedArrayList<CachedData> toFree = new LinkedArrayList<>(10);
			long now = System.currentTimeMillis();
			for (CachedData data : getCachedData()) {
				if (data.cachedDataCurrentUsage() > 0) continue;
				long expiration = data.cachedDataLastUsage() + getCachedDataExpiration(data);
				if (now >= expiration)
					toFree.add(data);
			}
			for (CachedData data : toFree) free(data);
			return;
		}
		long maxIdle;
		switch (level) {
		default:
		case LOW:
			maxIdle = 5 * 60 * 1000;
			break;
		case MEDIUM:
			maxIdle = 60 * 1000;
			break;
		case URGENT:
			maxIdle = 0;
			break;
		}
		LinkedArrayList<CachedData> toFree = new LinkedArrayList<>(15);
		long now = System.currentTimeMillis();
		for (CachedData data : getCachedData())
			if (data.cachedDataCurrentUsage() == 0 && data.cachedDataLastUsage() - now <= maxIdle)
				toFree.add(data);
		for (CachedData data : toFree) free(data);
	}
	
}
