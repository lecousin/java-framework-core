package net.lecousin.framework.core.tests.util;

import java.math.BigInteger;
import java.text.ParseException;
import java.util.Locale;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.encoding.EncodingException;
import net.lecousin.framework.util.StringUtil;

import org.junit.Assert;
import org.junit.Test;

public class TestStringUtil extends LCCoreAbstractTest {

	@Test
	public void testPaddingLeft() {
		Assert.assertEquals("xxxHello", StringUtil.paddingLeft(new StringBuilder("Hello"), 8, 'x').toString());
		Assert.assertEquals("Hello", StringUtil.paddingLeft(new StringBuilder("Hello"), 5, 'x').toString());
		Assert.assertEquals("Hello", StringUtil.paddingLeft(new StringBuilder("Hello"), 3, 'x').toString());
		
		Assert.assertEquals("HellotttttWorld", StringUtil.paddingLeft(new StringBuilder("Hello"), "World", 10, 't').toString());
		Assert.assertEquals("HelloWorld", StringUtil.paddingLeft(new StringBuilder("Hello"), "World", 5, 't').toString());
		Assert.assertEquals("HelloWorld", StringUtil.paddingLeft(new StringBuilder("Hello"), "World", 1, 't').toString());
		
		Assert.assertEquals("Test  123", StringUtil.paddingLeft(new StringBuilder("Test"), 123L, 5).toString());
		Assert.assertEquals("Test123", StringUtil.paddingLeft(new StringBuilder("Test"), 123L, 3).toString());
		Assert.assertEquals("Test123", StringUtil.paddingLeft(new StringBuilder("Test"), 123L, 1).toString());
	}

	@Test
	public void testPaddingRight() {
		Assert.assertEquals("Helloxxx", StringUtil.paddingRight(new StringBuilder("Hello"), 8, 'x').toString());
		Assert.assertEquals("Hello", StringUtil.paddingRight(new StringBuilder("Hello"), 5, 'x').toString());
		Assert.assertEquals("Hello", StringUtil.paddingRight(new StringBuilder("Hello"), 3, 'x').toString());
		
		Assert.assertEquals("HelloWorldttttt", StringUtil.paddingRight(new StringBuilder("Hello"), "World", 10, 't').toString());
		Assert.assertEquals("HelloWorld", StringUtil.paddingRight(new StringBuilder("Hello"), "World", 5, 't').toString());
		Assert.assertEquals("HelloWorld", StringUtil.paddingRight(new StringBuilder("Hello"), "World", 1, 't').toString());
		
		Assert.assertEquals("Test123  ", StringUtil.paddingRight(new StringBuilder("Test"), 123L, 5).toString());
		Assert.assertEquals("Test123", StringUtil.paddingRight(new StringBuilder("Test"), 123L, 3).toString());
		Assert.assertEquals("Test123", StringUtil.paddingRight(new StringBuilder("Test"), 123L, 1).toString());
	}
	
	@Test
	public void testHexa() throws EncodingException {
		Assert.assertEquals("00", StringUtil.encodeHexa((byte)0));
		Assert.assertEquals("12", StringUtil.encodeHexa((byte)0x12));
		Assert.assertEquals("7F", StringUtil.encodeHexa((byte)0x7F));
		Assert.assertEquals("8E", StringUtil.encodeHexa((byte)0x8E));
		Assert.assertEquals("AB", StringUtil.encodeHexa((byte)0xAB));
		Assert.assertEquals("FF", StringUtil.encodeHexa((byte)0xFF));
		
		Assert.assertEquals("00346F8DBAFF", StringUtil.encodeHexa(new byte[] { 0, 0x34, 0x6F, (byte)0x8D, (byte)0xBA, (byte)0xFF}));
		
		Assert.assertEquals("0011223344FF", StringUtil.encodeHexa(new byte[] { 1, 2, 3, 5, 0, 0x11, 0x22, 0x33, 0x44, (byte)0xFF, 6, 7, 8}, 4, 6));

		Assert.assertEquals("0000000000000000", StringUtil.encodeHexaPadding(0L));
		Assert.assertEquals("0123456789ABCDEF", StringUtil.encodeHexaPadding(0x123456789ABCDEFL));
		Assert.assertEquals("A123456789ABCDEF", StringUtil.encodeHexaPadding(0xA123456789ABCDEFL));
		
		Assert.assertArrayEquals(new byte[] { 0, 0x34, 0x6F, (byte)0x8D, (byte)0xBA, (byte)0xFF}, StringUtil.decodeHexa("00346F8DBAFF"));
		
		Assert.assertEquals(0x00, StringUtil.decodeHexaByte("00"));
		Assert.assertEquals(0x01, StringUtil.decodeHexaByte("01"));
		Assert.assertEquals(0x03, StringUtil.decodeHexaByte("3"));
		Assert.assertEquals(0x34, StringUtil.decodeHexaByte("34"));
		Assert.assertEquals(0x7F, StringUtil.decodeHexaByte("7F"));
		Assert.assertEquals((byte)0x9A, StringUtil.decodeHexaByte("9A"));
		Assert.assertEquals((byte)0xDB, StringUtil.decodeHexaByte("DB"));
		Assert.assertEquals((byte)0xFF, StringUtil.decodeHexaByte("FF"));
		
		Assert.assertEquals(0x0000000000000000L, StringUtil.decodeHexaLong("0"));
		Assert.assertEquals(0x0000000000000000L, StringUtil.decodeHexaLong("000000"));
		Assert.assertEquals(0x0000000000000001L, StringUtil.decodeHexaLong("0000001"));
		Assert.assertEquals(0x123456789ABCDEF0L, StringUtil.decodeHexaLong("123456789ABCDEF0"));
	}
	
	@Test
	public void testSize() throws ParseException {
		testSize(0, "0");
		testSize(20, "20");
		testSize(1024, "1.00 KB");
		testSize(2048, "2.00 KB");
		testSize(2560, "2.50 KB");
		testSize(2L * 1024 * 1024, "2.00 MB");
		testSize(3L * 1024 * 1024 * 1024, "3.00 GB");
		testSize(4L * 1024 * 1024 * 1024 * 1024, "4.00 TB");
	}
	
	private static void testSize(long size, String str) throws ParseException {
		Locale def = Locale.getDefault();
		Locale.setDefault(Locale.US);
		String s = StringUtil.size(size);
		Assert.assertEquals(str, s);
		s = StringUtil.size(new BigInteger(Long.toString(size)));
		Assert.assertEquals(str, s);
		long si = StringUtil.parseSize(s);
		Assert.assertEquals(size, si);
		Locale.setDefault(def);
	}
	
	public static enum E1 {
		HELLO,
		WORLD;
	}
	
	@Test
	public void otherTests() {
		Assert.assertEquals("HELLO, WORLD", StringUtil.possibleValues(E1.class));
	}
	
}
