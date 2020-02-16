package net.lecousin.framework.core.test.io.provider;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.core.test.io.TestIOError;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.io.provider.IOProviderFrom;
import net.lecousin.framework.io.provider.IOProviderFromURI;
import net.lecousin.framework.io.util.EmptyReadable;

public class TestURIProvider implements IOProviderFrom<URI> {

	private static final TestURIProvider instance = new TestURIProvider();
	
	private Map<String, IOProvider> paths = new HashMap<>();
	
	public static TestURIProvider getInstance() {
		return instance;
	}
	
	private TestURIProvider() {
		IOProviderFromURI.getInstance().registerProtocol("test", this);

		// test error cases
		register("/errors/provider/readable", new IOProvider.Readable() {
			
			@Override
			public String getDescription() {
				return "/errors/provider/readable";
			}
			
			@Override
			public IO.Readable provideIOReadable(byte priority) throws IOException {
				throw new IOException("It's normal: " + getDescription());
			}
		});
		register("/errors/provider/writable", new IOProvider.Writable() {
			
			@Override
			public String getDescription() {
				return "/errors/provider/writable";
			}
			
			@Override
			public IO.Writable provideIOWritable(byte priority) throws IOException {
				throw new IOException("It's normal: " + getDescription());
			}
		});
		register("/errors/always/readable", new IOProvider.Readable() {
			
			@Override
			public String getDescription() {
				return TestIOError.ReadableAlwaysError.class.getName();
			}
			
			@Override
			public IO.Readable provideIOReadable(byte priority) throws IOException {
				return new TestIOError.ReadableAlwaysError();
			}
		});
		register("/errors/always/readable/knownsize", new IOProvider.Readable.KnownSize() {
			
			@Override
			public String getDescription() {
				return TestIOError.ReadableAlwaysError.KnownSizeAlwaysError.class.getName();
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public TestIOError.ReadableAlwaysError.KnownSizeAlwaysError provideIOReadableKnownSize(byte priority) throws IOException {
				return new TestIOError.ReadableAlwaysError.KnownSizeAlwaysError();
			}
		});
		
		// test empty
		register("/readable/empty", new IOProvider.Readable.KnownSize() {
			
			@Override
			public String getDescription() {
				return EmptyReadable.class.getName();
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public EmptyReadable provideIOReadableKnownSize(byte priority) throws IOException {
				return new EmptyReadable("empty", priority);
			}
		});
	}
	
	public void register(String path, IOProvider provider) {
		paths.put(path, provider);
	}
	
	@Override
	public IOProvider get(URI from) {
		IOProvider provider = paths.get(from.getPath());
		if (provider == null)
			LCCore.getApplication().getDefaultLogger().warn("Path not registered in TestURIProvider: " + from.getPath());
		return provider;
	}
	
}
