package net.lecousin.framework.core.test.text;

import net.lecousin.framework.text.ArrayString;
import net.lecousin.framework.text.ArrayStringBuffer;
import net.lecousin.framework.text.CharArrayString;
import net.lecousin.framework.text.CharArrayStringBuffer;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestArrayStringBuffer<TS extends ArrayString, T extends ArrayStringBuffer<TS, T>> extends TestIString {

	@Override
	protected abstract T createString(String s);
	
	protected abstract TS createSimpleString(String s);
	
	@Test
	public void testModifications() {
		T s = createString("Hello");
		s.addFirst(' ');
		Assert.assertEquals(" Hello", s.asString());
		s.addFirst("World");
		Assert.assertEquals("World Hello", s.asString());
		Assert.assertEquals("Worxxd Hexxxxo", s.replace('l', "xx").asString());
		Assert.assertEquals("Worxxyzyexxxxo", s.replace(5, 7, new CharArrayStringBuffer("yzy")).asString());
		
		s = createString("");
		s.append(new char[] { 'a', 'b' }, 0, 2);
		Assert.assertEquals("ab", s.asString());

		s = createString("");
		s.append(new CharArrayString("abcd"));
		s.append(new char[] { 'a', 'b' }, 0, 2);
		Assert.assertEquals("abcdab", s.asString());

		s = createString("");
		s.addFirst(new CharArrayString("abc"));
		Assert.assertEquals("abc", s.asString());
		
		s = createString("");
		Assert.assertEquals(0, s.charAt(10));
		Assert.assertEquals(0, s.fillIso8859Bytes(new byte[10], 0));
		Assert.assertEquals(0, s.substring(1).length());
		Assert.assertEquals(0, s.substring(1, 2).length());
		Assert.assertTrue(s == s.removeEndChars(10));
		Assert.assertTrue(s.isEmpty());
		try {
			s.setCharAt(0, ' ');
			throw new AssertionError("must throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		
		s.append(new CharArrayString("hello"));
		s.append(new CharArrayString(" "));
		s.append(new CharArrayString("world"));
		s.append(new CharArrayString("!"));
		s.removeStartChars(6);
		Assert.assertEquals('w', s.charAt(0));
		Assert.assertEquals(0, s.charAt(10));
		
		s = createString("abcdefg");
		CharSequence cs = createSimpleString("01");
		check("ab01fg", s.replace(2, 4, cs));
		cs = createSimpleString("987654321");
		check("ab987654321g", s.replace(2, 4, cs));
		cs = createString("wxcvbn");
		check("wxcvbn7654321g", s.replace(0, 3, cs));
		check("wxwxcvbnn7654321g", s.replace(2, 4, cs));
		
		s = createString("");
		for (int i = 0; i < 100; ++i)
			s.append("Hello");
		Assert.assertEquals(500, s.length());
		for (int i = 0; i < 100; ++i)
			Assert.assertEquals("Hello", s.substring(i * 5, (i + 1) * 5).toString());
		
		s = createString("");
		for (int i = 0; i < 100; ++i)
			s.append("Hello".toCharArray(), 0, 5);
		Assert.assertEquals(500, s.length());
		for (int i = 0; i < 100; ++i)
			Assert.assertEquals("Hello", s.substring(i * 5, (i + 1) * 5).toString());
	}
	
}
