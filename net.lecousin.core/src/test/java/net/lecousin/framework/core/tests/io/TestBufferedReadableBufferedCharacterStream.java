package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestCharacterStreamReadableBuffered;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.ICharacterStream;

@RunWith(Parameterized.class)
public class TestBufferedReadableBufferedCharacterStream extends TestCharacterStreamReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestBufferedReadableBufferedCharacterStream(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	@Override
	protected ICharacterStream.Readable.Buffered openStream(IO.Readable io) {
		return new BufferedReadableCharacterStream(io, Charset.forName("UTF-8"), 4096, 4);
	}
	
}
