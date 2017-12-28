package net.lecousin.framework.core.test.io;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestCharacterStreamReadableBuffered extends TestIO.UsingGeneratedTestFiles {

	protected TestCharacterStreamReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	protected abstract ICharacterStream.Readable.Buffered openStream(IO.Readable io) throws Exception;
	
	@Override
	protected IO getIOForCommonTests() {
		return openFile();
	}

	@SuppressWarnings({ "resource" })
	@Test(timeout=120000)
	public void testCharByChar() throws Exception {
		ICharacterStream.Readable.Buffered s = openStream(openFile());
		for (int i = 0; i < nbBuf; ++i) {
			for (int j = 0; j < testBuf.length; ++j) {
				char c;
				try { c = s.read(); }
				catch (IOException e) {
					throw new Exception("Read character error at " + (i*testBuf.length+j) + " in file " + testFile.getAbsolutePath(), e);
				}
				if (c != (char)(testBuf[j]&0xFF))
					throw new Exception("Invalid character "+c+" at "+(i*testBuf.length+j));
			}
		}
		try {
			s.read();
			throw new AssertionError("Can read after the end of stream");
		} catch (EOFException e) {}
		s.back('w');
		Assert.assertEquals('w', s.read());
		s.back('z');
		char[] buf = new char[20];
		Assert.assertEquals(1, s.readSync(buf, 0, 10));
		Assert.assertEquals('z', buf[0]);
		s.close();
	}

}
