package net.lecousin.framework.core.tests.collections.maps;

import net.lecousin.framework.collections.map.IntegerMap;
import net.lecousin.framework.collections.map.IntegerMapRBT;

public class TestIntegerMapRBT extends TestIntegerMap {

	@Override
	public IntegerMap<Object> createIntegerMap() {
		return new IntegerMapRBT<Object>(20);
	}
	
}
