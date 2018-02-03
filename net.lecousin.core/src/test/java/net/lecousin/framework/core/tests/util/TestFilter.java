package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.Filter;

import org.junit.Assert;
import org.junit.Test;

public class TestFilter extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		Filter.Single<Integer> filter = new Filter.Single<>(Integer.valueOf(51));
		Assert.assertTrue(filter.accept(Integer.valueOf(51)));
		Assert.assertFalse(filter.accept(Integer.valueOf(50)));
		Assert.assertFalse(filter.accept(Integer.valueOf(-51)));
	}
	
}
