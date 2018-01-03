package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;

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

	@Test(timeout=120000)
	public void testWithSkip() throws Exception {
		InputStream in = openStream();
		byte[] buf = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			int toSkip = rand.nextInt(testBuf.length - 10);
			Assert.assertEquals(toSkip, in.skip(toSkip));
			int nb = in.read(buf, 0, testBuf.length - toSkip);
			Assert.assertTrue(nb > 0);
			while (nb < testBuf.length - toSkip) {
				int n = in.read(buf, nb, testBuf.length - toSkip - nb);
				Assert.assertTrue(n > 0);
				nb += n;
			}
			Assert.assertTrue(ArrayUtil.equals(testBuf, toSkip, buf, 0, testBuf.length - toSkip));
		}
		int nb = in.read(buf);
		Assert.assertTrue(nb <= 0);
		in.close();
	}
	
}
