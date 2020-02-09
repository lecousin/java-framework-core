package net.lecousin.framework.core.test.io.data;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.text.CharArrayString;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public abstract class TestCharsReadable extends TestDataBuffer<Chars.Readable, char[]> {

	@SuppressWarnings("boxing")
	@Parameters(name = "pos = {1} length = {2} subBuffer = {3}")
	public static Collection<Object[]> parameters() {
		Collection<Object[]> tests = new LinkedList<>(Arrays.asList(
			new Object[] { new char[] { 1, 8, 6, 18, 24, 76, 41, 92, 53, 44, 0, 63, 123, (char)255, (char)140, 15, (char)217 }, 0, 17 },
			new Object[] { new char[] { 1, 8, 6, 18, 24, 76, 41, 92, 53, 44, 0, 63, 123, (char)255, (char)140, 15, (char)217 }, 5, 6 }
		));
		char[] b = new char[2000];
		for (int i = 0; i < b.length; ++i)
			b[i] = (char)i;
		tests.add(new Object[] { b, 159, b.length - 753 });
		tests = addTestParameter(tests, Boolean.FALSE, Boolean.TRUE);
		return tests;
	}
	
	protected TestCharsReadable(Chars.Readable bytes, char[] data, int initialPos, int length, boolean useSubBuffer) {
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
			Assert.assertEquals("char " + nb, data[initialPos + nb], buffer.get());
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
			char[] b = new char[len + 10];
			buffer.get(b, 3, len);
			for (int j = 0; j < len; ++j)
				Assert.assertEquals(data[initialPos + i + j], b[3 + j]);
			Assert.assertEquals(i + len, buffer.position());
			Assert.assertEquals(length - i - len, buffer.remaining());
		}
		buffer.setPosition(0);
		char[] b = new char[length + 10];
		buffer.get(b, 1, length);
		for (int j = 0; j < length; ++j)
			Assert.assertEquals(data[initialPos + j], b[1 + j]);
		Assert.assertEquals(length, buffer.position());
		Assert.assertFalse(buffer.hasRemaining());
	}
	
	@Test
	public void testGetIString() {
		for (int i = 0; i < buffer.length(); ++i) {
			buffer.setPosition(i);
			int len = Math.min(buffer.length() - i, 10);
			char[] b = new char[len + 10];
			CharArrayString s = new CharArrayString(b, 3, 0, len + 7);
			buffer.get(s, len);
			for (int j = 0; j < len; ++j)
				Assert.assertEquals("pos " + i + " char " + j, data[initialPos + i + j], b[3 + j]);
			Assert.assertEquals(i + len, buffer.position());
			Assert.assertEquals(length - i - len, buffer.remaining());
		}
		buffer.setPosition(0);
		char[] b = new char[length + 10];
		CharArrayString s = new CharArrayString(b, 1, 0, b.length);
		buffer.get(s, length);
		for (int j = 0; j < length; ++j)
			Assert.assertEquals(data[initialPos + j], b[1 + j]);
		Assert.assertEquals(length, buffer.position());
		Assert.assertFalse(buffer.hasRemaining());
	}
	
	@Test
	public void testToCharBuffer() {
		CharBuffer b = buffer.toCharBuffer();
		int nb = 0;
		while (b.hasRemaining()) {
			Assert.assertEquals("char " + nb, data[initialPos + nb], b.get());
			nb++;
		}
		Assert.assertEquals(length, nb);
	}
	
}
