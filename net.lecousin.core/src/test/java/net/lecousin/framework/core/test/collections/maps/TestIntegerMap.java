package net.lecousin.framework.core.test.collections.maps;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.map.IntegerMap;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public abstract class TestIntegerMap extends LCCoreAbstractTest {

	public abstract IntegerMap<Object> createIntegerMap();

	@Test(timeout=120000)
	public void testIntegerMapIncrement() {
		IntegerMap<Object> map = createIntegerMap();
		HashMap<Integer, Object> checkMap = new HashMap<>();
		checkEmpty(map);
		map.remove(123);
		checkEmpty(map);
		for (int i = 0; i < 1000; ++i)
			put(i, map, checkMap);
		for (int i = 2000; i < 10000; i += 7)
			put(i, map, checkMap);
		for (int i = 0; i < 10000; i += 3)
			remove(i, map, checkMap);
		for (int i = -100; i > -1000; --i)
			put(i, map, checkMap);
		for (int i = 600; i < 800; ++i) {
			put(i, map, checkMap);
			put(i, map, checkMap);
		}
	}

	@Test(timeout=120000)
	public void testIntegerMapRandom() {
		IntegerMap<Object> map = createIntegerMap();
		HashMap<Integer, Object> checkMap = new HashMap<>();
		checkEmpty(map);
		LinkedList<Integer> integers = new LinkedList<>();
		Random rand = new Random();
		int value = 0;
		for (int i = 0; i < 2500; ++i) {
			value += rand.nextInt(20);
			integers.add(Integer.valueOf(value));
		}
		while (!integers.isEmpty())
			put(integers.remove(rand.nextInt(integers.size())).intValue(), map, checkMap);
		integers.clear();
		integers.addAll(checkMap.keySet());
		while (!integers.isEmpty())
			remove(integers.remove(rand.nextInt(integers.size())).intValue(), map, checkMap);
		checkEmpty(map);
	}
	
	@Test(timeout=30000)
	public void testClear() {
		IntegerMap<Object> map = createIntegerMap();
		checkEmpty(map);
		map.clear();
		checkEmpty(map);
		for (int i = 10; i < 100; ++i)
			map.put(i, Integer.valueOf(i));
		map.clear();
		checkEmpty(map);
	}
	
	protected void checkEmpty(IntegerMap<Object> map) {
		Assert.assertEquals(0, map.size());
		Assert.assertTrue(map.isEmpty());
		Assert.assertFalse(map.containsKey(0));
		Assert.assertFalse(map.containsKey(1));
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(1));
		Assert.assertFalse(map.values().hasNext());
	}
	
	protected void checkMap(IntegerMap<Object> map, HashMap<Integer, Object> checkMap) {
		Assert.assertEquals(checkMap.size(), map.size());
		Assert.assertTrue(checkMap.isEmpty() == map.isEmpty());
		for (Map.Entry<Integer, Object> e : checkMap.entrySet()) {
			Assert.assertTrue("containsKey(" + e.getKey() + ") returns false", map.containsKey(e.getKey().intValue()));
			Assert.assertEquals(e.getValue(), map.get(e.getKey().intValue()));
		}
		Iterator<Object> values = map.values();
		for (int i = 0; i < checkMap.size(); ++i) {
			Assert.assertTrue(values.hasNext());
			Assert.assertTrue(checkMap.containsValue(values.next()));
		}
		Assert.assertFalse(values.hasNext());
	}
	
	protected void put(int i, IntegerMap<Object> map, HashMap<Integer, Object> checkMap) {
		try { map.put(i, Integer.valueOf(i)); }
		catch (Throwable t) {
			throw new RuntimeException("Error in put(" + i + ")", t);
		}
		checkMap.put(Integer.valueOf(i), Integer.valueOf(i));
		checkMap(map, checkMap);
	}
	
	protected void remove(int i, IntegerMap<Object> map, HashMap<Integer, Object> checkMap) {
		if (!checkMap.containsKey(Integer.valueOf(i))) {
			Assert.assertFalse(map.containsKey(i));
			Assert.assertTrue(map.remove(i) == null);
			return;
		}
		try { map.remove(i); }
		catch (Throwable t) {
			throw new RuntimeException("Error in remove(" + i + ")", t);
		}
		checkMap.remove(Integer.valueOf(i));
		checkMap(map, checkMap);
	}
	
}
