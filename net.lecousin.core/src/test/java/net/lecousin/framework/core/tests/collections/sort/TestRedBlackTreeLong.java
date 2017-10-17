package net.lecousin.framework.core.tests.collections.sort;

import net.lecousin.framework.collections.sort.RedBlackTreeLong;
import net.lecousin.framework.collections.sort.Sorted;

public class TestRedBlackTreeLong extends TestSortedAssociatedWithLong {

	@Override
	protected Sorted.AssociatedWithLong<Object> createSorted() {
		return new RedBlackTreeLong<Object>();
	}
	
}
