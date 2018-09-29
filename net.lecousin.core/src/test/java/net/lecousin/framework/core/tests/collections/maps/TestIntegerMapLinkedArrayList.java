package net.lecousin.framework.core.tests.collections.maps;

import net.lecousin.framework.collections.map.IntegerMap;
import net.lecousin.framework.collections.map.IntegerMapLinkedArrayList;
import net.lecousin.framework.core.test.collections.maps.TestIntegerMap;

public class TestIntegerMapLinkedArrayList extends TestIntegerMap {

	@Override
	public IntegerMap<Object> createIntegerMap() {
		return new IntegerMapLinkedArrayList<Object>(20, 10);
	}
	
}
