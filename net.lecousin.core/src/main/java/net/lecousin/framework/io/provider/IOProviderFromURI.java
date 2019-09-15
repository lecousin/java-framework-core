package net.lecousin.framework.io.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;

/**
 * Provide IOs from URLs.
 */
public class IOProviderFromURI implements IOProviderFrom<URI> {

	public static IOProviderFromURI getInstance() { return instance; }
	
	private static IOProviderFromURI instance = new IOProviderFromURI();
	
	/** Register a new IOProvider from URL for a specific protocol. */
	public void registerProtocol(String protocol, IOProviderFrom<URI> provider) {
		protocols.put(protocol.toLowerCase(), provider);
	}
	
	private IOProviderFromURI() {
		protocols.put("file", new IOProviderFrom<URI>() {
			@Override
			public IOProvider get(URI from) {
				File file;
				try { file = new File(from); }
				catch (Exception e) {
					return null;
				}
				if (!file.exists())
					return null;
				return new FileIOProvider(file);
			}
		});
	}
	
	private Map<String, IOProviderFrom<URI>> protocols = new HashMap<>();
	
	@Override
	public IOProvider get(URI from) {
		String protocol = from.getScheme().toLowerCase();
		IOProviderFrom<URI> p = protocols.get(protocol);
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
				InputStream in = from.toURL().openStream();
				return new IOFromInputStream(in, from.toString(), Threading.getUnmanagedTaskManager(), priority);
			}
		};
	}
	
}
