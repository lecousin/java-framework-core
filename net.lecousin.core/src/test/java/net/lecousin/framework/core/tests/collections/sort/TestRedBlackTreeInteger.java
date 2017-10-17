package net.lecousin.framework.core.tests.collections.sort;

import net.lecousin.framework.collections.sort.RedBlackTreeInteger;
import net.lecousin.framework.collections.sort.Sorted.AssociatedWithInteger;

public class TestRedBlackTreeInteger extends TestSortedAssociatedWithInteger {

	@Override
	protected AssociatedWithInteger<Object> createSorted() {
		return new RedBlackTreeInteger<Object>();
	}
	
}
