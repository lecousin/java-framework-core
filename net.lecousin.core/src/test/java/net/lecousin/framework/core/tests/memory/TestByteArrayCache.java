package net.lecousin.framework.core.tests.memory;

import java.nio.ByteBuffer;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.memory.ByteArrayCache;
import net.lecousin.framework.memory.IMemoryManageable.FreeMemoryLevel;

import org.junit.Assert;
import org.junit.Test;

public class TestByteArrayCache extends LCCoreAbstractTest {

	@Test
	public void test() {
		ByteArrayCache cache = ByteArrayCache.getInstance();
		cache.setMaxBuffersBySizeAbove128KB(cache.getMaxBuffersBySizeAbove128KB());
		cache.setMaxBuffersBySizeUnder128KB(cache.getMaxBuffersBySizeUnder128KB());
		cache.setMaxTotalSize(cache.getMaxTotalSize());
		cache.setTimeBeforeToRemove(cache.getTimeBeforeToRemove());
		cache.free(ByteBuffer.allocate(0));
		cache.free(ByteBuffer.allocate(0).asReadOnlyBuffer());
		Assert.assertTrue(cache == ByteArrayCache.getInstance());
		// clear content
		cache.freeMemory(FreeMemoryLevel.URGENT);
		// create buffers
		byte[] b512 = cache.get(512, false);
		Assert.assertEquals(512, b512.length);
		byte[] b1024 = cache.get(1024, false);
		Assert.assertEquals(1024, b1024.length);
		byte[] b1200 = cache.get(1200, false);
		Assert.assertEquals(1200, b1200.length);
		byte[] b1400 = cache.get(1400, false);
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
		byte[] b1024_2 = new byte[1024];
		cache.free(b1024_2);
		byte[] b = cache.get(1024, true);
		Assert.assertTrue(b == b1024 || b == b1024_2);
		b = cache.get(1024, true);
		Assert.assertTrue(b == b1024 || b == b1024_2);
		cache.free(b1024);
		// free many buffers
		for (int i = 0; i < cache.getMaxBuffersBySizeUnder128KB() + 1; ++i)
			cache.free(new byte[65536]);
		for (int i = 0; i < cache.getMaxBuffersBySizeAbove128KB() + 1; ++i)
			cache.free(new byte[10 * 1024 * 1024]);
		for (int i = 0; i < cache.getMaxBuffersBySizeUnder128KB() + 1; ++i)
			cache.free(new byte[100000]);
		// free memory
		cache.freeMemory(FreeMemoryLevel.EXPIRED_ONLY);
		cache.freeMemory(FreeMemoryLevel.LOW);
		cache.freeMemory(FreeMemoryLevel.MEDIUM);
		cache.freeMemory(FreeMemoryLevel.URGENT);
		long expirationTime = cache.getTimeBeforeToRemove();
		cache.setTimeBeforeToRemove(-1);
		for (int i = 0; i < cache.getMaxBuffersBySizeUnder128KB(); ++i)
			cache.free(new byte[1024]);
		cache.freeMemory(FreeMemoryLevel.EXPIRED_ONLY);
		cache.freeMemory(FreeMemoryLevel.URGENT);
		cache.setTimeBeforeToRemove(expirationTime);
		cache.free(new byte[1024]);
		cache.free(new byte[1024]);
		cache.get(1024, false);
		cache.freeMemory(FreeMemoryLevel.EXPIRED_ONLY);
		cache.freeMemory(FreeMemoryLevel.URGENT);
		// code cov
		cache.getDescription();
		cache.getItemsDescription();
	}
	
}
