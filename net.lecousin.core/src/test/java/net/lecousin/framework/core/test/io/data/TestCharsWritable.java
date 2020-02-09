package net.lecousin.framework.core.test.io.data;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collection;

import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.data.Chars;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public abstract class TestCharsWritable extends TestDataBuffer<Chars.Writable, char[]> {

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
	
	protected TestCharsWritable(Chars.Writable bytes, int initialPos, int length, boolean useSubBuffer) {
		super(bytes, initialPos, length, null);
		if (useSubBuffer && length > 3) {
			this.buffer = this.buffer.subBuffer(2, length - 3);
			this.initialPos += 2;
			this.length -= 3;
		}
	}
	
	protected abstract void check(char[] expected, int expectedOffset, int bufferOffset, int len);

	@Test
	public void testPut() {
		char[] b = new char[buffer.length()];
		for (int i = 0; i < b.length; ++i) {
			b[i] = (char)((i + 87) / 3 + 11);
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
			char[] b = new char[len + 10];
			for (int j = 0; j < b.length; ++j)
				b[j] = (char)((i + j + 87) / 3 + 11);
			buffer.put(b, 3, len);
			check(b, 3, i, len);
			Assert.assertEquals(i + len, buffer.position());
			Assert.assertEquals(length - i - len, buffer.remaining());
		}
		buffer.setPosition(0);
		char[] b = new char[length + 10];
		for (int i = 0; i < b.length; ++i)
			b[i] = (char)((i + 87) / 3 + 11);
		buffer.put(b, 1, length);
		check(b, 1, 0, length);
		Assert.assertEquals(length, buffer.position());
		Assert.assertFalse(buffer.hasRemaining());
	}
	
	@Test
	public void testToCharBuffer() {
		try {
			char[] c = new char[length];
			for (int i = 0; i < length; ++i)
				c[i] = (char)(i & 0x75);
			buffer.put(c, 0, length);
			buffer.setPosition(3);
			CharBuffer b = buffer.toCharBuffer();
			Assert.assertEquals(length - 3, b.remaining());
			for (int i = 3; i < length; ++i)
				Assert.assertEquals(c[i], b.get());
		} catch (UnsupportedOperationException e) {
			// ok
		}
	}

}
