package net.lecousin.framework.memory;

import java.util.ArrayList;

import net.lecousin.framework.application.LCCore;

/** Base class for implementing a cached data that register its users.
 * @param <T> type of object cached
 */
public abstract class CachedObject<T> implements CacheManager.CachedData {

	/** Constructor. */
	public CachedObject(T object, long expiration) {
		this.object = object;
		this.expiration = expiration;
		if (LCCore.getApplication().isDebugMode())
			users = new ArrayList<>(2);
	}
	
	protected T object;
	protected int usage = 0;
	protected long expiration;
	protected long lastUsage = System.currentTimeMillis();
	protected ArrayList<Object> users = null;
	
	public T get() { return object; }
	
	/** Add the given object as user of this cached data. */
	public synchronized void use(Object user) {
		usage++;
		lastUsage = System.currentTimeMillis();
		if (users != null)
			users.add(user);
	}

	/** Remove the given object as user of this cached data. */
	public synchronized void release(Object user) {
		usage--;
		if (users != null && !users.remove(user))
			LCCore.getApplication().getDefaultLogger()
				.error("CachedObject released by " + user + " but not in users list", new Exception());
	}
	
	public int getUsage() { return usage; }
	
	public long getLastUsage() { return lastUsage; }
	
	public long getExpiration() { return expiration; }
	
	/** Close this cached data, and release any resource of it. */
	public void close() {
		closeCachedObject(object);
		object = null;
	}
	
	protected abstract void closeCachedObject(T object);
	
	@Override
	public int cachedDataCurrentUsage() {
		return usage;
	}
	
	@Override
	public long cachedDataLastUsage() {
		return lastUsage;
	}
	
}
