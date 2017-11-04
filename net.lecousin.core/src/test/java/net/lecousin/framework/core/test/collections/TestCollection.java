package net.lecousin.framework.core.test.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;

public abstract class TestCollection extends LCCoreAbstractTest {

	public abstract Collection<Long> createLongCollection();
	
	public boolean supportRetainAll() { return true; }

	@Test
	public void testCollection() {
		Collection<Long> c = createLongCollection();
		testCollectionEmpty(c);

		// add 1 element
		c.add(Long.valueOf(1));
		checkCollection(c, 1);

		// remove non present element
		Assert.assertFalse(c.remove(Long.valueOf(0)));
		checkCollection(c, 1);
		
		// remove element
		Assert.assertTrue(c.remove(Long.valueOf(1)));
		testCollectionEmpty(c);

		// add 3 elements
		Assert.assertTrue(c.add(Long.valueOf(10)));
		checkCollection(c, 10);
		Assert.assertTrue(c.add(Long.valueOf(11)));
		checkCollection(c, 10, 11);
		Assert.assertTrue(c.add(Long.valueOf(12)));
		checkCollection(c, 10, 11, 12);

		// remove non present element
		Assert.assertFalse(c.remove(Long.valueOf(111)));
		checkCollection(c, 10, 11, 12);
		// remove 1 element
		Assert.assertTrue(c.remove(Long.valueOf(11)));
		checkCollection(c, 10, 12);
		
		ArrayList<Long> arr = new ArrayList<>(10);
		arr.add(Long.valueOf(13));
		arr.add(Long.valueOf(14));
		arr.add(Long.valueOf(15));
		
		// test addAll
		c.addAll(arr);
		checkCollection(c, 10, 12, 13, 14, 15);
		arr.clear();
		c.addAll(arr);
		checkCollection(c, 10, 12, 13, 14, 15);
		arr.add(Long.valueOf(21));
		arr.add(Long.valueOf(22));
		arr.add(Long.valueOf(23));
		arr.add(Long.valueOf(24));
		arr.add(Long.valueOf(25));
		arr.add(Long.valueOf(26));
		arr.add(Long.valueOf(27));
		arr.add(Long.valueOf(28));
		arr.add(Long.valueOf(29));
		c.addAll(arr);
		checkCollection(c, 10, 12, 13, 14, 15, 21, 22, 23, 24, 25, 26, 27, 28, 29);
		
		// test removeAll
		arr.clear();
		arr.add(Long.valueOf(12));
		arr.add(Long.valueOf(14));
		c.removeAll(arr);
		checkCollection(c, 10, 13, 15, 21, 22, 23, 24, 25, 26, 27, 28, 29);
		arr.add(Long.valueOf(12));
		arr.add(Long.valueOf(14));
		arr.add(Long.valueOf(22));
		arr.add(Long.valueOf(24));
		c.removeAll(arr);
		checkCollection(c, 10, 13, 15, 21, 23, 25, 26, 27, 28, 29);
		
		// remove
		Assert.assertTrue(c.remove(Long.valueOf(25)));
		Assert.assertFalse(c.remove(Long.valueOf(35)));
		Assert.assertTrue(c.remove(Long.valueOf(10)));
		Assert.assertTrue(c.remove(Long.valueOf(29)));
		Assert.assertFalse(c.remove(Long.valueOf(10)));
		checkCollection(c, 13, 15, 21, 23, 26, 27, 28);
		
		// test retainAll
		arr.clear();
		arr.add(Long.valueOf(10));
		arr.add(Long.valueOf(15));
		arr.add(Long.valueOf(16));
		arr.add(Long.valueOf(26));
		if (supportRetainAll()) {
			c.retainAll(arr);
			checkCollection(c, 15, 26);
		} else {
			assertException(() -> { c.retainAll(arr); }, UnsupportedOperationException.class);
			c.remove(Long.valueOf(13));
			c.remove(Long.valueOf(21));
			c.remove(Long.valueOf(23));
			c.remove(Long.valueOf(27));
			c.remove(Long.valueOf(28));
			checkCollection(c, 15, 26);
		}
		
		// test clear
		c.clear();
		testCollectionEmpty(c);
		c.clear();
		testCollectionEmpty(c);
		
		// test to add many elements
		for (long i = 1000; i < 15000; ++i)
			Assert.assertTrue(c.add(Long.valueOf(i)));
		Assert.assertTrue(c.size() == 14000);
		for (long i = 1000; i < 15000; ++i)
			Assert.assertTrue(c.contains(Long.valueOf(i)));
		
		// test to remove half of elements
		for (long i = 1000; i < 15000; i += 2)
			Assert.assertTrue(c.remove(Long.valueOf(i)));
		Assert.assertTrue(c.size() == 7000);
		for (long i = 1000; i < 15000; ++i)
			if ((i % 2) == 0)
				Assert.assertFalse(c.contains(Long.valueOf(i)));
			else
				Assert.assertTrue(c.contains(Long.valueOf(i)));
		
		// test to remove elements from 10000 to 13000 (1500 elements)
		for (long i = 10000; i < 13000; ++i)
			if ((i % 2) == 0)
				Assert.assertFalse(c.remove(Long.valueOf(i)));
			else
				Assert.assertTrue(c.remove(Long.valueOf(i)));
		Assert.assertTrue(c.size() == 5500);
		
		// test to remove elements from 14000 to 15000 (500 elements)
		for (long i = 14000; i < 15000; ++i)
			if ((i % 2) == 0)
				Assert.assertFalse(c.remove(Long.valueOf(i)));
			else
				Assert.assertTrue(c.remove(Long.valueOf(i)));
		Assert.assertTrue(c.size() == 5000);
		
		arr.clear();
		for (Long e : c) arr.add(e);
		Assert.assertTrue(arr.size() == 5000);
		Assert.assertTrue(c.containsAll(arr));
		arr.add(Long.valueOf(0));
		Assert.assertFalse(c.containsAll(arr));
		arr.remove(Long.valueOf(0));
		for (long i = 1000; i < 2000; ++i)
			arr.remove(Long.valueOf(i));
		Assert.assertTrue(c.containsAll(arr));
		
		// retain only between 1000 and 2000, half of them are already removed
		if (supportRetainAll()) {
			arr.clear();
			for (long i = 1000; i < 2000; ++i)
				arr.add(Long.valueOf(i));
			Assert.assertTrue(c.retainAll(arr));
			Assert.assertTrue(c.size() == 500);
			for (long i = 1000; i < 2000; ++i)
				if ((i % 2) == 0)
					Assert.assertFalse(c.contains(Long.valueOf(i)));
				else
					Assert.assertTrue(c.contains(Long.valueOf(i)));
		}
	}
	
