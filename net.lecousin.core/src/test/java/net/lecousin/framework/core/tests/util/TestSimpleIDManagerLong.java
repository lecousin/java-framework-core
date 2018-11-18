package net.lecousin.framework.core.tests.util;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.SimpleIDManagerLong;

public class TestSimpleIDManagerLong extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		SimpleIDManagerLong idm = new SimpleIDManagerLong();
		Assert.assertEquals(1, idm.allocate());
		Assert.assertEquals(2, idm.allocate());
		Assert.assertEquals(3, idm.allocate());
		idm.free(2);
		Assert.assertEquals(2, idm.allocate());
		idm.free(1);
		idm.free(3);
		Assert.assertEquals(1, idm.allocate());
		Assert.assertEquals(3, idm.allocate());
		Assert.assertEquals(4, idm.allocate());
	}
	
}
