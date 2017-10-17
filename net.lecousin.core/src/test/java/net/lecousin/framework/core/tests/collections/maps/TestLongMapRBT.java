package net.lecousin.framework.core.tests.collections.maps;

import net.lecousin.framework.collections.map.LongMap;
import net.lecousin.framework.collections.map.LongMapRBT;

public class TestLongMapRBT extends TestLongMap {

	@Override
	public LongMap<Object> createLongMap() {
		return new LongMapRBT<Object>(20);
	}
	
}
