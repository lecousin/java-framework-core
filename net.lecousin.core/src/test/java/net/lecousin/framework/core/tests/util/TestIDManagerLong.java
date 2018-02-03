package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.encoding.HexaDecimalEncoder;
import net.lecousin.framework.util.IDManagerLong;

import org.junit.Assert;
import org.junit.Test;

public class TestIDManagerLong extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		IDManagerLong idm = new IDManagerLong(new HexaDecimalEncoder());
		Assert.assertEquals("0100000000000000", idm.allocate());
		Assert.assertEquals("0200000000000000", idm.allocate());
		Assert.assertEquals("0300000000000000", idm.allocate());
		idm.free("0200000000000000");
		Assert.assertEquals("0200000000000000", idm.allocate());
		idm.free("0100000000000000");
		idm.free("0300000000000000");
		Assert.assertEquals("0100000000000000", idm.allocate());
		Assert.assertEquals("0300000000000000", idm.allocate());
		Assert.assertEquals("0400000000000000", idm.allocate());
	}
	
}
