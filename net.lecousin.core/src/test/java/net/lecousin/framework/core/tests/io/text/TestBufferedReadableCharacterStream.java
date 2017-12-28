package net.lecousin.framework.core.tests.io.text;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestCharacterStreamReadable;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.ICharacterStream;

@RunWith(Parameterized.class)
public class TestBufferedReadableCharacterStream extends TestCharacterStreamReadable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestBufferedReadableCharacterStream(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	@Override
	protected ICharacterStream.Readable openStream(IO.Readable io) {
		return new BufferedReadableCharacterStream(io, Charset.forName("UTF-8"), 4096, 4);
	}
	
}
