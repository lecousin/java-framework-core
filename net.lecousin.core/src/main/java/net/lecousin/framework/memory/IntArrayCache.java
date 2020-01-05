package net.lecousin.framework.memory;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;

/**
 * Cache of byte[] useful when arrays of same size may be needed, to avoid memory allocation and garbage collection.
 */
public class IntArrayCache extends ArrayCache<int[]> {
	
	/** Get the default instance. */
	public static IntArrayCache getInstance() {
		Application app = LCCore.getApplication();
		synchronized (IntArrayCache.class) {
			IntArrayCache instance = app.getInstance(IntArrayCache.class);
			if (instance != null)
				return instance;
			instance = new IntArrayCache();
			app.setInstance(IntArrayCache.class, instance);
			return instance;
		}
	}
	
	private IntArrayCache() {
		// private
	}
	
	@Override
	protected int[] allocate(int size) {
		return new int[size];
	}
	
	@Override
	protected int length(int[] buf) {
		return buf.length;
	}
	
	@Override
	public String getDescription() {
		return "Integer arrays cache";
	}
}
