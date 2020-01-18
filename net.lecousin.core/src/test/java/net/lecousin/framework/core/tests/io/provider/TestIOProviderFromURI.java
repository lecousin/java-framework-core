package net.lecousin.framework.core.tests.io.provider;

import java.io.File;
import java.net.URI;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.io.provider.IOProviderFrom;
import net.lecousin.framework.io.provider.IOProviderFromURI;

import org.junit.Assert;
import org.junit.Test;

public class TestIOProviderFromURI extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		IOProviderFromURI provider = IOProviderFromURI.getInstance();
		provider.registerProtocol("testProtocol", new IOProviderFrom<URI>() {
			@Override
			public IOProvider get(URI from) {
				return new IOProvider() {
					@Override
					public String getDescription() {
						return "test:" + from.toString();
					}
				};
			}
		});
		Assert.assertEquals("test:testProtocol://hello", provider.get(new URI("testProtocol://hello")).getDescription());
		
		Assert.assertNull(provider.get(new URI("file:///does/not/exist")));
		Assert.assertNotNull(provider.get(new File("./pom.xml").toURI()));
		
		IOProvider p = provider.get(new URI("https://www.google.com"));
		Assert.assertNotNull(p);
		p.getDescription();
		Assert.assertTrue(p instanceof IOProvider.Readable);
		IO.Readable io = ((IOProvider.Readable)p).provideIOReadable(Task.PRIORITY_NORMAL);
		Assert.assertNotNull(io);
		io.close();

		p = provider.get(new URI("https://www.google.com/does_not_exist"));
		Assert.assertTrue(p instanceof IOProvider.Readable);
		try {
			io = ((IOProvider.Readable)p).provideIOReadable(Task.PRIORITY_NORMAL);
			throw new AssertionError("should throw an exception");
		} catch (Exception e) {
			// ok
		}
	}
	
}
