package net.lecousin.framework.core.tests.memory;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.memory.IntArrayCache;
import net.lecousin.framework.memory.IMemoryManageable.FreeMemoryLevel;

public class TestIntArrayCache extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void test() {
		IntArrayCache cache = IntArrayCache.getInstance();
		Assert.assertTrue(cache == IntArrayCache.getInstance());
		// clear content
		cache.freeMemory(FreeMemoryLevel.URGENT);
		// create buffers
		int[] b512 = cache.get(512, false);
		Assert.assertEquals(512, b512.length);
		int[] b1024 = cache.get(1024, false);
		Assert.assertEquals(1024, b1024.length);
		int[] b1200 = cache.get(1200, false);
		Assert.assertEquals(1200, b1200.length);
		int[] b1400 = cache.get(1400, false);
		Assert.assertEquals(1400, b1400.length);
		// free buffers
		cache.free(b512);
		cache.free(b1024);
		cache.free(b1200);
		cache.free(b1400);
		// check we get back the same buffers
		Assert.assertTrue(b512 == cache.get(512, false));
		Assert.assertTrue(b1024 == cache.get(1024, false));
		Assert.assertTrue(b1200 == cache.get(1200, false));
		Assert.assertTrue(b1400 == cache.get(1400, false));
		// free 1024 and 1200
		cache.free(b1024);
		cache.free(b1200);
		// check using acceptGreater
		Assert.assertTrue(b1024 == cache.get(1000, true));
		Assert.assertTrue(b1200 == cache.get(1000, true));
		cache.free(b512);
		cache.free(b1024);
		cache.free(b1200);
		cache.free(b1400);
		// check we get the best size using acceptGreater
		Assert.assertTrue(b1024 == cache.get(1024, true));
		Assert.assertTrue(b1200 == cache.get(1024, true));
		cache.free(b1024);
		cache.free(b1200);
		// check we cannot get greater than 3 / 2 the size
		Assert.assertTrue(cache.get(50, true).length == 50);
		// check if we have 2 available buffers of the same size
		int[] b1024_2 = new int[1024];
		cache.free(b1024_2);
		int[] b = cache.get(1024, true);
		Assert.assertTrue(b == b1024 || b == b1024_2);
		b = cache.get(1024, true);
		Assert.assertTrue(b == b1024 || b == b1024_2);
		cache.free(b1024);
		// free many buffers
		for (int i = 0; i < cache.maxBuffersBySizeUnder128KB + 1; ++i)
			cache.free(new int[8192]);
		for (int i = 0; i < cache.maxBuffersBySizeAbove128KB + 1; ++i)
			cache.free(new int[2 * 1024 * 1024]);
		for (int i = 0; i < cache.maxBuffersBySizeUnder128KB + 1; ++i)
			cache.free(new int[30000]);
		for (int i = 0; i < cache.maxBuffersBySizeUnder128KB + 1; ++i)
			cache.free(new int[20000]);
		for (int i = 0; i < cache.maxBuffersBySizeUnder128KB + 1; ++i)
			cache.free(new int[10000]);
		// free memory
		cache.freeMemory(FreeMemoryLevel.EXPIRED_ONLY);
		cache.freeMemory(FreeMemoryLevel.LOW);
		cache.freeMemory(FreeMemoryLevel.MEDIUM);
		cache.freeMemory(FreeMemoryLevel.URGENT);
		long expirationTime = cache.timeBeforeToRemove;
		cache.timeBeforeToRemove = -1;
		for (int i = 0; i < cache.maxBuffersBySizeUnder128KB; ++i)
			cache.free(new int[1024]);
		for (int i = 0; i < cache.maxBuffersBySizeUnder128KB; ++i)
			cache.free(new int[2048]);
		for (int i = 0; i < cache.maxBuffersBySizeUnder128KB; ++i)
			cache.free(new int[4096]);
		for (int i = 0; i < cache.maxBuffersBySizeUnder128KB; ++i)
			cache.free(new int[8192]);
		cache.freeMemory(FreeMemoryLevel.EXPIRED_ONLY);
		cache.freeMemory(FreeMemoryLevel.MEDIUM);
		cache.freeMemory(FreeMemoryLevel.MEDIUM);
		cache.freeMemory(FreeMemoryLevel.MEDIUM);
		cache.freeMemory(FreeMemoryLevel.MEDIUM);
		cache.freeMemory(FreeMemoryLevel.MEDIUM);
		cache.freeMemory(FreeMemoryLevel.URGENT);
		cache.timeBeforeToRemove = expirationTime;
		cache.free(new int[1024]);
		cache.free(new int[1024]);
		cache.get(1024, false);
		cache.freeMemory(FreeMemoryLevel.EXPIRED_ONLY);
		cache.freeMemory(FreeMemoryLevel.URGENT);
		// code cov
		cache.getDescription();
		cache.getItemsDescription();
	}
	
}
