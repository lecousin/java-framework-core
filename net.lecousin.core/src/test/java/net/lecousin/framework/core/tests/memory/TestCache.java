package net.lecousin.framework.core.tests.memory;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.memory.Cache;

import org.junit.Assert;
import org.junit.Test;

public class TestCache extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		Cache<String, Integer> cache = new Cache<>("Test cache", (integer) -> {
			
		});
		// non-existing
		Assert.assertNull(cache.get("test", null));
		Assert.assertEquals(0, cache.getCachedData().size());
		// set a value with null user
		cache.put("test", Integer.valueOf(1), null);
		Assert.assertEquals(1, cache.getCachedData().size());
		Assert.assertEquals(1, cache.getCachedData().iterator().next().cachedDataCurrentUsage());
		// use it with a second null user
		Assert.assertEquals(1, cache.get("test", null).intValue());
		Assert.assertEquals(1, cache.getCachedData().size());
		Assert.assertEquals(2, cache.getCachedData().iterator().next().cachedDataCurrentUsage());
		// free for second null user
		cache.free(Integer.valueOf(1), null);
		Assert.assertEquals(1, cache.getCachedData().size());
		Assert.assertEquals(1, cache.getCachedData().iterator().next().cachedDataCurrentUsage());
		// free for first null user
		cache.free(Integer.valueOf(1), null);
		// close
		cache.close();
	}
	
}
