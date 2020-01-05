package net.lecousin.framework.memory;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;

/**
 * Cache of byte[] useful when arrays of same size may be needed, to avoid memory allocation and garbage collection.
 */
public class CharArrayCache extends ArrayCache<char[]> {
	
	/** Get the default instance. */
	public static CharArrayCache getInstance() {
		Application app = LCCore.getApplication();
		synchronized (CharArrayCache.class) {
			CharArrayCache instance = app.getInstance(CharArrayCache.class);
			if (instance != null)
				return instance;
			instance = new CharArrayCache();
			app.setInstance(CharArrayCache.class, instance);
			return instance;
		}
	}
	
	private CharArrayCache() {
		// private
	}
	
	@Override
	protected char[] allocate(int size) {
		return new char[size];
	}
	
	@Override
	protected int length(char[] buf) {
		return buf.length;
	}
	
	@Override
	public String getDescription() {
		return "Char arrays cache";
	}
}
