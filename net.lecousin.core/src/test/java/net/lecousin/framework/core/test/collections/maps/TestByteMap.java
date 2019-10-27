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

import net.lecousin.framework.collections.map.ByteMap;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public abstract class TestByteMap extends LCCoreAbstractTest {

	public abstract ByteMap<Object> createByteMap();

	@Test
	public void testByteMapIncrement() {
		ByteMap<Object> map = createByteMap();
		HashMap<Byte, Object> checkMap = new HashMap<>();
		checkEmpty(map);
		for (int i = 0; i < 256; ++i)
			put((byte)i, map, checkMap);
		for (int i = 0; i < 256; ++i)
			put((byte)i, map, checkMap);
		for (int i = 0; i < 256; i += 2)
			remove((byte)i, map, checkMap);
		for (int i = 0; i < 256; ++i) {
			put((byte)i, map, checkMap);
			put((byte)i, map, checkMap);
		}
		for (int i = 0; i < 256; i += 3)
			remove((byte)i, map, checkMap);
		for (int i = 0; i < 256; i += 3)
			remove((byte)i, map, checkMap);
	}

	@Test
	public void testByteMapRandom() {
		ByteMap<Object> map = createByteMap();
		HashMap<Byte, Object> checkMap = new HashMap<>();
		checkEmpty(map);
		LinkedList<Byte> bytes = new LinkedList<>();
		for (int i = 0; i < 256; ++i) bytes.add(Byte.valueOf((byte)i));
		Random rand = new Random();
		for (int i = 0; i < 256; ++i)
			put(bytes.remove(rand.nextInt(bytes.size())).byteValue(), map, checkMap);
		bytes.clear();
		for (int j = 0; j < 3; ++j)
			for (int i = 0; i < 256; ++i)
				bytes.add(Byte.valueOf((byte)i));
		while (!bytes.isEmpty())
			remove(bytes.remove(rand.nextInt(bytes.size())).byteValue(), map, checkMap);
		checkEmpty(map);
	}
	
	@Test
	public void testClear() {
		ByteMap<Object> map = createByteMap();
		checkEmpty(map);
		map.clear();
		checkEmpty(map);
		for (int i = 10; i < 100; ++i)
			map.put((byte)i, Integer.valueOf(i));
		map.clear();
		checkEmpty(map);
	}
	
	
	@Test
	public void testValuesIterator() {
		ByteMap<Object> map = createByteMap();
		List<Byte> list = new LinkedList<>();
		for (byte i = 0; i < 100; ++i) {
			map.put(i, Byte.valueOf(i));
			list.add(Byte.valueOf(i));
		}
		Assert.assertEquals(100, map.size());
		Iterator<Object> it = map.values();
		while (it.hasNext()) {
			Object o = it.next();
			Assert.assertTrue(o instanceof Byte);
			byte i = ((Byte)o).byteValue();
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

	protected void checkEmpty(ByteMap<Object> map) {
		Assert.assertEquals(0, map.size());
		Assert.assertTrue(map.isEmpty());
		Assert.assertFalse(map.containsKey((byte)0));
		Assert.assertFalse(map.containsKey((byte)1));
		Assert.assertNull(map.get((byte)0));
		Assert.assertNull(map.get((byte)1));
		Assert.assertFalse(map.values().hasNext());
	}
	
	protected void checkMap(ByteMap<Object> map, HashMap<Byte, Object> checkMap) {
		Assert.assertEquals(checkMap.size(), map.size());
		Assert.assertTrue(checkMap.isEmpty() == map.isEmpty());
		for (Map.Entry<Byte, Object> e : checkMap.entrySet()) {
			Assert.assertTrue("containsKey(" + e.getKey() + ") returns false", map.containsKey(e.getKey().byteValue()));
			Assert.assertEquals(e.getValue(), map.get(e.getKey().byteValue()));
		}
		byte b = Byte.MIN_VALUE;
		do {
			if (checkMap.get(Byte.valueOf(b)) == null)
				Assert.assertNull(map.get(b));
			b++;
		} while (b != Byte.MIN_VALUE);
		Iterator<Object> values = map.values();
		for (int i = 0; i < checkMap.size(); ++i) {
			Assert.assertTrue(values.hasNext());
			Assert.assertTrue(checkMap.containsValue(values.next()));
		}
		Assert.assertFalse(values.hasNext());
	}
	
	protected void put(byte b, ByteMap<Object> map, HashMap<Byte, Object> checkMap) {
		try { map.put(b, Integer.valueOf(b & 0xFF)); }
		catch (Throwable t) {
			throw new RuntimeException("Error in put(" + b + ")", t);
		}
		checkMap.put(Byte.valueOf(b), Integer.valueOf(b & 0xFF));
		checkMap(map, checkMap);
	}
	
	protected void remove(byte b, ByteMap<Object> map, HashMap<Byte, Object> checkMap) {
		if (!checkMap.containsKey(Byte.valueOf(b))) {
			Assert.assertFalse(map.containsKey(b));
			Assert.assertTrue(map.remove(b) == null);
			return;
		}
		try { map.remove(b); }
		catch (Throwable t) {
			throw new RuntimeException("Error in remove(" + b + ")", t);
		}
		checkMap.remove(Byte.valueOf(b));
		checkMap(map, checkMap);
	}
	
}
