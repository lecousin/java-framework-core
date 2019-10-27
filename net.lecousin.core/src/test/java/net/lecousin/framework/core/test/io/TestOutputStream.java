package net.lecousin.framework.core.test.io;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import net.lecousin.framework.io.IO;

public abstract class TestOutputStream extends TestIO.UsingTestData {

	public TestOutputStream(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	protected abstract OutputStream createStream() throws Exception;
	
	protected abstract IO.Readable openReadable(OutputStream out) throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		Assume.assumeTrue(nbBuf < 5000);
		return openReadable(createStream());
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testWriteByFullBuffer() throws Exception {
		OutputStream stream = createStream();
		for (int i = 0; i < nbBuf; ++i)
			stream.write(testBuf);
		stream.flush();
		IO.Readable in = openReadable(stream);
		byte[] buf = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			Assert.assertEquals(testBuf.length, in.readFullySync(ByteBuffer.wrap(buf)));
			Assert.assertArrayEquals(testBuf, buf);
		}
		Assert.assertTrue(in.readFullySync(ByteBuffer.wrap(buf)) <= 0);
		stream.write(51);
		stream.close();
	}

}
