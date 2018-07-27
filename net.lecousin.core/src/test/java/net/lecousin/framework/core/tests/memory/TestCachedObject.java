package net.lecousin.framework.core.tests.memory;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.memory.CachedObject;

public class TestCachedObject extends LCCoreAbstractTest {

	
	@Test(timeout=30000)
	public void test() {
		CachedObject<Integer> co = new CachedObject<Integer>(Integer.valueOf(51), 5000) {
			@Override
			protected void closeCachedObject(Integer object) {
			}
		};
		Assert.assertEquals(51, co.get().intValue());
		Assert.assertEquals(0, co.getUsage());
		Object user = new Object();
		co.use(user);
		Assert.assertEquals(1, co.getUsage());
		co.getLastUsage();
		co.getExpiration();
		co.cachedDataCurrentUsage();
		co.cachedDataLastUsage();
		co.release(user);
		Assert.assertEquals(51, co.get().intValue());
		Assert.assertEquals(0, co.getUsage());
		co.close();
	}
}
