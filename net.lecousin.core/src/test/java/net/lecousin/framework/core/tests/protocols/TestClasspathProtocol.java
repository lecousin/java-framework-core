package net.lecousin.framework.core.tests.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestClasspathProtocol extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		URL url = new URL("classpath:/net/lecousin/framework/locale/b.en");
		InputStream in = url.openStream();
		Assert.assertNotNull(in);
		in.close();
		url = new URL("classpath:/net/lecousin/framework/locale/b.enxxx");
		try {
			in = url.openStream();
			Assert.assertNull(in);
		} catch (IOException e) {
			// ok
		}
		url = new URL("classpath:/java/lang/String.class");
		in = url.openStream();
		Assert.assertNotNull(in);
		in.close();
	}
	
}
