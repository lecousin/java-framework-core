package net.lecousin.framework.memory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.Provider;

/**
 * A simple cache implementation.
 * @param <Key> type of key
 * @param <Data> type of cached data
 */
public class SimpleCache<Key,Data> implements IMemoryManageable, Closeable {

	/** Constructor. */
	public SimpleCache(String description, Provider.FromValue<Key,Data> provider) {
		this.description = description;
		this.provider = provider;
		MemoryManager.register(this);
	}
	
	private String description;
	private Provider.FromValue<Key,Data> provider;
	private HashMap<Key,Pair<Data,Long>> cache = new HashMap<>();
	
	@Override
	public void close() {
		MemoryManager.unregister(this);
		cache = null;
		provider = null;
	}
	
	/** Get a data, or create it using the provider. */
	public Data get(Key key) {
		synchronized (cache) {
			Pair<Data,Long> p = cache.get(key);
			if (p != null) {
				p.setValue2(Long.valueOf(System.currentTimeMillis()));
				return p.getValue1();
			}
			p = new Pair<>(provider.provide(key), Long.valueOf(System.currentTimeMillis()));
			cache.put(key, p);
			return p.getValue1();
		}
	}
	
	/** Remove the cached data associated with the given key. */
	public void remove(Key key) {
		synchronized (cache) {
			cache.remove(key);
		}
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public List<String> getItemsDescription() {
		synchronized (cache) {
			ArrayList<String> list = new ArrayList<>(cache.size());
			for (Pair<Data,Long> p : cache.values())
				list.add("Last usage " + (System.currentTimeMillis() - p.getValue2().longValue()) + "ms. ago: " + p.getValue1());
			return list;
		}
	}
	
	@Override
	public void freeMemory(FreeMemoryLevel level) {
		switch (level) {
		default:
		case EXPIRED_ONLY:
			free(System.currentTimeMillis() - 10 * 60 * 1000);
			break;
		case LOW:
			free(System.currentTimeMillis() - 2 * 60 * 1000);
			break;
		case MEDIUM:
			free(System.currentTimeMillis() - 30 * 1000);
			break;
		case URGENT:
			free(System.currentTimeMillis() - 3000);
			break;
		}
	}
	
	private void free(long keepTime) {
		ArrayList<Key> toRemove = new ArrayList<>();
		synchronized (cache) {
			for (Map.Entry<Key,Pair<Data,Long>> e : cache.entrySet())
				if (e.getValue().getValue2().longValue() < keepTime)
					toRemove.add(e.getKey());
			for (Key k : toRemove) cache.remove(k);
		}
	}
	
}
