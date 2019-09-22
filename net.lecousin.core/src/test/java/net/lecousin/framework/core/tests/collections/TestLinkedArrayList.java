package net.lecousin.framework.core.tests.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.CollectionsUtil;
import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.core.test.collections.TestList;

public class TestLinkedArrayList extends TestList {

	@Override
	public LinkedArrayList<Long> createLongCollection() {
		return new LinkedArrayList<Long>(5);
	}
	
	@Test(timeout=120000)
	public void testLong() {
		LinkedArrayList<Long> l = createLongCollection();
		check(l);
		try {
			l.getlong(10);
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		try {
			l.removelong(10);
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		Assert.assertNull(l.removeLast());
		Assert.assertEquals(0, l.removeFirstArray(Long.class).length);
		
		l.addlong(0, Long.valueOf(1));
		l.addlong(1, Long.valueOf(2));
		l.addlong(2, Long.valueOf(3));
		check(l, 1, 2, 3);

		try {
			l.getlong(-10);
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		try {
			l.removelong(-10);
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		try {
			l.getlong(3);
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		try {
			l.removelong(3);
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		
		Assert.assertEquals(Long.valueOf(2), l.removelong(1));
		Assert.assertTrue(l.size() == 2);
		Assert.assertEquals(Long.valueOf(1), l.getlong(0));
		Assert.assertEquals(Long.valueOf(3), l.getlong(1));

		Assert.assertEquals(Long.valueOf(3), l.removeLast());
		Assert.assertTrue(l.longsize() == 1);
		Assert.assertEquals(Long.valueOf(1), l.getlong(0));
		
		l = createLongCollection();
		l.add(0, Long.valueOf(1));
		l.add(1, Long.valueOf(2));
		l.add(10, Long.valueOf(3));
		l.add(3, Long.valueOf(4));
		l.add(4, Long.valueOf(5));
		l.add(5, Long.valueOf(6));
		l.add(6, Long.valueOf(7));
		l.add(7, Long.valueOf(8));
		l.add(8, Long.valueOf(9));
		l.add(9, Long.valueOf(10));
		l.add(10, Long.valueOf(11));
		check(l, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
		l.add(5, Long.valueOf(55));
		check(l, 1, 2, 3, 4, 5, 55, 6, 7, 8, 9, 10, 11);
		l.add(10, Long.valueOf(100));
		check(l, 1, 2, 3, 4, 5, 55, 6, 7, 8, 9, 100, 10, 11);
		l.add(100, Long.valueOf(-1));
		check(l, 1, 2, 3, 4, 5, 55, 6, 7, 8, 9, 100, 10, 11, -1);
		l.add(0, Long.valueOf(-10));
		check(l, -10, 1, 2, 3, 4, 5, 55, 6, 7, 8, 9, 100, 10, 11, -1);
		l.add(1, Long.valueOf(11));
		check(l, -10, 11, 1, 2, 3, 4, 5, 55, 6, 7, 8, 9, 100, 10, 11, -1);
		l.addlong(0, Long.valueOf(1111));
		check(l, 1111, -10, 11, 1, 2, 3, 4, 5, 55, 6, 7, 8, 9, 100, 10, 11, -1);
		l.addlong(8, Long.valueOf(2222));
		check(l, 1111, -10, 11, 1, 2, 3, 4, 5, 2222, 55, 6, 7, 8, 9, 100, 10, 11, -1);
		l.addlong(100, Long.valueOf(3333));
		check(l, 1111, -10, 11, 1, 2, 3, 4, 5, 2222, 55, 6, 7, 8, 9, 100, 10, 11, -1, 3333);

		l.removelong(12);
		check(l, 1111, -10, 11, 1, 2, 3, 4, 5, 2222, 55, 6, 7, 9, 100, 10, 11, -1, 3333);
		Assert.assertEquals(Long.valueOf(3333), l.removeLast());
		check(l, 1111, -10, 11, 1, 2, 3, 4, 5, 2222, 55, 6, 7, 9, 100, 10, 11, -1);
		Assert.assertEquals(Long.valueOf(-1), l.removeLast());
		check(l, 1111, -10, 11, 1, 2, 3, 4, 5, 2222, 55, 6, 7, 9, 100, 10, 11);
		Assert.assertEquals(Long.valueOf(11), l.removeLast());
		check(l, 1111, -10, 11, 1, 2, 3, 4, 5, 2222, 55, 6, 7, 9, 100, 10);
		Assert.assertEquals(Long.valueOf(10), l.removeLast());
		check(l, 1111, -10, 11, 1, 2, 3, 4, 5, 2222, 55, 6, 7, 9, 100);
		l.removeFirstArray(Long.class);
		check(l, 3, 4, 5, 2222, 55, 6, 7, 9, 100);
		l.removeFirstArray(Long.class);
		check(l, 6, 7, 9, 100);
		l.removeFirstArray(Long.class);
		check(l);
		
		l.add(Long.valueOf(51));
		check(l, 51);
		Assert.assertEquals(Long.valueOf(51), l.removeLast());
		check(l);
		
		Long[] array = new Long[] { Long.valueOf(10), Long.valueOf(20), Long.valueOf(30) };
		l.insertFirstArray(array, 3, 0);
		check(l);
		l.insertFirstArray(array, 1, 1);
		check(l, 20);
		l.insertFirstArray(array, 0, 3);
		check(l, 10, 20, 30, 20);
		array = new Long[] {
			Long.valueOf(10),
			Long.valueOf(20),
			Long.valueOf(30),
			Long.valueOf(100),
			Long.valueOf(200),
			Long.valueOf(300),
			Long.valueOf(1000),
			Long.valueOf(2000),
			Long.valueOf(3000),
			Long.valueOf(10000),
			Long.valueOf(20000),
			Long.valueOf(30000),
		};
		l.insertFirstArray(array, 0, array.length);
		check(l, 10, 20, 30, 100, 200, 300, 1000, 2000, 3000, 10000, 20000, 30000, 10, 20, 30, 20);
		
		ListIterator<Long> lit = l.listIterator(0);
		Assert.assertTrue(lit.hasNext());
		Assert.assertFalse(lit.hasPrevious());
		Assert.assertEquals(10, lit.next().longValue());
		lit = l.listIterator(100);
		Assert.assertFalse(lit.hasNext());
		lit = l.listIterator(7);
		Assert.assertTrue(lit.hasNext());
		Assert.assertEquals(2000, lit.next().longValue());
		Assert.assertEquals(2000, lit.previous().longValue());
		Assert.assertEquals(1000, lit.previous().longValue());
		Assert.assertEquals(300, lit.previous().longValue());
		Assert.assertEquals(200, lit.previous().longValue());
		Assert.assertEquals(100, lit.previous().longValue());
		Assert.assertEquals(30, lit.previous().longValue());
		Assert.assertEquals(20, lit.previous().longValue());
		Assert.assertEquals(10, lit.previous().longValue());
		Assert.assertFalse(lit.hasPrevious());
		lit = l.listIterator(13);
		Assert.assertTrue(lit.hasNext());
		Assert.assertEquals(20, lit.next().longValue());
		Assert.assertEquals(30, lit.next().longValue());
		Assert.assertEquals(20, lit.next().longValue());
		Assert.assertFalse(lit.hasNext());
		
		l = createLongCollection();
		lit = l.listIterator(10);
		Assert.assertFalse(lit.hasNext());
		Assert.assertFalse(lit.hasPrevious());
	}
	
	@Override
	protected void check(List<Long> l, long... expectedValues) {
		super.check(l, expectedValues);
		if (!(l instanceof LinkedArrayList)) return;
		for (int i = 0; i < expectedValues.length; ++i) {
			Assert.assertEquals(Long.valueOf(expectedValues[i]), ((LinkedArrayList<Long>)l).getlong(i));
		}
	}
	
	@Test(timeout=30000)
	public void testAddLongOutOfRange() {
		LinkedArrayList<Long> l = new LinkedArrayList<>(2);
		ArrayList<Long> l2 = new ArrayList<>();
		l.add(Long.valueOf(10));
		l2.add(Long.valueOf(10));
		l.add(Long.valueOf(20));
		l2.add(Long.valueOf(20));
		l.add(Long.valueOf(30));
		l2.add(Long.valueOf(30));
		Assert.assertTrue(CollectionsUtil.equals(l, l2));
		for (long val = 100; val < 200; val++) {
			try {
				l.addlong(1000L, Long.valueOf(val));
				l2.add(Long.valueOf(val));
			} catch (IndexOutOfBoundsException e) {
				// ok
			}
			Assert.assertTrue(CollectionsUtil.equals(l, l2));
		}
	}

	@Test(timeout=30000)
	public void testAppendArray() {
		LinkedArrayList<Long> l = new LinkedArrayList<>(2);
		for (long val = 1; val <= 20; val++)
			l.add(Long.valueOf(val));
		Long[] arr = new Long[100];
		for (int i = 0; i < 100; ++i)
			arr[i] = Long.valueOf(1000 + i);
		l.appendArray(arr, 20, 50);
		l.appendArray(arr, 0, 1);
		l.appendArray(arr, 0, 1);
		Assert.assertEquals(72, l.size());
		for (int i = 0; i < 20; ++i)
			Assert.assertEquals(Long.valueOf(i + 1), l.get(i));
		for (int i = 20; i < 70; ++i)
			Assert.assertEquals(Long.valueOf(1000 + i), l.get(i));
		Assert.assertEquals(Long.valueOf(1000), l.get(70));
		Assert.assertEquals(Long.valueOf(1000), l.get(71));
	}

}
