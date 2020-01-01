package net.lecousin.framework.core.tests.encoding.number;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.encoding.number.HexadecimalNumber;

import org.junit.Assert;
import org.junit.Test;

public class TestHexadecimalNumber extends LCCoreAbstractTest {

	@Test
	public void test() {
		HexadecimalNumber n = new HexadecimalNumber();
		Assert.assertEquals(0, n.getNumber());
		n.addChar('3');
		Assert.assertEquals(0x000003, n.getNumber());
		n.addChar('B');
		Assert.assertEquals(0x00003B, n.getNumber());
		n.addChar('F');
		Assert.assertEquals(0x0003BF, n.getNumber());
		n.addChar('Z');
		Assert.assertEquals(0x0003BF, n.getNumber());
		n.addChar(' ');
		Assert.assertEquals(0x0003BF, n.getNumber());
		n.addChar('0');
		Assert.assertEquals(0x003BF0, n.getNumber());
		n.reset();
		Assert.assertEquals(0, n.getNumber());
	}
	
}
