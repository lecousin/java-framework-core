package net.lecousin.framework.core.tests.collections.maps;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.map.LongMap;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public abstract class TestLongMap extends LCCoreAbstractTest {

	public abstract LongMap<Object> createLongMap();

	@Test
	public void testLongMapIncrement() {
		LongMap<Object> map = createLongMap();
		HashMap<Long, Object> checkMap = new HashMap<>();
		checkEmpty(map);
		for (long i = 0; i < 200; ++i)
			put(i, map, checkMap);
		for (long i = 5000; i < 10000; i += 7)
			put(i, map, checkMap);
		for (long i = 1000000000L; i < 1000050000L; i += 1500)
			put(i, map, checkMap);
		for (long i = 0; i < 300; i += 3)
			remove(i, map, checkMap);
		for (long i = 4500; i < 11000; i += 5)
			remove(i, map, checkMap);
		for (long i = 1000000007L; i < 1000050007L; i += 13)
			remove(i, map, checkMap);
	}

	@Test
	public void testLongMapRandom() {
		LongMap<Object> map = createLongMap();
		HashMap<Long, Object> checkMap = new HashMap<>();
		checkEmpty(map);
		LinkedList<Long> values = new LinkedList<>();
		Random rand = new Random();
		long value = 0;
		for (int i = 0; i < 5000; ++i) {
			value += rand.nextLong();
			values.add(Long.valueOf(value));
		}
		while (!values.isEmpty())
			put(values.remove(rand.nextInt(values.size())).longValue(), map, checkMap);
		values.clear();
		values.addAll(checkMap.keySet());
		while (!values.isEmpty())
			remove(values.remove(rand.nextInt(values.size())).longValue(), map, checkMap);
		checkEmpty(map);
	}
	
	protected void checkEmpty(LongMap<Object> map) {
		Assert.assertEquals(0, map.size());
		Assert.assertTrue(map.isEmpty());
		Assert.assertFalse(map.containsKey(0));
		Assert.assertFalse(map.containsKey(1));
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(1));
	}
	
	protected void checkMap(LongMap<Object> map, HashMap<Long, Object> checkMap) {
		Assert.assertEquals(checkMap.size(), map.size());
		Assert.assertTrue(checkMap.isEmpty() == map.isEmpty());
		for (Map.Entry<Long, Object> e : checkMap.entrySet()) {
			Assert.assertTrue("containsKey(" + e.getKey() + ") returns false", map.containsKey(e.getKey().longValue()));
			Assert.assertEquals(e.getValue(), map.get(e.getKey().longValue()));
		}
	}
	
	protected void put(long i, LongMap<Object> map, HashMap<Long, Object> checkMap) {
		try { map.put(i, Long.valueOf(i)); }
		catch (Throwable t) {
			throw new RuntimeException("Error in put(" + i + ")", t);
		}
		checkMap.put(Long.valueOf(i), Long.valueOf(i));
		checkMap(map, checkMap);
	}
	
	protected void remove(long i, LongMap<Object> map, HashMap<Long, Object> checkMap) {
		if (!checkMap.containsKey(Long.valueOf(i))) {
			Assert.assertFalse(map.containsKey(i));
			Assert.assertTrue(map.remove(i) == null);
			return;
		}
		try { map.remove(i); }
		catch (Throwable t) {
			throw new RuntimeException("Error in remove(" + i + ")", t);
		}
		checkMap.remove(Long.valueOf(i));
		checkMap(map, checkMap);
	}
	
}
