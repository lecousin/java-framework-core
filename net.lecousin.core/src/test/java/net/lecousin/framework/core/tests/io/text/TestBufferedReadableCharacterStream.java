package net.lecousin.framework.core.tests.io.text;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import net.lecousin.framework.core.test.io.TestCharacterStreamReadable;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.data.CharArray;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.ICharacterStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
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
	
	@Test
	public void testWithInitBytesAndChars() throws Exception {
		try (ByteArrayIO io = new ByteArrayIO("This is a text".getBytes(StandardCharsets.US_ASCII), "test");
			BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(io, StandardCharsets.US_ASCII, 16, 2, ByteBuffer.wrap(" World!".getBytes(StandardCharsets.US_ASCII)), new CharArray("Hello".toCharArray()))) {
			char[] buf = new char[1024];
			int nb = stream.readFullySync(buf, 0, buf.length);
			Assert.assertEquals("Hello World!This is a text", new String(buf, 0, nb));
		}
		try (ByteArrayIO io = new ByteArrayIO("This is a text".getBytes(StandardCharsets.US_ASCII), "test");
			BufferedReadableCharacterStream stream = new BufferedReadableCharacterStream(io, StandardCharsets.US_ASCII, 16, 2, ByteBuffer.allocate(0), new CharArray(new char[0]))) {
			char[] buf = new char[1024];
			int nb = stream.readFullySync(buf, 0, buf.length);
			Assert.assertEquals("This is a text", new String(buf, 0, nb));
		}
	}
	
}
