package net.lecousin.framework.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.lecousin.framework.event.Listener;
import net.lecousin.framework.util.CloseableListenable;

/** Implementation of CacheManager that associate each cached data to a key.
 * @param <Key> type of key
 * @param <Value> type of cached data
 */
public class Cache<Key,Value> implements CacheManager {

	/** Constructor. */
	public Cache(String description, Listener<Value> freer) {
		this.description = description;
		this.freer = freer;
		MemoryManager.register(this);
	}
	
	private String description;
	private Listener<Value> freer;
	
	private static class Data<Value> implements CachedData {
		public ArrayList<CloseableListenable> users = new ArrayList<>();
		public Value value;
		public long lastUsage = 0;
		
		@Override
		public int cachedDataCurrentUsage() {
			return users.size();
		}
		
		@Override
		public long cachedDataLastUsage() {
			return lastUsage;
		}
	}
	
	private HashMap<Key,Data<Value>> map = new HashMap<>();
	private HashMap<Value,Data<Value>> values = new HashMap<>();
	
	/** Return the cached data corresponding to the given key, or null if it does not exist.
	 * The given user is added as using the cached data. Once closed, the user is automatically
	 * removed from the list of users.
	 */
	public synchronized Value get(Key key, CloseableListenable user) {
		Data<Value> data = map.get(key);
		if (data == null) return null;
		data.users.add(user);
		if (user != null)
			user.addCloseListener(new Listener<CloseableListenable>() {
				@Override
				public void fire(CloseableListenable event) {
					event.removeCloseListener(this);
					free(data.value, event);
				}
			});
		return data.value;
	}
	
	/** Signal that the given cached data is not anymore used by the given user. */
	public synchronized void free(Value value, CloseableListenable user) {
		Data<Value> data = values.get(value);
		if (data == null) return;
		data.lastUsage = System.currentTimeMillis();
		data.users.remove(user);
	}
	
	/** Add the given data to this cache, and add the given first user to it. */
	public synchronized void put(Key key, Value value, CloseableListenable firstUser) {
		if (map.containsKey(key))
			throw new IllegalStateException("Cannot put a value in Cache with an existing key: " + key);
		if (values.containsKey(value))
			throw new IllegalStateException("Cannot put 2 times the same value in Cache: " + value);
		Data<Value> data = new Data<Value>();
		data.value = value;
		data.lastUsage = System.currentTimeMillis();
		data.users.add(firstUser);
		map.put(key, data);
		values.put(value, data);
	}
	
	@Override
	public Collection<? extends CachedData> getCachedData() {
		return map.values();
	}
	
	// skip checkstyle: OverloadMethodsDeclarationOrder
	@Override
	public synchronized boolean free(CachedData data) {
		@SuppressWarnings("unchecked")
		Value value = ((Data<Value>)data).value;
		values.remove(value);
		Key key = null;
		for (Map.Entry<Key,Data<Value>> e : map.entrySet())
			if (e.getValue() == data) {
				key = e.getKey();
				break;
			}
		map.remove(key);
		if (freer != null) freer.fire(value);
		return true;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public synchronized List<String> getItemsDescription() {
		ArrayList<String> list = new ArrayList<>(values.size());
		for (Data<Value> d : values.values()) {
			StringBuilder s = new StringBuilder();
			s.append(d.value.toString());
			s.append(" (last used ").append((System.currentTimeMillis() - d.lastUsage)).append("ms. ago)");
			s.append(" ").append(d.cachedDataCurrentUsage()).append(" users:");
			for (CloseableListenable user : d.users)
				s.append("\r\n    => " + user);
			list.add(s.toString());
		}
		return list;
	}
	
	/** Close this cache, and free any data it holds. */
	public synchronized void close() {
		for (Data<Value> d : map.values())
			freer.fire(d.value);
		map.clear();
		values.clear();
		MemoryManager.unregister(this);
	}
}
