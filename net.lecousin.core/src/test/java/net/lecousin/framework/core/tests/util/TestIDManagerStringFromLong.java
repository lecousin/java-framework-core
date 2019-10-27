package net.lecousin.framework.core.tests.util;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.IDManagerLong;
import net.lecousin.framework.util.IDManagerStringFromLong;
import net.lecousin.framework.util.SimpleIDManagerLong;

public class TestIDManagerStringFromLong extends LCCoreAbstractTest {

	@Test
	public void test() {
		IDManagerLong idml = new SimpleIDManagerLong();
		IDManagerStringFromLong idms = new IDManagerStringFromLong(idml);
		Assert.assertEquals("1", idms.allocate());
		Assert.assertEquals("2", idms.allocate());
		idms.free("1");
		Assert.assertEquals("1", idms.allocate());
		Assert.assertEquals("3", idms.allocate());
		idms.used("5");
		Assert.assertEquals("4", idms.allocate());
		Assert.assertEquals("6", idms.allocate());
	}
	
}
