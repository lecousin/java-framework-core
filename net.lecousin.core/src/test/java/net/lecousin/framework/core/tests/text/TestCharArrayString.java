package net.lecousin.framework.core.tests.text;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.core.test.text.TestArrayString;
import net.lecousin.framework.text.ArrayString;
import net.lecousin.framework.text.CharArrayString;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestCharArrayString extends TestArrayString {

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
	
	public TestCharArrayString(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	protected int start;
	protected int end;
	
	@Override
	protected ArrayString createString(String s) {
		char[] chars = new char[s.length() + start + end];
		char[] c = s.toCharArray();
		for (int i = 0; i < c.length; ++i)
			chars[i + start] = c[i];
		return new CharArrayString(chars, start, c.length, c.length + end);
	}
	
	@Test
	public void testConstructor() {
		CharArrayString s1 = new CharArrayString(5);
		s1.append("Hello").append(' ').append("World").append('!');
		CharArrayString s2 = new CharArrayString(s1);
		s1.append("!!!");
		Assert.assertEquals("Hello World!", s2.asString());
	}
	
	@Test
	public void test() throws CharacterCodingException {
		CharArrayString s = new CharArrayString("Hello");
		/*ByteBuffer bb = */s.encode(StandardCharsets.UTF_8);
		// TODO check
	}
	
}
