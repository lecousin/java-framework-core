package net.lecousin.framework.core.tests.util;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestCharacterStreamReadable;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.util.UnprotectedStringBuffer;

import org.junit.Assume;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestUnprotectedStringBufferAsCharacterStream extends TestCharacterStreamReadable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(false);
	}
	
	public TestUnprotectedStringBufferAsCharacterStream(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	@Override
	protected ICharacterStream.Readable openStream(IO.Readable io) throws Exception {
		UnprotectedStringBuffer s = new UnprotectedStringBuffer();
		byte[] buf = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			io.readFullySync(ByteBuffer.wrap(buf));
			s.append(new String(buf, 0, buf.length));
		}
		io.close();
		return s.asCharacterStream();
	}
	
	@Override
	public void testIOError() throws Exception {
		Assume.assumeFalse(true);
	}

}
