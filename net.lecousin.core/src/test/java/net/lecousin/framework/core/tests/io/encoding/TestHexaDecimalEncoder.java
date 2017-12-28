package net.lecousin.framework.core.tests.io.encoding;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.encoding.HexaDecimalEncoder;

import org.junit.Assert;
import org.junit.Test;

public class TestHexaDecimalEncoder extends LCCoreAbstractTest {

	@Test
	public void test() {
		byte[] src = new byte[] { 0x31, 0x7D, 0x00, 0x05, (byte)0x98, (byte)0xEF, (byte)0xFF, 0x01 };
		HexaDecimalEncoder encoder = new HexaDecimalEncoder();
		byte[] encoded = encoder.encode(src);
		Assert.assertEquals("317D000598EFFF01", new String(encoded));
		byte[] decoded = encoder.decode(encoded);
		Assert.assertArrayEquals(src, decoded);
	}
	
}
