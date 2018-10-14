package net.lecousin.framework.core.tests.io.text;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.io.TestCharacterStreamReadable;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;
import net.lecousin.framework.io.text.Decoder;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.io.text.ProgressiveBufferedReadableCharStream;

@RunWith(Parameterized.class)
public class TestProgressiveBufferedReadableCharStream extends TestCharacterStreamReadable {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases(true);
	}
	
	public TestProgressiveBufferedReadableCharStream(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	@Override
	protected ICharacterStream.Readable openStream(IO.Readable io) throws Exception {
		Decoder d = Decoder.get(StandardCharsets.UTF_8);
		d.setInput(new SimpleBufferedReadable(io, 8192));
		return new ProgressiveBufferedReadableCharStream(d, 4096, 4);
	}
	
}
