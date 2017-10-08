package net.lecousin.framework.io.provider;

import java.io.IOException;
import java.io.InputStream;

import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.io.IO.Readable;
import net.lecousin.framework.io.IOFromInputStream;

/**
 * Provide IO.Readable from resources available from a class loader.
 */
public class IOProviderFromNameUsingClassloader implements IOProviderFromName.Readable {

	/** Constructor. */
	public IOProviderFromNameUsingClassloader(ClassLoader loader) {
		this.loader = loader;
	}
	
	private ClassLoader loader;
	
	@SuppressWarnings("resource")
	@Override
	public Readable provideReadableIO(String name, byte priority) throws IOException {
		if (loader instanceof IOProviderFromName.Readable)
			return ((IOProviderFromName.Readable)loader).provideReadableIO(name, priority);
		InputStream in = loader.getResourceAsStream(name);
		if (in == null)
			return null;
		// we specify CPU because most of the time it will be from a JAR, needing decompression,
		// and we don't want to block drive access. Anyway there is no way to know on which
		// physical drive it is.
		return new IOFromInputStream(in, name, Threading.getCPUTaskManager(), priority);
	}
	
}
