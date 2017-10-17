package net.lecousin.framework.core.tests.collections.maps;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
		for (int i = 0; i < 256; i += 2)
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
	
	protected void checkEmpty(ByteMap<Object> map) {
		Assert.assertEquals(0, map.size());
		Assert.assertTrue(map.isEmpty());
		Assert.assertFalse(map.containsKey((byte)0));
		Assert.assertFalse(map.containsKey((byte)1));
		Assert.assertNull(map.get((byte)0));
		Assert.assertNull(map.get((byte)1));
	}
	
	protected void checkMap(ByteMap<Object> map, HashMap<Byte, Object> checkMap) {
		Assert.assertEquals(checkMap.size(), map.size());
		Assert.assertTrue(checkMap.isEmpty() == map.isEmpty());
		for (Map.Entry<Byte, Object> e : checkMap.entrySet()) {
			Assert.assertTrue("containsKey(" + e.getKey() + ") returns false", map.containsKey(e.getKey().byteValue()));
			Assert.assertEquals(e.getValue(), map.get(e.getKey().byteValue()));
		}
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