	protected static void testCollectionEmpty(Collection<Long> c) {
		Assert.assertTrue(c.isEmpty());
		Assert.assertEquals(c.size(), 0);
		
		Assert.assertFalse(c.iterator().hasNext());
		assertException(() -> { c.iterator().next(); }, NoSuchElementException.class);

		Assert.assertFalse(c.remove(Long.valueOf(1)));
		Assert.assertFalse(c.removeAll(Arrays.asList(Long.valueOf(1), Long.valueOf(2))));
		
		Assert.assertFalse(c.contains(Long.valueOf(1)));
		Assert.assertFalse(c.containsAll(Arrays.asList(Long.valueOf(1), Long.valueOf(2))));
		
		Assert.assertArrayEquals(c.toArray(), new Long[0]);
		Assert.assertArrayEquals(c.toArray(new Long[0]), new Long[0]);
		Assert.assertArrayEquals(c.toArray(
			new Long[] { Long.valueOf(23), Long.valueOf(45) }),
			new Long[] { Long.valueOf(23), Long.valueOf(45) });
	}
	
	protected static void checkCollection(Collection<Long> c, long... values) {
		// size
		Assert.assertFalse(c.isEmpty());
		Assert.assertEquals(values.length, c.size());

		// contains
		for (int i = 0; i < values.length; ++i)
			Assert.assertTrue(c.contains(Long.valueOf(values[i])));
		Assert.assertFalse(c.contains(Long.valueOf(123456789)));
		
		// iterator
		Iterator<Long> it = c.iterator();
		boolean[] found = new boolean[values.length];
		for (int i = 0; i < values.length; ++i) found[i] = false;
		for (int i = 0; i < values.length; ++i) {
			Assert.assertTrue(it.hasNext());
			long val = it.next().longValue();
			boolean valFound = false;
			for (int j = 0; j < found.length; ++j) {
				if (found[j]) continue;
				if (values[j] == val) {
					found[j] = true;
					valFound = true;
					break;
				}
			}
			if (!valFound) throw new AssertionError("Value " + val + " returned by the iterator is not expected in the collection");
		}
		for (int i = 0; i < found.length; ++i)
			if (!found[i])
				throw new AssertionError("Value " + values[i] + " was never returned by the iterator");
		Assert.assertFalse(it.hasNext());
		assertException(() -> { it.next(); }, NoSuchElementException.class);
		
		// to array
		Object[] a = c.toArray();
		Assert.assertEquals(a.length, values.length);
		for (int i = 0; i < values.length; ++i) found[i] = false;
		for (int i = 0; i < values.length; ++i) {
			Assert.assertTrue(a[i] instanceof Long);
			long val = ((Long)a[i]).longValue();
			boolean valFound = false;
			for (int j = 0; j < found.length; ++j) {
				if (found[j]) continue;
				if (values[j] == val) {
					found[j] = true;
					valFound = true;
					break;
				}
			}
			if (!valFound) throw new AssertionError("Value " + val + " returned by toArray is not expected in the collection");
		}
		for (int i = 0; i < found.length; ++i)
			if (!found[i])
				throw new AssertionError("Value " + values[i] + " was not in the result of toArray");
		
		// toArray too small
		a = c.toArray(new Long[values.length - 1]);
		Assert.assertEquals(a.length, values.length);
		for (int i = 0; i < values.length; ++i) found[i] = false;
		for (int i = 0; i < values.length; ++i) {
			Assert.assertTrue(a[i] instanceof Long);
			long val = ((Long)a[i]).longValue();
			boolean valFound = false;
			for (int j = 0; j < found.length; ++j) {
				if (found[j]) continue;
				if (values[j] == val) {
					found[j] = true;
					valFound = true;
					break;
				}
			}
			if (!valFound) throw new AssertionError("Value " + val + " returned by toArray is not expected in the collection");
		}
		for (int i = 0; i < found.length; ++i)
			if (!found[i])
				throw new AssertionError("Value " + values[i] + " was not in the result of toArray");
		
		// toArray bigger
		a = c.toArray(new Long[values.length + 10]);
		Assert.assertEquals(a.length, values.length + 10);
		for (int i = 0; i < values.length; ++i) found[i] = false;
		for (int i = 0; i < values.length; ++i) {
			Assert.assertTrue(a[i] instanceof Long);
			long val = ((Long)a[i]).longValue();
			boolean valFound = false;
			for (int j = 0; j < found.length; ++j) {
				if (found[j]) continue;
				if (values[j] == val) {
					found[j] = true;
					valFound = true;
					break;
				}
			}
			if (!valFound) throw new AssertionError("Value " + val + " returned by toArray is not expected in the collection");
		}
		for (int i = 0; i < found.length; ++i)
			if (!found[i])
				throw new AssertionError("Value " + values[i] + " was not in the result of toArray");
	}
}
