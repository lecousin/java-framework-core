package net.lecousin.framework.core.tests.text;

import java.util.Arrays;
import java.util.Collection;

import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.core.test.text.TestArrayString;
import net.lecousin.framework.text.ArrayString;
import net.lecousin.framework.text.ByteArrayStringIso8859;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestByteArrayStringIso8859 extends TestArrayString {

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
	
	public TestByteArrayStringIso8859(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	protected int start;
	protected int end;
	
	@Override
	protected ArrayString createString(String s) {
		byte[] chars = new byte[s.length() + start + end];
		char[] c = s.toCharArray();
		for (int i = 0; i < c.length; ++i)
			chars[i + start] = (byte)c[i];
		return new ByteArrayStringIso8859(chars, start, c.length, c.length + end);
	}
	
	@Test
	public void testConstructor() {
		ByteArrayStringIso8859 s1 = new ByteArrayStringIso8859(5);
		s1.append("Hello").append(' ').append("World").append('!');
		ByteArrayStringIso8859 s2 = new ByteArrayStringIso8859(s1);
		s1.append("!!!");
		Assert.assertEquals("Hello World!", s2.asString());
	}
	
}
