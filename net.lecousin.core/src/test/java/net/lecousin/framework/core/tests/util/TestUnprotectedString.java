package net.lecousin.framework.core.tests.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.lecousin.framework.core.test.util.TestIString;
import net.lecousin.framework.util.IString;
import net.lecousin.framework.util.UnprotectedString;
import net.lecousin.framework.util.UnprotectedStringBuffer;

@RunWith(Parameterized.class)
public class TestUnprotectedString extends TestIString {

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
	
	public TestUnprotectedString(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	protected int start;
	protected int end;
	
	@Override
	protected IString createString(String s) {
		char[] chars = new char[s.length() + start + end];
		char[] c = s.toCharArray();
		for (int i = 0; i < c.length; ++i)
			chars[i + start] = c[i];
		return new UnprotectedString(chars, start, c.length, c.length + end);
	}
	
	@Test(timeout=120000)
	public void testConstructor() {
		UnprotectedString s1 = new UnprotectedString(5);
		s1.append("Hello").append(' ').append("World").append('!');
		UnprotectedString s2 = new UnprotectedString(s1);
		s1.append("!!!");
		Assert.assertEquals("Hello World!", s2.asString());
	}
	
	@Test(timeout=120000)
	public void testSimple() {
		UnprotectedString s = new UnprotectedString(0);
		Assert.assertEquals(-1, s.firstChar());
		Assert.assertEquals(-1, s.lastChar());
		
		s.append("Hello").append(' ').append("World").append('!');
		Assert.assertEquals('H', s.firstChar());
		Assert.assertEquals('!', s.lastChar());
		
		s.trimToSize();
		Assert.assertEquals("Hello World!", s.asString());
		Assert.assertEquals(2, s.countChar('o'));
		Assert.assertEquals(3, s.countChar('l'));
		Assert.assertEquals(1, s.countChar('d'));
		Assert.assertEquals(0, s.countChar('x'));
	}
	
	@Override
	@Test(timeout=120000)
	public void testAppend() {
		super.testAppend();
		UnprotectedString s = new UnprotectedString(0);
		UnprotectedString s1 = new UnprotectedString("Hello");
		UnprotectedStringBuffer s2 = new UnprotectedStringBuffer(" World");
		s2.append('!');
		s.append(s1);
		s.append(s2);
		Assert.assertEquals("Hello World!", s.asString());
	}
	
	@Test(timeout=30000)
	public void test() {
		UnprotectedString s = new UnprotectedString("Hello");
		/*ByteBuffer bb = */s.encode(StandardCharsets.UTF_8);
		// TODO check
	}
	
}
