package net.lecousin.framework.core.tests.io.encoding;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.encoding.DecimalNumber;

public class TestDecimalNumber extends LCCoreAbstractTest {

	@Test
	public void test() {
		DecimalNumber n = new DecimalNumber();
		Assert.assertEquals(0, n.getNumber());
		n.addChar('3');
		Assert.assertEquals(3, n.getNumber());
		n.addChar('9');
		Assert.assertEquals(39, n.getNumber());
		n.addChar('0');
		Assert.assertEquals(390, n.getNumber());
		n.addChar('1');
		Assert.assertEquals(3901, n.getNumber());
		n.addChar('8');
		Assert.assertEquals(39018, n.getNumber());
		n.addChar('0');
		Assert.assertEquals(390180, n.getNumber());
		Assert.assertFalse(n.addChar('a'));
		Assert.assertEquals(390180, n.getNumber());
		n.reset();
		Assert.assertEquals(0, n.getNumber());
	}
	
}
