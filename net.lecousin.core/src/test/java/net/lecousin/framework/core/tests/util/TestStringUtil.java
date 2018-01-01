package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
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
	public void testHexa() {
		Assert.assertEquals('0', StringUtil.encodeHexaDigit(0));
		Assert.assertEquals('1', StringUtil.encodeHexaDigit(1));
		Assert.assertEquals('2', StringUtil.encodeHexaDigit(2));
		Assert.assertEquals('3', StringUtil.encodeHexaDigit(3));
		Assert.assertEquals('4', StringUtil.encodeHexaDigit(4));
		Assert.assertEquals('5', StringUtil.encodeHexaDigit(5));
		Assert.assertEquals('6', StringUtil.encodeHexaDigit(6));
		Assert.assertEquals('7', StringUtil.encodeHexaDigit(7));
		Assert.assertEquals('8', StringUtil.encodeHexaDigit(8));
		Assert.assertEquals('9', StringUtil.encodeHexaDigit(9));
		Assert.assertEquals('A', StringUtil.encodeHexaDigit(10));
		Assert.assertEquals('B', StringUtil.encodeHexaDigit(11));
		Assert.assertEquals('C', StringUtil.encodeHexaDigit(12));
		Assert.assertEquals('D', StringUtil.encodeHexaDigit(13));
		Assert.assertEquals('E', StringUtil.encodeHexaDigit(14));
		Assert.assertEquals('F', StringUtil.encodeHexaDigit(15));
		
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
		
		Assert.assertEquals(0, StringUtil.decodeHexa('0'));
		Assert.assertEquals(1, StringUtil.decodeHexa('1'));
		Assert.assertEquals(2, StringUtil.decodeHexa('2'));
		Assert.assertEquals(3, StringUtil.decodeHexa('3'));
		Assert.assertEquals(4, StringUtil.decodeHexa('4'));
		Assert.assertEquals(5, StringUtil.decodeHexa('5'));
		Assert.assertEquals(6, StringUtil.decodeHexa('6'));
		Assert.assertEquals(7, StringUtil.decodeHexa('7'));
		Assert.assertEquals(8, StringUtil.decodeHexa('8'));
		Assert.assertEquals(9, StringUtil.decodeHexa('9'));
		Assert.assertEquals(10, StringUtil.decodeHexa('A'));
		Assert.assertEquals(11, StringUtil.decodeHexa('B'));
		Assert.assertEquals(12, StringUtil.decodeHexa('C'));
		Assert.assertEquals(13, StringUtil.decodeHexa('D'));
		Assert.assertEquals(14, StringUtil.decodeHexa('E'));
		Assert.assertEquals(15, StringUtil.decodeHexa('F'));
		Assert.assertEquals(10, StringUtil.decodeHexa('a'));
		Assert.assertEquals(11, StringUtil.decodeHexa('b'));
		Assert.assertEquals(12, StringUtil.decodeHexa('c'));
		Assert.assertEquals(13, StringUtil.decodeHexa('d'));
		Assert.assertEquals(14, StringUtil.decodeHexa('e'));
		Assert.assertEquals(15, StringUtil.decodeHexa('f'));
		
		Assert.assertEquals(0x00, StringUtil.decodeHexaByte("00"));
		Assert.assertEquals(0x01, StringUtil.decodeHexaByte("01"));
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
	
}
