package net.lecousin.framework.io.provider;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;

import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.util.ClassUtil;

/**
 * Provide IO.Readable from resources available from a class loader.
 */
public class IOProviderFromPathUsingClassloader implements IOProviderFrom.Readable<String> {

	/** Constructor. */
	public IOProviderFromPathUsingClassloader(ClassLoader loader) {
		this.loader = loader;
	}
	
	private ClassLoader loader;
	
	@Override
	public IOProvider.Readable get(String path) {
		if (path.startsWith("/")) path = path.substring(1);
		if (loader instanceof IOProviderFrom.Readable) {
			Method m = ClassUtil.getMethodFor(loader.getClass(), "get", String.class);
			if (m != null) {
				IOProvider.Readable provider;
				try { provider = (IOProvider.Readable)m.invoke(loader, path); }
				catch (Exception t) { provider = null; }
				if (provider != null)
					return provider;
			}
		}
		URL url = loader.getResource(path);
		if (url == null)
			return null;
		String p = path;
		return new IOProvider.Readable() {
			@Override
			public IO.Readable provideIOReadable(Priority priority) throws FileNotFoundException {
				InputStream in = loader.getResourceAsStream(p);
				if (in == null)
					throw new FileNotFoundException(p);
				return new IOFromInputStream(in, p, Threading.getUnmanagedTaskManager(), priority);
			}

			@Override
			public String getDescription() {
				return p;
			}
		};
	}
	
}
