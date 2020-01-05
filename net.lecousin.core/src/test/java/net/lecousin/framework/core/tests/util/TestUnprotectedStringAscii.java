package net.lecousin.framework.core.tests.util;

import java.util.Arrays;
import java.util.Collection;

import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.core.test.util.TestIString;
import net.lecousin.framework.util.IString;
import net.lecousin.framework.util.UnprotectedStringIso8859;
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
		return new UnprotectedStringIso8859(chars, start, c.length, c.length + end);
	}
	
	@Test
	public void testConstructor() {
		UnprotectedStringIso8859 s1 = new UnprotectedStringIso8859(5);
		s1.append("Hello").append(' ').append("World").append('!');
		UnprotectedStringIso8859 s2 = new UnprotectedStringIso8859(s1);
		s1.append("!!!");
		Assert.assertEquals("Hello World!", s2.asString());
	}
	
	@Test
	public void testSimple() {
		UnprotectedStringIso8859 s = new UnprotectedStringIso8859(0);
		Assert.assertEquals(-1, s.firstChar());
		Assert.assertEquals(-1, s.lastChar());
		
		s.append("Hello").append(' ').append("World").append('!');
		Assert.assertEquals('H', s.firstChar());
		Assert.assertEquals('!', s.lastChar());
		
		s.trimToSize();
		Assert.assertEquals("Hello World!", s.asString());
		
		s = new UnprotectedStringIso8859(16);
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
		UnprotectedStringIso8859 s = new UnprotectedStringIso8859(0);
		UnprotectedStringIso8859 s1 = new UnprotectedStringIso8859("Hello");
		UnprotectedStringBuffer s2 = new UnprotectedStringBuffer(" World");
		s2.append('!');
		s.append(s1);
		s.append(s2);
		Assert.assertEquals("Hello World!", s.asString());
	}
	
}
