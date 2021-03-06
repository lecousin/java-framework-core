package net.lecousin.framework.core.test.text;

import java.util.List;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.data.CharArray;
import net.lecousin.framework.text.CharArrayString;
import net.lecousin.framework.text.CharArrayStringBuffer;
import net.lecousin.framework.text.IString;

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
		s.append((CharSequence)null);
		check("Hello World!!!hijk2398hijk2398hijk2398null", s);
		s.append((CharSequence)null, 0, 0);
		check("Hello World!!!hijk2398hijk2398hijk2398nullnull", s);
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
		Assert.assertEquals("", createString("").subSequence(0, 0).toString());
		
		s = createString("");
		for (int i = 0; i < 100; ++i)
			s.append("Hello");
		Assert.assertEquals("el", s.subSequence(491, 493).toString());
		StringBuilder s2 = new StringBuilder();
		for (int i = 0; i < 100; ++i)
			s2.append("Hello");
		Assert.assertTrue(s.equals(s2));
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
		
		s = createString("");
		check("", s.replace('a', "b"));
		check("", s.replace('a', "bb"));
		s = createString("Hello world");
		check("Hepzpzo worpzd", s.replace('l', "pz"));
		
		s = createString("");
		check("", s.replace("a", 'b'));
		check("", s.replace("aa", 'b'));
		s = createString("abcdabcdabcd");
		check("ab0dab0dab0d", s.replace("c", '0'));
		check("ab1ab1ab1", s.replace("0d", '1'));
		check("515151", s.replace("ab", '5'));
		
		s = createString("");
		check("", s.replace('a', new char[] { 'b' }));
		check("", s.replace('a', new char[] { 'b', 'b' }));
		s = createString("Hello world");
		check("Hepzpzo worpzd", s.replace('l', new char[] { 'p', 'z' }));
		
		s = createString("");
		check("", s.replace("abcd", new char[] { 'b' }));
		check("", s.replace("abcd", new char[] { 'b', 'g' }));
		s = createString("abcdabcdabcd");
		check("abzdabzdabzd", s.replace("c", new char[] { 'z' }));
		check("abz012bz012bzd", s.replace("da", new char[] { '0', '1', '2' }));
		check("987z012bz012bzd", s.replace("ab", new char[] { '9', '8', '7' }));
		check("987z012bz012b987", s.replace("zd", new char[] { '9', '8', '7' }));
		
		s = createString("abcdabcdabcd");
		check("abczdabcd", s.replace(3, 6, 'z'));
		check("abc01cd", s.replace(3, 6, new char[] { '0', '1' }));
		check("a987654321001cd", s.replace(1, 2, new char[] { '9', '8', '7', '6', '5', '4', '3', '2', '1', '0' }));
		CharSequence cs = createString("yuiop");
		check("a9yuiop4321001cd", s.replace(2, 5, cs));
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
		
		Assert.assertEquals("", createString("hel").removeStartChars(10).toString());
		Assert.assertEquals("", createString("lo").removeEndChars(10).toString());
		Assert.assertEquals("", createString("").removeStartChars(10).toString());
		Assert.assertEquals("", createString("").removeEndChars(10).toString());
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
		
		IString s = createString("Hello World");
		s.append("Hello World");
		s.append("Hello World");
		s.append("Hello World");
		s.append("Hello World");
		s.append("Hello World");
		Assert.assertTrue(s.startsWith("Hello WorldHello WorldHello WorldHello WorldHello World"));
		Assert.assertFalse(s.startsWith("Hello WorldHello WorldHello WorldHello WorldHello Worlx"));
		Assert.assertFalse(s.startsWith("Hello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello World"));
		Assert.assertTrue(s.endsWith("Hello WorldHello WorldHello WorldHello WorldHello World"));
		Assert.assertFalse(s.endsWith("Hello WorldHello WorldHello WorldHello WorldHello Worlx"));
		Assert.assertFalse(s.endsWith("hello WorldHello WorldHello WorldHello WorldHello World"));
		Assert.assertFalse(s.endsWith("Hello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello World"));
		
		Assert.assertTrue(createString("").startsWith(""));
		Assert.assertTrue(createString("").endsWith(""));
		Assert.assertFalse(createString("").startsWith("a"));
		Assert.assertFalse(createString("").endsWith("a"));
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
		
		Assert.assertEquals(0, createString("").asCharacters().length);
	}
	
	@Test
	public void testAsCharBuffers() {
		CharArray[] arrays;
		arrays = createString("").asCharBuffers();
		check(arrays, "");
		arrays = createString("toto").asCharBuffers();
		check(arrays, "toto");
	}
	
	private static void check(CharArray[] arrays, String expected) {
		StringBuilder s = new StringBuilder();
		for (CharArray a : arrays) {
			while (a.hasRemaining())
				s.append(a.get());
		}
		Assert.assertEquals(expected, s.toString());
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
		
		Assert.assertEquals("", createString("").trim().asString());
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
	
	@Test
	public void testToString() {
		Assert.assertEquals("", createString("").toString());
		Assert.assertEquals("abcdef", createString("abcdef").toString());
		IString s = createString("Hello World");
		s.append("Hello World");
		s.append("Hello World");
		s.append("Hello World");
		s.append("Hello World");
		s.append("Hello World");
		Assert.assertEquals("Hello WorldHello WorldHello WorldHello WorldHello WorldHello World", s.toString());
	}
}
