package net.lecousin.framework.core.test.collections.sort;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.sort.Sorted;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public abstract class TestSortedAssociatedWithInteger extends LCCoreAbstractTest {

	protected abstract Sorted.AssociatedWithInteger<Object> createSorted();
	
	@Test(timeout=120000)
	public void testAddRemoveIncrement() {
		Sorted.AssociatedWithInteger<Object> list = createSorted();
		TreeSet<Integer> order = new TreeSet<>();
		for (int i = 0; i < 1000; ++i) {
			list.add(i, Integer.valueOf(-i));
			order.add(Integer.valueOf(i));
			check(list, order);
		}
		for (int i = 0; i < 1000; ++i) {
			list.remove(i, Integer.valueOf(-i));
			order.remove(Integer.valueOf(i));
			check(list, order);
		}
	}
	
	@Test(timeout=120000)
	public void testAddIncrementRemoveDecrement() {
		Sorted.AssociatedWithInteger<Object> list = createSorted();
		TreeSet<Integer> order = new TreeSet<>();
		for (int i = 0; i < 1000; ++i) {
			list.add(i, Integer.valueOf(-i));
			order.add(Integer.valueOf(i));
			check(list, order);
		}
		for (int i = 999; i >= 0; --i) {
			list.remove(i, Integer.valueOf(-i));
			order.remove(Integer.valueOf(i));
			check(list, order);
		}
	}
	
	@Test(timeout=120000)
	public void testAddRemoveDecrement() {
		Sorted.AssociatedWithInteger<Object> list = createSorted();
		TreeSet<Integer> order = new TreeSet<>();
		for (int i = 999; i >= 0; --i) {
			list.add(i, Integer.valueOf(-i));
			order.add(Integer.valueOf(i));
			check(list, order);
		}
		for (int i = 999; i >= 0; --i) {
			list.remove(i, Integer.valueOf(-i));
			order.remove(Integer.valueOf(i));
			check(list, order);
		}
	}
	
	@Test(timeout=120000)
	public void testAddDecrementRemoveIncrement() {
		Sorted.AssociatedWithInteger<Object> list = createSorted();
		TreeSet<Integer> order = new TreeSet<>();
		for (int i = 999; i >= 0; --i) {
			list.add(i, Integer.valueOf(-i));
			order.add(Integer.valueOf(i));
			check(list, order);
		}
		for (int i = 0; i < 1000; ++i) {
			list.remove(i, Integer.valueOf(-i));
			order.remove(Integer.valueOf(i));
			check(list, order);
		}
	}
	
	@Test(timeout=120000)
	public void testAddRemoveRandom() {
		Sorted.AssociatedWithInteger<Object> list = createSorted();
		TreeSet<Integer> order = new TreeSet<>();
		Random rand = new Random();
		for (int i = 0; i < 5000; ++i) {
			int value = rand.nextInt();
			while (order.contains(Integer.valueOf(value))) value++;
			list.add(value, Integer.valueOf(-value));
			order.add(Integer.valueOf(value));
			check(list, order);
		}
		LinkedList<Integer> values = new LinkedList<>(order);
		while (!values.isEmpty()) {
			int value = values.remove(rand.nextInt(values.size())).intValue();
			list.remove(value, Integer.valueOf(-value));
			order.remove(Integer.valueOf(value));
			check(list, order);
		}
	}
	
	protected void check(Sorted.AssociatedWithInteger<Object> sorted, TreeSet<Integer> order) {
		Assert.assertEquals(order.size(), sorted.size());
		for (Integer i : order) {
			int value = i.intValue();
			Assert.assertTrue("contains(" + value + ") returned false", sorted.contains(value, Integer.valueOf(-value)));
		}
		for (Object o : sorted) {
			int value = -((Integer)o).intValue();
			Assert.assertTrue(order.contains(Integer.valueOf(value)));
		}
		Iterator<Object> it1 = sorted.orderedIterator();
		Iterator<Integer> it2 = order.iterator();
		int nb = 0;
		while (it1.hasNext()) {
			Assert.assertTrue(it2.hasNext());
			Object o = it1.next();
			Integer i = it2.next();
			Assert.assertEquals(i.intValue(), -((Integer)o).intValue());
			nb++;
		}
		Assert.assertEquals(sorted.size(), nb);
		Assert.assertFalse(it2.hasNext());
	}
	
}
