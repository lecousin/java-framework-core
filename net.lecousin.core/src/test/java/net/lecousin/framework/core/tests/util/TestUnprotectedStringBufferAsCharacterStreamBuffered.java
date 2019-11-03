package net.lecousin.framework.core.tests.util;

import java.io.File;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestCharacterStreamReadableBuffered;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.util.UnprotectedString;
import net.lecousin.framework.util.UnprotectedStringBuffer;

import org.junit.Assume;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestUnprotectedStringBufferAsCharacterStreamBuffered extends TestCharacterStreamReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestUnprotectedStringBufferAsCharacterStreamBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	@Override
	protected ICharacterStream.Readable.Buffered openStream(IO.Readable io) throws Exception {
		io.closeAsync();
		UnprotectedStringBuffer s = new UnprotectedStringBuffer();
		char[] chars = new char[testBuf.length];
		for (int i = 0; i < testBuf.length; ++i)
			chars[i] = (char)(testBuf[i] & 0xFF);
		for (int i = 0; i < nbBuf; ++i) {
			s.append(new UnprotectedString(chars, 0, chars.length, chars.length));
		}
		return s.asCharacterStream();
	}
	
	@Override
	public void testIOError() throws Exception {
		Assume.assumeFalse(true);
	}
	
}
