package net.lecousin.framework.core.test.text;

import java.util.List;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.text.IString;
import net.lecousin.framework.text.CharArrayString;
import net.lecousin.framework.text.CharArrayStringBuffer;

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
		s.append(new CharArrayString("abcdefghijklmnopqrstuvwxyz"), 7, 11);
		check("Hello World!!!hijk", s);
		s.append(new CharArrayString("0123"), 2, 4);
		check("Hello World!!!hijk23", s);
		s.append(new CharArrayString("9876"), 0, 2);
		check("Hello World!!!hijk2398", s);
		s.append("abcdefghijklmnopqrstuvwxyz", 7, 11);
		check("Hello World!!!hijk2398hijk", s);
		s.append("0123", 2, 4);
		check("Hello World!!!hijk2398hijk23", s);
		s.append("9876", 0, 2);
		check("Hello World!!!hijk2398hijk2398", s);
		s.append(new CharArrayStringBuffer("abcdefghijklmnopqrstuvwxyz"), 7, 11);
		check("Hello World!!!hijk2398hijk2398hijk", s);
		s.append(new CharArrayStringBuffer("0123"), 2, 4);
		check("Hello World!!!hijk2398hijk2398hijk23", s);
		s.append(new CharArrayStringBuffer("9876"), 0, 2);
		check("Hello World!!!hijk2398hijk2398hijk2398", s);
		s.append("-------", 3, 3);
		check("Hello World!!!hijk2398hijk2398hijk2398", s);
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
		Assert.assertEquals(-1, s.indexOf("od", 7));
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
		
		Assert.assertEquals("World", createString("Hello World!").subSequence(6, 11).toString());
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
		
		s = createString("");
		check("", s.replace("ab", "cd"));
		s = createString("Hello World!");
		check("Helisa World!", s.replace("lo", "isa"));
		check("Helisa .rld!", s.replace("Wo", "."));
		check("Elisa .rld!", s.replace("Hel", "El"));
		check("Elisa .rabcdefgh", s.replace("ld!", "abcdefgh"));
		s = createString("abcd012abcd012abcd012");
		check("abcz92abcz92abcz92", s.replace("d01", "z9"));
		check("ab8765492ab8765492ab8765492", s.replace("cz", "87654"));
		
		s = createString("Hello World!");
		check("Helbonjourorld!", s.replace(3, 6, "bonjour"));
		check("Hel1ourorld!", s.replace(3, 6, "1"));
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
		Assert.assertTrue(createString("Hel").isStartOf("Hello World"));
		Assert.assertTrue(createString("Hello World").isStartOf("Hello World"));
		Assert.assertTrue(createString("H").isStartOf("Hello World"));
		Assert.assertTrue(createString("").isStartOf("Hello World"));
		Assert.assertFalse(createString("ello").isStartOf("Hello World"));
	}
	
	@Test
	public void testAsCharacters() {
		char[][] chars = createString("Hello").append(' ').append("World").append('!').asCharacters();
		String s = new String("Hello World!");
		int pos = 0;
		for (int i = 0; i < chars.length; ++i)
			for (int j = 0; j < chars[i].length; ++j)
				Assert.assertEquals(s.charAt(pos++), chars[i][j]);
		Assert.assertEquals(s.length(), pos);
	}
	
	@Test
	public void testTrim() {
		Assert.assertEquals("Hello", createString("Hello").trim().asString());
		Assert.assertEquals("Hello", createString(" Hello").trim().asString());
		Assert.assertEquals("Hello", createString("Hello ").trim().asString());
		Assert.assertEquals("Hello", createString("  Hello").trim().asString());
		Assert.assertEquals("Hello", createString("Hello  ").trim().asString());
		Assert.assertEquals("Hello", createString("  Hello  ").trim().asString());
		Assert.assertEquals("Hello", createString("\rHello").trim().asString());
		Assert.assertEquals("Hello", createString("\nHello").trim().asString());
		Assert.assertEquals("Hello", createString("\tHello").trim().asString());
		Assert.assertEquals("Hello", createString("Hello\r").trim().asString());
		Assert.assertEquals("Hello", createString("Hello\n").trim().asString());
		Assert.assertEquals("Hello", createString("Hello\t").trim().asString());
		Assert.assertEquals("Hello", createString(" \r\n\tHello").trim().asString());
		Assert.assertEquals("Hello", createString("Hello \r\n\t").trim().asString());
		Assert.assertEquals("Hello", createString("\n\t\r Hello\r \t \n").trim().asString());
	}
	
	@Test
	public void testToUsAsciiBytes() {
		Assert.assertArrayEquals(new byte[] { 'H', 'e', 'l', 'l', 'o' }, createString("Hello").toIso8859Bytes());
		byte[] bytes = new byte[5];
		createString("He").append("ll").append('o').fillIso8859Bytes(bytes);
		Assert.assertArrayEquals(new byte[] { 'H', 'e', 'l', 'l', 'o' }, bytes);
	}
	
	@Test
	public void testCopy() {
		IString s = createString("abcdefgh");
		IString copy = s.copy();
		Assert.assertEquals("abcdefgh", copy.toString());
		s.append('z');
		Assert.assertEquals("abcdefgh", copy.toString());
	}
}
