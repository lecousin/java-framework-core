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

import net.lecousin.framework.collections.map.LongMap;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public abstract class TestLongMap extends LCCoreAbstractTest {

	public abstract LongMap<Object> createLongMap();

	@Test
	public void testLongMapIncrement() {
		LongMap<Object> map = createLongMap();
		HashMap<Long, Object> checkMap = new HashMap<>();
		checkEmpty(map);
		map.remove(123);
		checkEmpty(map);
		for (long i = 0; i < 200; ++i)
			put(i, map, checkMap, i % 2 == 0);
		for (long i = 5000; i < 10000; i += 7)
			put(i, map, checkMap, i % 2 == 0);
		for (long i = 1000000000L; i < 1000020000L; i += 1500)
			put(i, map, checkMap, i % 2 == 0);
		for (long i = 0; i < 300; i += 3)
			remove(i, map, checkMap, i % 2 == 0);
		for (long i = 4500; i < 11000; i += 5)
			remove(i, map, checkMap, i % 2 == 0);
		for (long i = 1000000007L; i < 1000020007L; i += 13)
			remove(i, map, checkMap, i % 2 == 0);
		for (long i = -5000; i > -7000; i -= 3)
			put(i, map, checkMap, i % 2 == 0);
		for (long i = 600; i < 800; ++i) {
			put(i, map, checkMap, i % 2 == 0);
			put(i, map, checkMap, i % 2 == 0);
		}
	}

	@Test
	public void testLongMapRandom() {
		LongMap<Object> map = createLongMap();
		HashMap<Long, Object> checkMap = new HashMap<>();
		checkEmpty(map);
		LinkedList<Long> values = new LinkedList<>();
		Random rand = new Random();
		long value = 0;
		for (int i = 0; i < 2500; ++i) {
			value += rand.nextLong();
			values.add(Long.valueOf(value));
		}
		while (!values.isEmpty())
			put(values.remove(rand.nextInt(values.size())).longValue(), map, checkMap, values.size() % 2 == 0);
		values.clear();
		values.addAll(checkMap.keySet());
		while (!values.isEmpty())
			remove(values.remove(rand.nextInt(values.size())).longValue(), map, checkMap, values.size() % 2 == 0);
		checkEmpty(map);
	}

	@Test
	public void testLongMapRandomWithNegativeValues() {
		LongMap<Object> map = createLongMap();
		HashMap<Long, Object> checkMap = new HashMap<>();
		checkEmpty(map);
		LinkedList<Long> values = new LinkedList<>();
		Random rand = new Random();
		long value = 0;
		for (int i = 0; i < 2500; ++i) {
			value += rand.nextLong();
			if (rand.nextBoolean())
				value = -value;
			values.add(Long.valueOf(value));
		}
		while (!values.isEmpty())
			put(values.remove(rand.nextInt(values.size())).longValue(), map, checkMap, values.size() % 2 == 0);
		values.clear();
		values.addAll(checkMap.keySet());
		while (!values.isEmpty())
			remove(values.remove(rand.nextInt(values.size())).longValue(), map, checkMap, values.size() % 2 == 0);
		checkEmpty(map);
	}
	
	@Test
	public void testClear() {
		LongMap<Object> map = createLongMap();
		checkEmpty(map);
		map.clear();
		checkEmpty(map);
		for (int i = 10; i < 100; ++i)
			map.put(i, Integer.valueOf(i));
		map.clear();
		checkEmpty(map);
	}
	
	@Test
	public void testValuesIterator() {
		LongMap<Object> map = createLongMap();
		List<Long> list = new LinkedList<>();
		for (long i = 0; i < 100; ++i) {
			map.put(i, Long.valueOf(i));
			list.add(Long.valueOf(i));
		}
		Assert.assertEquals(100, map.size());
		Iterator<Object> it = map.values();
		while (it.hasNext()) {
			Object o = it.next();
			Assert.assertTrue(o instanceof Long);
			long i = ((Long)o).longValue();
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

	protected void checkEmpty(LongMap<Object> map) {
		Assert.assertEquals(0, map.size());
		Assert.assertTrue(map.isEmpty());
		Assert.assertFalse(map.containsKey(0));
		Assert.assertFalse(map.containsKey(1));
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(1));
		Assert.assertFalse(map.values().hasNext());
	}
	
	protected void checkMap(LongMap<Object> map, HashMap<Long, Object> checkMap) {
		Assert.assertEquals(checkMap.size(), map.size());
		Assert.assertTrue(checkMap.isEmpty() == map.isEmpty());
		for (Map.Entry<Long, Object> e : checkMap.entrySet()) {
			Assert.assertTrue("containsKey(" + e.getKey() + ") returns false", map.containsKey(e.getKey().longValue()));
			Assert.assertEquals(e.getValue(), map.get(e.getKey().longValue()));
		}
		Iterator<Object> values = map.values();
		for (int i = 0; i < checkMap.size(); ++i) {
			Assert.assertTrue(values.hasNext());
			Assert.assertTrue(checkMap.containsValue(values.next()));
		}
		Assert.assertFalse(values.hasNext());
	}
	
	protected void put(long i, LongMap<Object> map, HashMap<Long, Object> checkMap, boolean doCheck) {
		try { map.put(i, Long.valueOf(i)); }
		catch (Throwable t) {
			throw new RuntimeException("Error in put(" + i + ")", t);
		}
		checkMap.put(Long.valueOf(i), Long.valueOf(i));
		if (doCheck)
			checkMap(map, checkMap);
	}
	
	protected void remove(long i, LongMap<Object> map, HashMap<Long, Object> checkMap, boolean doCheck) {
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
		if (doCheck)
			checkMap(map, checkMap);
	}
	
}
