package net.lecousin.framework.core.tests.memory;

import java.util.Collection;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.memory.Cache;
import net.lecousin.framework.memory.CacheManager.CachedData;
import net.lecousin.framework.memory.IMemoryManageable.FreeMemoryLevel;
import net.lecousin.framework.util.CloseableListenable;

import org.junit.Assert;
import org.junit.Test;

public class TestCache extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		Cache<String, Integer> cache = new Cache<>("Test cache", (integer) -> {
			
		});
		// non-existing
		Assert.assertNull(cache.get("test", null));
		Assert.assertEquals(0, cache.getCachedData().size());
		// set a value with null user
		cache.put("test", Integer.valueOf(1), null);
		Assert.assertEquals(1, cache.getCachedData().size());
		Assert.assertEquals(1, cache.getCachedData().iterator().next().cachedDataCurrentUsage());
		Assert.assertTrue(cache.getCachedData().iterator().next().cachedDataLastUsage() > 0);
		// use it with a second null user
		Assert.assertEquals(1, cache.get("test", null).intValue());
		Assert.assertEquals(1, cache.getCachedData().size());
		Assert.assertEquals(2, cache.getCachedData().iterator().next().cachedDataCurrentUsage());
		// free for second null user
		cache.free(Integer.valueOf(1), null);
		Assert.assertEquals(1, cache.getCachedData().size());
		Assert.assertEquals(1, cache.getCachedData().iterator().next().cachedDataCurrentUsage());
		// use it with a user
		CloseableListenable user = new CloseableListenable.Impl();
		user.isClosed();
		Runnable listener = () -> {};
		user.addCloseListener(listener);
		user.removeCloseListener(listener);
		Assert.assertEquals(1, cache.get("test", user).intValue());
		Assert.assertEquals(1, cache.getCachedData().size());
		Assert.assertEquals(2, cache.getCachedData().iterator().next().cachedDataCurrentUsage());
		// free for first null user
		cache.free(Integer.valueOf(1), null);
		Assert.assertEquals(1, cache.getCachedData().size());
		Assert.assertEquals(1, cache.getCachedData().iterator().next().cachedDataCurrentUsage());
		// close user
		user.close();
		Collection<? extends CachedData> col = cache.getCachedData();
		Assert.assertTrue(col.isEmpty() || col.iterator().next().cachedDataCurrentUsage() == 0);
		
		cache.put("test2", Integer.valueOf(2), null);
		col = cache.getCachedData();
		cache.free(col.iterator().next());
		
		cache.getItemsDescription();
		cache.freeMemory(FreeMemoryLevel.EXPIRED_ONLY);
		cache.freeMemory(FreeMemoryLevel.LOW);
		cache.freeMemory(FreeMemoryLevel.MEDIUM);
		cache.freeMemory(FreeMemoryLevel.URGENT);
		
		// close
		cache.close();
	}
	
}
