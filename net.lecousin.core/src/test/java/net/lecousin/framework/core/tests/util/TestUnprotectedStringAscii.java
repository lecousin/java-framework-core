package net.lecousin.framework.core.tests.util;

import java.util.Arrays;
import java.util.Collection;

import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.core.test.util.TestIString;
import net.lecousin.framework.util.IString;
import net.lecousin.framework.util.UnprotectedStringAscii;
import net.lecousin.framework.util.UnprotectedStringBuffer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestUnprotectedStringAscii extends TestIString {

	@SuppressWarnings("boxing")
	@Parameters(name = "start = {0}, end = {1}")
	public static Collection<Object[]> parameters() {
		return Arrays.asList(
			new Object[] { 0, 0 },
			new Object[] { 1, 0 },
			new Object[] { 0, 1 },
			new Object[] { 1, 1 },
			new Object[] { 10, 0 },
			new Object[] { 0, 10 },
			new Object[] { 10, 10 }
		);
	}
	
	public TestUnprotectedStringAscii(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	protected int start;
	protected int end;
	
	@Override
	protected IString createString(String s) {
		byte[] chars = new byte[s.length() + start + end];
		char[] c = s.toCharArray();
		for (int i = 0; i < c.length; ++i)
			chars[i + start] = (byte)c[i];
		return new UnprotectedStringAscii(chars, start, c.length, c.length + end);
	}
	
	@Test
	public void testConstructor() {
		UnprotectedStringAscii s1 = new UnprotectedStringAscii(5);
		s1.append("Hello").append(' ').append("World").append('!');
		UnprotectedStringAscii s2 = new UnprotectedStringAscii(s1);
		s1.append("!!!");
		Assert.assertEquals("Hello World!", s2.asString());
	}
	
	@Test
	public void testSimple() {
		UnprotectedStringAscii s = new UnprotectedStringAscii(0);
		Assert.assertEquals(-1, s.firstChar());
		Assert.assertEquals(-1, s.lastChar());
		
		s.append("Hello").append(' ').append("World").append('!');
		Assert.assertEquals('H', s.firstChar());
		Assert.assertEquals('!', s.lastChar());
		
		s.trimToSize();
		Assert.assertEquals("Hello World!", s.asString());
		
		s = new UnprotectedStringAscii(16);
		s.append('a');
		Assert.assertEquals(1, s.length());
		s.reset();
		Assert.assertEquals(0, s.length());
		s.append("abcdef");
		Assert.assertEquals(6, s.length());
		s.moveForward(2);
		Assert.assertEquals(4, s.length());
		Assert.assertEquals("cdef", s.toString());
	}
	
	@Override
	@Test
	public void testAppend() {
		super.testAppend();
		UnprotectedStringAscii s = new UnprotectedStringAscii(0);
		UnprotectedStringAscii s1 = new UnprotectedStringAscii("Hello");
		UnprotectedStringBuffer s2 = new UnprotectedStringBuffer(" World");
		s2.append('!');
		s.append(s1);
		s.append(s2);
		Assert.assertEquals("Hello World!", s.asString());
	}
	
}
