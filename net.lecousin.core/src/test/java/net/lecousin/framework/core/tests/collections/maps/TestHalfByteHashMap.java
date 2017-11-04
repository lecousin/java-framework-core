package net.lecousin.framework.core.tests.collections.maps;

import net.lecousin.framework.collections.map.ByteMap;
import net.lecousin.framework.collections.map.HalfByteHashMap;
import net.lecousin.framework.core.test.collections.maps.TestByteMap;

public class TestHalfByteHashMap extends TestByteMap {

	@Override
	public ByteMap<Object> createByteMap() {
		return new HalfByteHashMap<>();
	}
	
}
