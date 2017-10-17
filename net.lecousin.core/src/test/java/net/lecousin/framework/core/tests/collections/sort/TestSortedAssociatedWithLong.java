package net.lecousin.framework.core.tests.collections.sort;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.sort.Sorted;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public abstract class TestSortedAssociatedWithLong extends LCCoreAbstractTest {

	protected abstract Sorted.AssociatedWithLong<Object> createSorted();
	
	@Test
	public void testAddRemoveIncrement() {
		Sorted.AssociatedWithLong<Object> list = createSorted();
		TreeSet<Long> order = new TreeSet<>();
		for (long i = 0; i < 1000; ++i) {
			list.add(i, Long.valueOf(-i));
			order.add(Long.valueOf(i));
			check(list, order);
		}
		for (int i = 0; i < 1000; ++i) {
			list.remove(i, Long.valueOf(-i));
			order.remove(Long.valueOf(i));
			check(list, order);
		}
	}
	
	@Test
	public void testAddIncrementRemoveDecrement() {
		Sorted.AssociatedWithLong<Object> list = createSorted();
		TreeSet<Long> order = new TreeSet<>();
		for (long i = 0; i < 1000; ++i) {
			list.add(i, Long.valueOf(-i));
			order.add(Long.valueOf(i));
			check(list, order);
		}
		for (long i = 999; i >= 0; --i) {
			list.remove(i, Long.valueOf(-i));
			order.remove(Long.valueOf(i));
			check(list, order);
		}
	}
	
	@Test
	public void testAddRemoveDecrement() {
		Sorted.AssociatedWithLong<Object> list = createSorted();
		TreeSet<Long> order = new TreeSet<>();
		for (long i = 999; i >= 0; --i) {
			list.add(i, Long.valueOf(-i));
			order.add(Long.valueOf(i));
			check(list, order);
		}
		for (long i = 999; i >= 0; --i) {
			list.remove(i, Long.valueOf(-i));
			order.remove(Long.valueOf(i));
			check(list, order);
		}
	}
	
	@Test
	public void testAddDecrementRemoveIncrement() {
		Sorted.AssociatedWithLong<Object> list = createSorted();
		TreeSet<Long> order = new TreeSet<>();
		for (long i = 999; i >= 0; --i) {
			list.add(i, Long.valueOf(-i));
			order.add(Long.valueOf(i));
			check(list, order);
		}
		for (long i = 0; i < 1000; ++i) {
			list.remove(i, Long.valueOf(-i));
			order.remove(Long.valueOf(i));
			check(list, order);
		}
	}
	
	@Test
	public void testAddRemoveRandom() {
		Sorted.AssociatedWithLong<Object> list = createSorted();
		TreeSet<Long> order = new TreeSet<>();
		Random rand = new Random();
		for (int i = 0; i < 5000; ++i) {
			long value = rand.nextLong();
			while (order.contains(Long.valueOf(value))) value++;
			list.add(value, Long.valueOf(-value));
			order.add(Long.valueOf(value));
			check(list, order);
		}
		LinkedList<Long> values = new LinkedList<>(order);
		while (!values.isEmpty()) {
			long value = values.remove(rand.nextInt(values.size())).longValue();
			list.remove(value, Long.valueOf(-value));
			order.remove(Long.valueOf(value));
			check(list, order);
		}
	}
	
	protected void check(Sorted.AssociatedWithLong<Object> sorted, TreeSet<Long> order) {
		Assert.assertEquals(order.size(), sorted.size());
		for (Long i : order) {
			long value = i.longValue();
			Assert.assertTrue("contains(" + value + ") returned false", sorted.contains(value, Long.valueOf(-value)));
		}
		for (Object o : sorted) {
			long value = -((Long)o).longValue();
			Assert.assertTrue(order.contains(Long.valueOf(value)));
		}
		Iterator<Object> it1 = sorted.orderedIterator();
		Iterator<Long> it2 = order.iterator();
		int nb = 0;
		while (it1.hasNext()) {
			Assert.assertTrue(it2.hasNext());
			Object o = it1.next();
			Long i = it2.next();
			Assert.assertEquals(i.longValue(), -((Long)o).longValue());
			nb++;
		}
		Assert.assertEquals(sorted.size(), nb);
		Assert.assertFalse(it2.hasNext());
	}
	
}
