package net.lecousin.framework.core.test.io.data;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.data.Bytes;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public abstract class TestBytesWritable extends TestDataBuffer<Bytes.Writable, byte[]> {

	@SuppressWarnings("boxing")
	@Parameters
	public static Collection<Object[]> parameters() {
		Collection<Object[]> tests = Arrays.asList(
			new Object[] { 0, 17 },
			new Object[] { 5, 6 }
		);
		tests = addTestParameter(tests, Boolean.FALSE, Boolean.TRUE);
		return tests;
	}
	
	protected TestBytesWritable(Bytes.Writable bytes, int initialPos, int length, boolean useSubBuffer) {
		super(bytes, initialPos, length, null);
		if (useSubBuffer && length > 3) {
			this.buffer = this.buffer.subBuffer(2, length - 3);
			this.initialPos += 2;
			this.length -= 3;
		}
	}
	
	protected abstract void check(byte[] expected, int expectedOffset, int bufferOffset, int len);

	@Test
	public void testPut() {
		byte[] b = new byte[buffer.length()];
		for (int i = 0; i < b.length; ++i) {
			b[i] = (byte)((i + 87) / 3 + 11);
			buffer.put(b[i]);
		}
		Assert.assertEquals(length, buffer.position());
		Assert.assertFalse(buffer.hasRemaining());
		check(b, 0, 0, length);
	}
	
	@Test
	public void testPutBuffer() {
		for (int i = 0; i < buffer.length(); ++i) {
			buffer.setPosition(i);
			int len = Math.min(buffer.length() - i, 10);
			byte[] b = new byte[len + 10];
			for (int j = 0; j < b.length; ++j)
				b[j] = (byte)((i + j + 87) / 3 + 11);
			buffer.put(b, 3, len);
			check(b, 3, i, len);
			Assert.assertEquals(i + len, buffer.position());
			Assert.assertEquals(length - i - len, buffer.remaining());
		}
		buffer.setPosition(0);
		byte[] b = new byte[length + 10];
		for (int i = 0; i < b.length; ++i)
			b[i] = (byte)((i + 87) / 3 + 11);
		buffer.put(b, 1, length);
		check(b, 1, 0, length);
		Assert.assertEquals(length, buffer.position());
		Assert.assertFalse(buffer.hasRemaining());
	}
	
	@Test
	public void testToByteBuffer() {
		try {
			byte[] c = new byte[length];
			for (int i = 0; i < length; ++i)
				c[i] = (byte)(i & 0x75);
			buffer.put(c, 0, length);
			buffer.setPosition(3);
			ByteBuffer b = buffer.toByteBuffer();
			Assert.assertEquals(length - 3, b.remaining());
			for (int i = 3; i < length; ++i)
				Assert.assertEquals(c[i], b.get());
		} catch (UnsupportedOperationException e) {
			// ok
		}
	}
}
