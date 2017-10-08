package net.lecousin.framework.core.tests.io;

import java.io.File;

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
			int nb = s.readFully(buf, 0, buf.length);
			if (nb != testBuf.length)
				throw new Exception("" + nb + " characters read, expected is " + testBuf.length);
			for (int j = 0; j < testBuf.length; ++j)
				if (buf[j] != (testBuf[j] & 0xFF))
					throw new Exception("Invalid character "+buf[j]+" at "+(i*testBuf.length+j));
		}
		s.close();
	}

	// TODO test read
	// TODO test endReached
	
}
