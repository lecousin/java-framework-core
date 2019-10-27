package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;

public abstract class TestInputStream extends TestIO.UsingGeneratedTestFiles {

	public TestInputStream(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	protected abstract InputStream openStream();

	@Test
	public void testBasics() throws Exception {
		InputStream in = openStream();
		in.available();
		if (in.markSupported()) {
			in.mark(1);
			in.reset();
		}
		in.close();
	}
	
	@Test
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
	
	@Test
	public void testByByte() throws Exception {
		Assume.assumeTrue(nbBuf < 500);
		InputStream in = openStream();
		for (int i = 0; i < nbBuf; ++i) {
			for (int j = 0; j < testBuf.length; ++j)
				Assert.assertEquals(testBuf[j] & 0xFF, in.read());
		}
		Assert.assertEquals(-1, in.read());
		in.close();
	}

	@Test
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
