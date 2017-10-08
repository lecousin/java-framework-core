package net.lecousin.framework.core.tests.collections;

import net.lecousin.framework.collections.LinkedArrayList;

public class TestLinkedArrayList extends TestList {

	@Override
	public LinkedArrayList<Long> createLongCollection() {
		return new LinkedArrayList<Long>(5);
	}
	
}
