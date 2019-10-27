package net.lecousin.framework.core.tests.util;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.GUIDUtil;

public class TestGUIDUtil extends LCCoreAbstractTest {

	@Test
	public void test() {
		byte[] guid = GUIDUtil.toGUID(12345678, 753, 159, 85246, 987654321);
		String s = GUIDUtil.toString(guid);
		Assert.assertEquals("{00BC614E-02F1-009F-4CFE-00003ADE68B1}", s);
	}
	
}
