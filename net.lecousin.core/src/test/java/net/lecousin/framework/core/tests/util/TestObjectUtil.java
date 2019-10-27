package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.ObjectUtil;

import org.junit.Assert;
import org.junit.Test;

public class TestObjectUtil extends LCCoreAbstractTest {

	@Test
	public void test() {
		Assert.assertTrue(ObjectUtil.equalsOrNull(null, null));
		Assert.assertFalse(ObjectUtil.equalsOrNull(new Object(), null));
		Assert.assertFalse(ObjectUtil.equalsOrNull(null, new Object()));
		Assert.assertTrue(ObjectUtil.equalsOrNull(Integer.valueOf(1), Integer.valueOf(1)));
		Assert.assertFalse(ObjectUtil.equalsOrNull(Integer.valueOf(1), Integer.valueOf(2)));
		
		Assert.assertEquals("null", ObjectUtil.toString(null));
		Assert.assertEquals("1", ObjectUtil.toString(Integer.valueOf(1)));
	}
	
}
