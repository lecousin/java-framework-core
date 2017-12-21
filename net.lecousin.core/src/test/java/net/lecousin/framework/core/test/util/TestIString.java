package net.lecousin.framework.core.test.util;

import java.util.List;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.IString;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestIString extends LCCoreAbstractTest {

	protected abstract IString createString(String s);
	
	protected void check(String expected, IString found) {
		Assert.assertEquals(expected, found.asString());
		Assert.assertTrue(createString(expected).equals(found));
		Assert.assertTrue(found.equals(expected));
	}
	
	@Test
	public void testString() {
		check("Hello", createString("Hello"));
		check("", createString(""));
	}
	
	@Test
	public void testSetCharAt() {
		IString s;
		s = createString("");
		try {
			s.setCharAt(0, 'a');
			throw new AssertionError("setCharAt(0) on an empty string must throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		try {
			s.setCharAt(-1, 'a');
			throw new AssertionError("setCharAt(-1) on an empty string must throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		try {
			s.setCharAt(10, 'a');
			throw new AssertionError("setCharAt(10) on an empty string must throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		
		s = createString("Hello");
		check("Hello", s);
		s.setCharAt(0, 'h');
		check("hello", s);
		s.setCharAt(3, 'h');
		check("helho", s);
		try {
			s.setCharAt(10, 'a');
			throw new AssertionError("setCharAt(10) is expected to throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testAppend() {
		IString s;
		s = createString("Hello");
		s.append(' ');
		check("Hello ", s);
		s.append(new char[] { 't', 'W', 'o', 'r', 'l', 'd', 't' }, 1, 5);
		check("Hello World", s);
		s.append("!!!");
		check("Hello World!!!", s);
	}
	
	@Test
	public void testIndexOf() {
		IString s;
		s = createString("");
		Assert.assertEquals(-1, s.indexOf('o'));
		Assert.assertEquals(-1, s.indexOf("or"));
		Assert.assertEquals(-1, s.indexOf('o', 5));
		Assert.assertEquals(-1, s.indexOf("or", 5));
		s = createString("Hello World");
		Assert.assertEquals(4, s.indexOf('o'));
		Assert.assertEquals(7, s.indexOf("or"));
		Assert.assertEquals(7, s.indexOf('o', 5));
		Assert.assertEquals(7, s.indexOf("or", 5));
		Assert.assertEquals(-1, s.indexOf('o', 10));
		Assert.assertEquals(-1, s.indexOf("or", 10));
	}
	
	@Test
	public void testSubstring() {
		IString s;
		s = createString("");
		check("", s.substring(0));
		check("", s.substring(1));
		check("", s.substring(10));
		check("", s.substring(0, 10));
		check("", s.substring(1, 10));
		check("", s.substring(5, 10));
		s = createString("Hello World!");
		check("Hello World!", s.substring(0));
		check("ello World!", s.substring(1));
		check(" World!", s.substring(5));
		check("Hello World!", s.substring(0, 100));
		check("H", s.substring(0, 1));
		check("Hello", s.substring(0, 5));
		check("World", s.substring(6, 11));
		check("World!", s.substring(6, 12));
		check("World!", s.substring(6, 100));
	}
	
	@Test
	public void testReplace() {
		IString s;
		s = createString("");
		check("", s.replace('o', 'a'));
		s = createString("Hello World!");
		check("Hella Warld!", s.replace('o', 'a'));
		check("Hebba Warbd!", s.replace('l', 'b'));
		check("Hebba zarbd!", s.replace('W', 'z'));
		check("Hebba zarbd!", s.replace('W', 'y'));
	}
	
	@Test
	public void testRemove() {
		check("Hello World", createString("Hello World").removeEndChars(0));
		check("Hello Wor", createString("Hello World").removeEndChars(2));
		check("Hello", createString("Hello World").removeEndChars(6));
		check("", createString("Hello World").removeEndChars(11));
		check("", createString("Hello World").removeEndChars(12));

		check("Hello World", createString("Hello World").removeStartChars(0));
		check("llo World", createString("Hello World").removeStartChars(2));
		check("World", createString("Hello World").removeStartChars(6));
		check("", createString("Hello World").removeStartChars(11));
		check("", createString("Hello World").removeStartChars(12));
	}
	
	@Test
	public void testSplit() {
		List<? extends IString> list = createString("Hello World").split('o');
		Assert.assertEquals(3, list.size());
		check("Hell", list.get(0));
		check(" W", list.get(1));
		check("rld", list.get(2));
	}
	
	@Test
	public void testCase() {
		check("HELLO WORLD!", createString("Hello World!").toUpperCase());
		check("hello world!", createString("Hello World!").toLowerCase());
	}
	
	@Test
	public void testStartEndWith() {
		Assert.assertTrue(createString("Hello World").startsWith("Hel"));
		Assert.assertTrue(createString("Hello World").startsWith("Hello World"));
		Assert.assertTrue(createString("Hello World").startsWith("H"));
		Assert.assertTrue(createString("Hello World").startsWith(""));
		Assert.assertFalse(createString("Hello World").startsWith("ello"));
		Assert.assertTrue(createString("Hello World").endsWith("rld"));
		Assert.assertTrue(createString("Hello World").endsWith("Hello World"));
		Assert.assertTrue(createString("Hello World").endsWith("d"));
		Assert.assertTrue(createString("Hello World").endsWith(""));
		Assert.assertFalse(createString("Hello World").endsWith("orl"));
	}
	
}
