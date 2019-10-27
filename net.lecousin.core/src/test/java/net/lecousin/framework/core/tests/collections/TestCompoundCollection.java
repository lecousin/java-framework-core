package net.lecousin.framework.core.tests.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.CollectionsUtil;
import net.lecousin.framework.collections.CompoundCollection;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public class TestCompoundCollection extends LCCoreAbstractTest {

	@Test
	public void test() {
		CompoundCollection<Integer> col = new CompoundCollection<>();
		check(col);
		col.addSingleton(Integer.valueOf(10));
		check(col, 10);
		col.add(Arrays.asList(Integer.valueOf(20), Integer.valueOf(21), Integer.valueOf(22)));
		check(col, 10, 20, 21, 22);
		col.add(CollectionsUtil.enumeration(Arrays.asList(Integer.valueOf(30), Integer.valueOf(31)).iterator()));
		check(col, 10, 20, 21, 22, 30, 31);
		col.enumeration();

		col = new CompoundCollection<>();
		check(col);
		col.addSingleton(Integer.valueOf(10));
		check(col, 10);
		col.add(new ArrayList<>(0));
		check(col, 10);
		col.add(Arrays.asList(Integer.valueOf(20), Integer.valueOf(21), Integer.valueOf(22)));
		check(col, 10, 20, 21, 22);

		col = new CompoundCollection<>();
		col.add(new ArrayList<>(0));
		check(col);
		col.addSingleton(Integer.valueOf(10));
		check(col, 10);
	}
	
	private static void check(CompoundCollection<Integer> col, int... values) {
		Iterator<Integer> it = col.iterator();
		for (int i = 0; i < values.length; ++i) {
			Assert.assertTrue(it.hasNext());
			Assert.assertEquals(values[i], it.next().intValue());
		}
		Assert.assertFalse(it.hasNext());
	}
	
}
