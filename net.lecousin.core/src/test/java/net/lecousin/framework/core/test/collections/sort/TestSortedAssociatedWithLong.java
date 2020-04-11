package net.lecousin.framework.core.test.collections.sort;

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
	public void simpleTests() {
		Sorted.AssociatedWithLong<Object> list = createSorted();
		Integer elem1 = Integer.valueOf(10);
		list.add(10, elem1);
		Integer elem2 = Integer.valueOf(12);
		list.add(12, elem2);
		Assert.assertTrue(list.containsInstance(10, elem1));
		Assert.assertTrue(list.containsInstance(12, elem2));
		Assert.assertFalse(list.containsInstance(9, elem1));
		Assert.assertFalse(list.containsInstance(13, elem1));
		Assert.assertFalse(list.containsInstance(10, Integer.valueOf(9)));
		Assert.assertFalse(list.containsInstance(10, Integer.valueOf(11)));
		Assert.assertFalse(list.containsInstance(12, Integer.valueOf(9)));
		Assert.assertFalse(list.containsInstance(12, Integer.valueOf(10)));
		Assert.assertFalse(list.containsInstance(11, Integer.valueOf(11)));
		list.clear();
		Assert.assertTrue(list.isEmpty());
		list.add(10, elem1);
		list.add(12, elem2);
		Assert.assertTrue(list.containsInstance(10, elem1));
		Assert.assertTrue(list.containsInstance(12, elem2));
		Assert.assertEquals(2, list.size());
	}
	
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
	public void testAddThenRemoveRandom() {
		Sorted.AssociatedWithLong<Object> list = createSorted();
		TreeSet<Long> order = new TreeSet<>();
		Random rand = new Random();
		for (int i = 0; i < 2000; ++i) {
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
	
	@Test
	public void testAddAndRemoveRandom() {
		Sorted.AssociatedWithLong<Object> list = createSorted();
		Random rand = new Random();
		LinkedList<Long> toAdd = new LinkedList<>();
		for (int i = 0; i < 2000; ++i) {
			Long value = Long.valueOf(rand.nextLong());
			while (toAdd.contains(value)) value = Long.valueOf(rand.nextLong());
			toAdd.add(value);
		}
		LinkedList<Long> toRemove = new LinkedList<>();
		TreeSet<Long> order = new TreeSet<>();
		do {
			if (!toAdd.isEmpty()) {
				int nb = rand.nextInt(toAdd.size()) + 1;
				for (int i = 0; i < nb; ++i) {
					Long value = toAdd.removeFirst();
					Assert.assertFalse(list.contains(value.longValue(), Long.valueOf(-value.longValue())));
					list.add(value.longValue(), Long.valueOf(-value.longValue()));
					Assert.assertTrue(list.contains(value.longValue(), Long.valueOf(-value.longValue())));
					order.add(value);
					toRemove.add(value);
				}
			}
			int nb = rand.nextInt(toRemove.size()) + 1;
			for (int i = 0; i < nb; ++i) {
				Long value = toRemove.remove(rand.nextInt(toRemove.size()));
				Assert.assertTrue(list.contains(value.longValue(), Long.valueOf(-value.longValue())));
				list.remove(value.longValue(), Long.valueOf(-value.longValue()));
				Assert.assertFalse(list.contains(value.longValue(), Long.valueOf(-value.longValue())));
				order.remove(value);
			}
			check(list, order);
		} while (!toAdd.isEmpty() || !toRemove.isEmpty());
		Assert.assertEquals(0, order.size());
		Assert.assertEquals(0, list.size());
	}
	
	
	protected void check(Sorted.AssociatedWithLong<Object> sorted, TreeSet<Long> order) {
		Assert.assertEquals(order.size(), sorted.size());
		if (order.isEmpty())
			Assert.assertFalse(sorted.contains(0, Long.valueOf(0)));
		for (Long i : order) {
			long value = i.longValue();
			Assert.assertTrue("contains(" + value + ") returned false", sorted.contains(value, Long.valueOf(-value)));
			if (sorted.size() < 100) {
				Long instance = null;
				for (Object o : sorted)
					if (((Long)o).longValue() == -value) {
						instance = (Long)o;
						break;
					}
				Assert.assertTrue(sorted.containsInstance(value, instance));
			}
		}
		if (!order.isEmpty()) {
			long val = order.last().longValue() + 1;
			Assert.assertFalse(sorted.contains(val, Long.valueOf(-val)));
			val = order.first().longValue() - 1;
			Assert.assertFalse(sorted.contains(val, Long.valueOf(-val)));
		}
		Assert.assertFalse(sorted.contains(-10, Long.valueOf(10)));
		Assert.assertFalse(sorted.contains(Long.MAX_VALUE, Long.valueOf(-Long.MAX_VALUE)));
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

		it1 = sorted.reverseOrderIterator();
		it2 = order.descendingIterator();
		nb = 0;
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
