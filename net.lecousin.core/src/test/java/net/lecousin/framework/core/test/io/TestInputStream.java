package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestInputStream extends TestIO.UsingGeneratedTestFiles {

	public TestInputStream(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	protected abstract InputStream openStream();

	@Test(timeout=120000)
	public void testByFullBuffer() throws Exception {
		InputStream in = openStream();
		byte[] buf = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			int nb = in.read(buf);
			Assert.assertTrue(nb > 0);
			while (nb < testBuf.length) {
				int n = in.read(buf, nb, testBuf.length - nb);
				Assert.assertTrue(n > 0);
				nb += n;
			}
			Assert.assertArrayEquals(testBuf, buf);
		}
		int nb = in.read(buf);
		Assert.assertTrue(nb <= 0);
		in.close();
	}

}
