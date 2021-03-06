package net.lecousin.framework.core.tests.io.text;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestCharacterStreamReadableBuffered;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.BufferedReadableCharacterStreamLocation;
import net.lecousin.framework.io.text.ICharacterStream;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestBufferedReadableCharacterStreamLocationBuffered extends TestCharacterStreamReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(false);
	}
	
	public TestBufferedReadableCharacterStreamLocationBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	@SuppressWarnings("resource")
	@Override
	protected ICharacterStream.Readable.Buffered openStream(IO.Readable io) {
		return new BufferedReadableCharacterStreamLocation(new BufferedReadableCharacterStream(io, Charset.forName("UTF-8"), 4096, 4));
	}
	
}
