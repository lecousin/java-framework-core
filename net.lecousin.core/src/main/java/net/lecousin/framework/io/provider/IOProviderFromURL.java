package net.lecousin.framework.io.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;

/**
 * Provide IOs from URLs.
 */
public class IOProviderFromURL implements IOProviderFrom<URL> {

	public static IOProviderFromURL getInstance() { return instance; }
	
	private static IOProviderFromURL instance = new IOProviderFromURL();
	
	/** Register a new IOProvider from URL for a specific protocol. */
	public void registerProtocol(String protocol, IOProviderFrom<URL> provider) {
		protocols.put(protocol.toLowerCase(), provider);
	}
	
	private IOProviderFromURL() {
		protocols.put("file", new IOProviderFrom<URL>() {
			@Override
			public IOProvider get(URL from) {
				File file;
				try { file = new File(from.toURI()); }
				catch (Exception e) {
					return null;
				}
				if (!file.exists())
					return null;
				return new FileIOProvider(file);
			}
		});
	}
	
	private Map<String, IOProviderFrom<URL>> protocols = new HashMap<>();
	
	@Override
	public IOProvider get(URL from) {
		String protocol = from.getProtocol().toLowerCase();
		IOProviderFrom<URL> p = protocols.get(protocol);
		if (p != null)
			return p.get(from);
		return new IOProvider.Readable() {
			@Override
			public String getDescription() {
				return from.toString();
			}
			
			@SuppressWarnings("resource")
			@Override
			public IO.Readable provideIOReadable(byte priority) throws IOException {
				InputStream in = from.openStream();
				return new IOFromInputStream(in, from.toString(), Threading.getUnmanagedTaskManager(), priority);
			}
		};
	}
	
}
