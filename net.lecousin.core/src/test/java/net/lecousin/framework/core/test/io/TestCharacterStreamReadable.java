package net.lecousin.framework.core.test.io;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;

public abstract class TestCharacterStreamReadable extends TestIO.UsingGeneratedTestFiles {

	protected TestCharacterStreamReadable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	protected abstract ICharacterStream.Readable openStream(IO.Readable io);
	
	@Override
	protected IO getIOForCommonTests() {
		return openFile();
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testBufferByBufferFully() throws Exception {
		ICharacterStream.Readable s = openStream(openFile());
		char[] buf = new char[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			int nb = s.readFullySync(buf, 0, buf.length);
			if (nb != testBuf.length)
				throw new AssertionError("" + nb + " characters read at buffer " + i + ", expected is " + testBuf.length);
			for (int j = 0; j < testBuf.length; ++j)
				if (buf[j] != (testBuf[j] & 0xFF))
					throw new AssertionError("Invalid character " + buf[j] + " at " + (i * testBuf.length + j));
			if (i < nbBuf - 1)
				Assert.assertFalse(s.endReached());
		}
		Assert.assertTrue(s.readFullySync(buf, 0, buf.length) <= 0);
		Assert.assertTrue(s.endReached());
		s.close();
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testBufferByBuffer() throws Exception {
		ICharacterStream.Readable s = openStream(openFile());
		char[] buf = new char[testBuf.length * 3 - testBuf.length / 10];
		int pos = 0;
		while (pos < nbBuf * testBuf.length) {
			int nb = s.readSync(buf, 0, buf.length);
			if (nb <= 0)
				throw new AssertionError("End of stream reached after " + pos + " characters, expected was "
					+ (nbBuf * testBuf.length));
			for (int i = 0; i < nb; ++i)
				if (buf[i] != testBuf[(pos + i) % testBuf.length])
					throw new AssertionError("Invalid character " + buf[i] + " at " + (pos + i));
			pos += nb;
			if (pos < nbBuf * testBuf.length)
				Assert.assertFalse(s.endReached());
		}
		Assert.assertTrue(s.readFullySync(buf, 0, buf.length) <= 0);
		Assert.assertTrue(s.endReached());
		s.close();
	}

}
