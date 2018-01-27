package net.lecousin.framework.core.tests.collections.maps;

import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.collections.map.MapUtil;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestMapUtil extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		Map<Integer, String> map = new HashMap<>();
		map.put(Integer.valueOf(0), "test1");
		map.put(Integer.valueOf(1), "test2");
		map.put(Integer.valueOf(2), "test1");
		map.put(Integer.valueOf(3), "test3");
		map.put(Integer.valueOf(4), "test1");
		map.put(Integer.valueOf(5), "test4");
		map.put(Integer.valueOf(6), "test1");
		map.put(Integer.valueOf(7), "test5");
		map.put(Integer.valueOf(8), "test1");
		map.put(Integer.valueOf(9), "test6");
		Assert.assertEquals(10, map.size());
		MapUtil.removeValue(map, "test4");
		Assert.assertEquals(9, map.size());
		MapUtil.removeValue(map, "test1");
		Assert.assertEquals(4, map.size());
	}
	
}
