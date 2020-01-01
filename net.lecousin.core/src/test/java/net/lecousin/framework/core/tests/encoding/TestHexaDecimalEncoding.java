package net.lecousin.framework.core.tests.encoding;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import net.lecousin.framework.core.test.encoding.AbstractTestBytesEncoding;
import net.lecousin.framework.encoding.BytesDecoder;
import net.lecousin.framework.encoding.BytesEncoder;
import net.lecousin.framework.encoding.EncodingException;
import net.lecousin.framework.encoding.HexaDecimalEncoding;
import net.lecousin.framework.util.Pair;

import org.junit.Assert;
import org.junit.Test;

public class TestHexaDecimalEncoding extends AbstractTestBytesEncoding {

	@Override
	protected List<Pair<byte[], byte[]>> getTestCases() {
		byte[] toEncode = new byte[128*1024];
		for (int i = 0; i < 1024; ++i) {
			for (int j = 0; j < 128; ++j) {
				toEncode[i * 128 + j] = (byte)((i * j % 300) + i + j - i/3);
			}
		}
		byte[] encoded = DatatypeConverter.printHexBinary(toEncode).getBytes(StandardCharsets.US_ASCII);
		
		return Arrays.asList(
			new Pair<>(new byte[] { 0x31, 0x7D, 0x00, 0x05, (byte)0x98, (byte)0xEF, (byte)0xFF, 0x01 }, "317D000598EFFF01".getBytes(StandardCharsets.US_ASCII)),
			new Pair<>(toEncode, encoded)
		);
	}
	
	@Override
	protected List<Pair<byte[], byte[]>> getErrorTestCases() {
		return Arrays.asList(
			new Pair<>(null, "ABG01".getBytes(StandardCharsets.UTF_8))
		);
	}
	
	@Override
	protected BytesEncoder createEncoder() {
		return HexaDecimalEncoding.instance;
	}
	
	@Override
	protected BytesDecoder createDecoder() {
		return HexaDecimalEncoding.instance;
	}
	@Test
	public void testHexa() throws EncodingException {
		Assert.assertEquals('0', HexaDecimalEncoding.encodeDigit(0));
		Assert.assertEquals('1', HexaDecimalEncoding.encodeDigit(1));
		Assert.assertEquals('2', HexaDecimalEncoding.encodeDigit(2));
		Assert.assertEquals('3', HexaDecimalEncoding.encodeDigit(3));
		Assert.assertEquals('4', HexaDecimalEncoding.encodeDigit(4));
		Assert.assertEquals('5', HexaDecimalEncoding.encodeDigit(5));
		Assert.assertEquals('6', HexaDecimalEncoding.encodeDigit(6));
		Assert.assertEquals('7', HexaDecimalEncoding.encodeDigit(7));
		Assert.assertEquals('8', HexaDecimalEncoding.encodeDigit(8));
		Assert.assertEquals('9', HexaDecimalEncoding.encodeDigit(9));
		Assert.assertEquals('A', HexaDecimalEncoding.encodeDigit(10));
		Assert.assertEquals('B', HexaDecimalEncoding.encodeDigit(11));
		Assert.assertEquals('C', HexaDecimalEncoding.encodeDigit(12));
		Assert.assertEquals('D', HexaDecimalEncoding.encodeDigit(13));
		Assert.assertEquals('E', HexaDecimalEncoding.encodeDigit(14));
		Assert.assertEquals('F', HexaDecimalEncoding.encodeDigit(15));
		
		Assert.assertEquals(0, HexaDecimalEncoding.decodeChar('0'));
		Assert.assertEquals(1, HexaDecimalEncoding.decodeChar('1'));
		Assert.assertEquals(2, HexaDecimalEncoding.decodeChar('2'));
		Assert.assertEquals(3, HexaDecimalEncoding.decodeChar('3'));
		Assert.assertEquals(4, HexaDecimalEncoding.decodeChar('4'));
		Assert.assertEquals(5, HexaDecimalEncoding.decodeChar('5'));
		Assert.assertEquals(6, HexaDecimalEncoding.decodeChar('6'));
		Assert.assertEquals(7, HexaDecimalEncoding.decodeChar('7'));
		Assert.assertEquals(8, HexaDecimalEncoding.decodeChar('8'));
		Assert.assertEquals(9, HexaDecimalEncoding.decodeChar('9'));
		Assert.assertEquals(10, HexaDecimalEncoding.decodeChar('A'));
		Assert.assertEquals(11, HexaDecimalEncoding.decodeChar('B'));
		Assert.assertEquals(12, HexaDecimalEncoding.decodeChar('C'));
		Assert.assertEquals(13, HexaDecimalEncoding.decodeChar('D'));
		Assert.assertEquals(14, HexaDecimalEncoding.decodeChar('E'));
		Assert.assertEquals(15, HexaDecimalEncoding.decodeChar('F'));
		Assert.assertEquals(10, HexaDecimalEncoding.decodeChar('a'));
		Assert.assertEquals(11, HexaDecimalEncoding.decodeChar('b'));
		Assert.assertEquals(12, HexaDecimalEncoding.decodeChar('c'));
		Assert.assertEquals(13, HexaDecimalEncoding.decodeChar('d'));
		Assert.assertEquals(14, HexaDecimalEncoding.decodeChar('e'));
		Assert.assertEquals(15, HexaDecimalEncoding.decodeChar('f'));
		try {
			HexaDecimalEncoding.decodeChar('g');
			throw new AssertionError("Error expected");
		} catch (EncodingException e) {
			// ok
		}
		try {
			HexaDecimalEncoding.decodeChar('G');
			throw new AssertionError("Error expected");
		} catch (EncodingException e) {
			// ok
		}
		try {
			HexaDecimalEncoding.decodeChar(')');
			throw new AssertionError("Error expected");
		} catch (EncodingException e) {
			// ok
		}
		try {
			HexaDecimalEncoding.decodeChar('[');
			throw new AssertionError("Error expected");
		} catch (EncodingException e) {
			// ok
		}
		try {
			HexaDecimalEncoding.decodeChar('?');
			throw new AssertionError("Error expected");
		} catch (EncodingException e) {
			// ok
		}
		
		Assert.assertTrue(HexaDecimalEncoding.isHexaDigit('b'));
		Assert.assertTrue(HexaDecimalEncoding.isHexaDigit('C'));
		Assert.assertTrue(HexaDecimalEncoding.isHexaDigit('6'));
		Assert.assertFalse(HexaDecimalEncoding.isHexaDigit('t'));
		Assert.assertFalse(HexaDecimalEncoding.isHexaDigit('H'));
		Assert.assertFalse(HexaDecimalEncoding.isHexaDigit('('));
		Assert.assertFalse(HexaDecimalEncoding.isHexaDigit(']'));
		Assert.assertFalse(HexaDecimalEncoding.isHexaDigit('?'));
	}

}
