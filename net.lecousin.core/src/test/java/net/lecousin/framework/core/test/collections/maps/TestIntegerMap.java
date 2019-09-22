package net.lecousin.framework.core.test.collections.maps;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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

	@Test(timeout=120000)
	public void testIntegerMapRandomWithNegativeValue() {
		IntegerMap<Object> map = createIntegerMap();
		HashMap<Integer, Object> checkMap = new HashMap<>();
		checkEmpty(map);
		LinkedList<Integer> integers = new LinkedList<>();
		Random rand = new Random();
		int value = 0;
		for (int i = 0; i < 2500; ++i) {
			value += rand.nextInt(20);
			if (rand.nextBoolean())
				value = -value;
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
	
	@Test(timeout=30000)
	public void testValuesIterator() {
		IntegerMap<Object> map = createIntegerMap();
		List<Integer> list = new LinkedList<>();
		for (int i = 0; i < 100; ++i) {
			map.put(i, Integer.valueOf(i));
			list.add(Integer.valueOf(i));
		}
		Assert.assertEquals(100, map.size());
		Iterator<Object> it = map.values();
		while (it.hasNext()) {
			Object o = it.next();
			Assert.assertTrue(o instanceof Integer);
			int i = ((Integer)o).intValue();
			Assert.assertTrue(i < 100 && i >= 0);
			Assert.assertTrue(list.remove(o));
		}
		Assert.assertTrue(list.isEmpty());
		try {
			it.next();
			throw new AssertionError("NoSuchElement");
		} catch (NoSuchElementException e) {
			// ok
		}
		// test remove half
		try {
			it = map.values();
			int i = 0;
			while (it.hasNext()) {
				it.next();
				if ((i % 2) == 0) it.remove();
				++i;
			}
			Assert.assertEquals(50, map.size());
			i = 0;
			it = map.values();
			while (it.hasNext()) {
				i++;
				it.next();
			}
			Assert.assertEquals(50, i);
		} catch (UnsupportedOperationException e) {
			// ok if not supported
		}
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
