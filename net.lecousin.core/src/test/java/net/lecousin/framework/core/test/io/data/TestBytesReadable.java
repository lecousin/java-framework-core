package net.lecousin.framework.core.test.io.data;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.data.Bytes;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public abstract class TestBytesReadable extends TestDataBuffer<Bytes.Readable, byte[]> {

	@SuppressWarnings("boxing")
	@Parameters
	public static Collection<Object[]> parameters() {
		Collection<Object[]> tests = new LinkedList<>(Arrays.asList(
			new Object[] { new byte[] { 1, 8, 6, 18, 24, 76, 41, 92, 53, 44, 0, 63, 123, (byte)255, (byte)140, 15, (byte)217 }, 0, 17 },
			new Object[] { new byte[] { 1, 8, 6, 18, 24, 76, 41, 92, 53, 44, 0, 63, 123, (byte)255, (byte)140, 15, (byte)217 }, 5, 6 }
		));
		byte[] b = new byte[2000];
		for (int i = 0; i < b.length; ++i)
			b[i] = (byte)i;
		tests.add(new Object[] { b, 159, b.length - 753 });
		tests = addTestParameter(tests, Boolean.FALSE, Boolean.TRUE);
		return tests;
	}
	
	protected TestBytesReadable(Bytes.Readable bytes, byte[] data, int initialPos, int length, boolean useSubBuffer) {
		super(bytes, initialPos, length, data);
		if (useSubBuffer && length > 3) {
			this.buffer = this.buffer.subBuffer(2, length - 3);
			this.initialPos += 2;
			this.length -= 3;
		}
	}
	
	@Test
	public void testGet() {
		int nb = 0;
		while (buffer.hasRemaining()) {
			Assert.assertEquals(data[initialPos + nb], buffer.get());
			nb++;
		}
		Assert.assertEquals(length, nb);
		for (int i = length * 2 / 3; i > 0; i /= 2) {
			buffer.setPosition(i);
			Assert.assertEquals(data[initialPos + i], buffer.get());
		}
	}
	
	@Test
	public void testGetForward() {
		for (int i = 0; i < buffer.length(); ++i)
			Assert.assertEquals(data[initialPos + i], buffer.getForward(i));
		Assert.assertEquals(0, buffer.position());
		Assert.assertEquals(length, buffer.remaining());
	}
	
	@Test
	public void testGetBuffer() {
		for (int i = 0; i < buffer.length(); ++i) {
			buffer.setPosition(i);
			int len = Math.min(buffer.length() - i, 10);
			byte[] b = new byte[len + 10];
			buffer.get(b, 3, len);
			for (int j = 0; j < len; ++j)
				Assert.assertEquals(data[initialPos + i + j], b[3 + j]);
			Assert.assertEquals(i + len, buffer.position());
			Assert.assertEquals(length - i - len, buffer.remaining());
		}
		buffer.setPosition(0);
		byte[] b = new byte[length + 10];
		buffer.get(b, 1, length);
		for (int j = 0; j < length; ++j)
			Assert.assertEquals(data[initialPos + j], b[1 + j]);
		Assert.assertEquals(length, buffer.position());
		Assert.assertFalse(buffer.hasRemaining());
	}
	
	@Test
	public void testToByteBuffer() {
		ByteBuffer b = buffer.toByteBuffer();
		int nb = 0;
		while (b.hasRemaining()) {
			Assert.assertEquals(data[initialPos + nb], b.get());
			nb++;
		}
		Assert.assertEquals(length, nb);
	}
	
}
