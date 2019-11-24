package net.lecousin.framework.core.tests.protocols;

import java.io.InputStream;
import java.net.URL;

import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestStringProtocol extends LCCoreAbstractTest {

	@Test
	public void testUTF8() throws Exception {
		URL url = new URL("string://UTF-8/this is a test");
		InputStream in = url.openStream();
		byte[] b = new byte[1024];
		int off = 0;
		do {
			int nb = in.read(b, off, b.length - off);
			if (nb <= 0) break;
			off += nb;
		} while (true);
		in.close();
		Assert.assertEquals(14, off);
		byte[] b2 = new byte[14];
		System.arraycopy(b, 0, b2, 0, 14);
		Assert.assertArrayEquals(b2, new byte[] { 't','h','i','s',' ','i','s',' ','a',' ','t','e','s','t' });
	}
	
}
