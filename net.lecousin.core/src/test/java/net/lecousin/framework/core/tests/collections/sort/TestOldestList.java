package net.lecousin.framework.core.tests.collections.sort;

import java.util.Iterator;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.collections.sort.OldestList;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestOldestList extends LCCoreAbstractTest {

	@Test
	public void test() {
		OldestList<Integer> list = new OldestList<>(5);
		list.add(5, Integer.valueOf(50));
		check(list, 50);
		list.add(4, Integer.valueOf(40));
		check(list, 40, 50);
		list.add(6, Integer.valueOf(60));
		check(list, 40, 50, 60);
		list.add(10, Integer.valueOf(100));
		check(list, 40, 50, 60, 100);
		list.add(20, Integer.valueOf(200));
		check(list, 40, 50, 60, 100, 200);
		list.add(1, Integer.valueOf(10));
		check(list, 10, 40, 50, 60, 100);
		list.add(2, Integer.valueOf(20));
		check(list, 10, 20, 40, 50, 60);
		list.add(7, Integer.valueOf(70));
		check(list, 10, 20, 40, 50, 60);
	}
	
	private static void check(OldestList<Integer> list, int... expected) {
		Iterator<Integer> it = list.iterator();
		for (int i = 0; i < expected.length; ++i) {
			int val = it.next().intValue();
			if (!ArrayUtil.contains(expected, val))
				throw new AssertionError("Unexpected value " + val);
		}
		Assert.assertFalse(it.hasNext());
	}
	
}
