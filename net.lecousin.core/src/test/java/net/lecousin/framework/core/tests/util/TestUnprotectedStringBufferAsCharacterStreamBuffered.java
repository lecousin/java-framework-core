package net.lecousin.framework.core.tests.util;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestCharacterStreamReadableBuffered;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.util.UnprotectedStringBuffer;

@RunWith(Parameterized.class)
public class TestUnprotectedStringBufferAsCharacterStreamBuffered extends TestCharacterStreamReadableBuffered {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(false);
	}
	
	public TestUnprotectedStringBufferAsCharacterStreamBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	@Override
	protected ICharacterStream.Readable.Buffered openStream(IO.Readable io) throws Exception {
		UnprotectedStringBuffer s = new UnprotectedStringBuffer();
		byte[] buf = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			io.readFullySync(ByteBuffer.wrap(buf));
			s.append(new String(buf, 0, buf.length));
		}
		io.close();
		return s.asCharacterStream();
	}
	
}
