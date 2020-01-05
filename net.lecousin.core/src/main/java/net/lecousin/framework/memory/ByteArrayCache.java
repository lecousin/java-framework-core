package net.lecousin.framework.memory;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;

/**
 * Cache of byte[] useful when arrays of same size may be needed, to avoid memory allocation and garbage collection.
 */
public class ByteArrayCache extends ArrayCache<byte[]> {
	
	/** Get the default instance. */
	public static ByteArrayCache getInstance() {
		Application app = LCCore.getApplication();
		synchronized (ByteArrayCache.class) {
			ByteArrayCache instance = app.getInstance(ByteArrayCache.class);
			if (instance != null)
				return instance;
			instance = new ByteArrayCache();
			app.setInstance(ByteArrayCache.class, instance);
			return instance;
		}
	}
	
	private ByteArrayCache() {
		// private
	}
	
	@Override
	protected byte[] allocate(int size) {
		return new byte[size];
	}
	
	@Override
	protected int length(byte[] buf) {
		return buf.length;
	}
	
	@Override
	public String getDescription() {
		return "Byte arrays cache";
	}
}
