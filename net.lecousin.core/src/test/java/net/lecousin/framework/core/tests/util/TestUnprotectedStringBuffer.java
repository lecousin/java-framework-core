package net.lecousin.framework.core.tests.util;

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.util.TestIString;
import net.lecousin.framework.util.IString;
import net.lecousin.framework.util.UnprotectedString;
import net.lecousin.framework.util.UnprotectedStringBuffer;

public class TestUnprotectedStringBuffer extends TestIString {

	@Override
	protected IString createString(String s) {
		return new UnprotectedStringBuffer(s);
	}
	
	@Test(timeout=120000)
	public void testModifications() {
		UnprotectedStringBuffer s = new UnprotectedStringBuffer(new StringBuilder("Hello"));
		s.addFirst(' ');
		Assert.assertEquals(" Hello", s.asString());
		s.addFirst("World");
		Assert.assertEquals("World Hello", s.asString());
		Assert.assertEquals("Worxxd Hexxxxo", s.replace('l', "xx").asString());
		Assert.assertEquals("Worxxyzyexxxxo", s.replace(5, 7, new UnprotectedStringBuffer("yzy")).asString());
		Function<UnprotectedStringBuffer, UnprotectedStringBuffer> provider = value -> new UnprotectedStringBuffer(Integer.toString(Integer.parseInt(value.asString()) * 2));
		s = new UnprotectedStringBuffer(new UnprotectedStringBuffer("abcd${123}efgh${200}ijk"));
		s.searchAndReplace("${", "}", provider);
		Assert.assertEquals("abcd246efgh400ijk", s.asString());
		s = new UnprotectedStringBuffer("${34}abcd${123}efgh${200}ijk${99}");
		s.searchAndReplace("${", "}", provider);
		Assert.assertEquals("68abcd246efgh400ijk198", s.asString());
		s = new UnprotectedStringBuffer("${3");
		s.searchAndReplace("${", "}", provider);
		Assert.assertEquals("${3", s.asString());
		
		s = new UnprotectedStringBuffer();
		s.append(new char[] { 'a', 'b' }, 0, 2);
		Assert.assertEquals("ab", s.asString());

		s = new UnprotectedStringBuffer();
		s.append(new UnprotectedString("abcd"));
		s.append(new char[] { 'a', 'b' }, 0, 2);
		Assert.assertEquals("abcdab", s.asString());

		s = new UnprotectedStringBuffer();
		s.addFirst(new UnprotectedString("abc"));
		Assert.assertEquals("abc", s.asString());
		
		s = new UnprotectedStringBuffer();
		Assert.assertEquals(0, s.getNbUsableUnprotectedStrings());
		Assert.assertEquals(0, s.charAt(10));
		Assert.assertEquals(0, s.fillUsAsciiBytes(new byte[10], 0));
		Assert.assertTrue(s == s.substring(1));
		Assert.assertTrue(s == s.substring(1, 2));
		Assert.assertTrue(s == s.removeEndChars(10));
		try {
			s.setCharAt(0, ' ');
			throw new AssertionError("must throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		
		s.append(new UnprotectedString("hello"));
		s.append(new UnprotectedString(" "));
		s.append(new UnprotectedString("world"));
		s.append(new UnprotectedString("!"));
		s.removeStartChars(6);
		Assert.assertEquals('w', s.charAt(0));
		Assert.assertEquals(0, s.charAt(10));
	}
	
}
