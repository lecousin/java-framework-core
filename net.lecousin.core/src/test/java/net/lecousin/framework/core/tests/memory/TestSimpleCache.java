package net.lecousin.framework.core.tests.memory;

import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.memory.SimpleCache;

import org.junit.Assert;
import org.junit.Test;

public class TestSimpleCache extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		Map<Integer, Integer> created = new HashMap<>();
		SimpleCache<String, Integer> cache = new SimpleCache<>("test simple cache", (str) -> {
			Integer val = Integer.valueOf(str);
			Integer i = created.get(val);
			if (i == null)
				created.put(val, Integer.valueOf(1));
			else
				created.put(val, Integer.valueOf(i.intValue() + 1));
			return val;
		});
		Assert.assertEquals(1, cache.get("1").intValue());
		Assert.assertEquals(1, created.get(Integer.valueOf(1)).intValue());
		Assert.assertEquals(1, cache.get("1").intValue());
		Assert.assertEquals(1, created.get(Integer.valueOf(1)).intValue());
		cache.remove("1");
		Assert.assertEquals(1, cache.get("1").intValue());
		Assert.assertEquals(2, created.get(Integer.valueOf(1)).intValue());
		cache.close();
	}
	
}
