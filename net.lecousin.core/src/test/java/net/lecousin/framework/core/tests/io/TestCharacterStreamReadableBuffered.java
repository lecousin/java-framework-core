package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;

public abstract class TestCharacterStreamReadableBuffered extends TestIO.UsingGeneratedTestFiles {

	protected TestCharacterStreamReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	protected abstract ICharacterStream.Readable.Buffered openStream(IO.Readable io);
	
	@Override
	protected IO getIOForCommonTests() {
		return openFile();
	}

	@SuppressWarnings({ "resource" })
	@Test
	public void testCharByChar() throws Exception {
		ICharacterStream.Readable.Buffered s = openStream(openFile());
		for (int i = 0; i < nbBuf; ++i) {
			for (int j = 0; j < testBuf.length; ++j) {
				char c;
				try { c = s.read(); }
				catch (IOException e) {
					throw new Exception("Read character error at " + (i*testBuf.length+j), e);
				}
				if (c != (char)(testBuf[j]&0xFF))
					throw new Exception("Invalid character "+c+" at "+(i*testBuf.length+j));
			}
		}
		s.close();
	}
	
	// TODO test to use back method

}
