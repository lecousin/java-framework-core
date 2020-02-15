package net.lecousin.framework.core.tests.collections;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import net.lecousin.framework.collections.CollectionsUtil;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestCollectionsUtil extends LCCoreAbstractTest {

	private static List<Integer> createList() {
		LinkedList<Integer> list = new LinkedList<>();
		list.add(Integer.valueOf(10));
		list.add(Integer.valueOf(20));
		list.add(Integer.valueOf(30));
		list.add(Integer.valueOf(40));
		list.add(Integer.valueOf(50));
		list.add(Integer.valueOf(60));
		list.add(Integer.valueOf(70));
		list.add(Integer.valueOf(80));
		list.add(Integer.valueOf(90));
		return list;
	}
	
	@Test
	public void testIteratorToEnumeration() {
		List<Integer> list = createList();
		Enumeration<Integer> e = CollectionsUtil.enumeration(list.iterator());
		Assert.assertTrue(e.hasMoreElements());
		Assert.assertEquals(10, e.nextElement().intValue());
		Assert.assertTrue(e.hasMoreElements());
		Assert.assertEquals(20, e.nextElement().intValue());
		Assert.assertTrue(e.hasMoreElements());
		Assert.assertEquals(30, e.nextElement().intValue());
		Assert.assertTrue(e.hasMoreElements());
		Assert.assertEquals(40, e.nextElement().intValue());
		Assert.assertTrue(e.hasMoreElements());
		Assert.assertEquals(50, e.nextElement().intValue());
		Assert.assertTrue(e.hasMoreElements());
		Assert.assertEquals(60, e.nextElement().intValue());
		Assert.assertTrue(e.hasMoreElements());
		Assert.assertEquals(70, e.nextElement().intValue());
		Assert.assertTrue(e.hasMoreElements());
		Assert.assertEquals(80, e.nextElement().intValue());
		Assert.assertTrue(e.hasMoreElements());
		Assert.assertEquals(90, e.nextElement().intValue());
		Assert.assertFalse(e.hasMoreElements());
		try {
			e.nextElement();
			throw new AssertionError("nextElement must throw a NoSuchElementException");
		} catch (NoSuchElementException ex) {
		}
	}
	
	@Test
	public void testEnumerationAsIterable() {
		List<Integer> list = createList();
		Enumeration<Integer> e = CollectionsUtil.enumeration(list.iterator());
		Iterable<Integer> i = CollectionsUtil.iterable(e);
		Iterator<Integer> it = i.iterator();
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(10, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(20, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(30, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(40, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(50, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(60, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(70, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(80, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(90, it.next().intValue());
		Assert.assertFalse(it.hasNext());
		try {
			it.next();
			throw new AssertionError("next must throw a NoSuchElementException");
		} catch (NoSuchElementException ex) {
		}
		it = i.iterator();
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(10, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(20, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(30, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(40, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(50, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(60, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(70, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(80, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(90, it.next().intValue());
		Assert.assertFalse(it.hasNext());
	}

	@Test
	public void testEnumerationAsSingleTimeIterable() {
		List<Integer> list = createList();
		Enumeration<Integer> e = CollectionsUtil.enumeration(list.iterator());
		Iterable<Integer> i = CollectionsUtil.singleTimeIterable(e);
		Iterator<Integer> it = i.iterator();
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(10, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(20, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(30, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(40, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(50, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(60, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(70, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(80, it.next().intValue());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(90, it.next().intValue());
		Assert.assertFalse(it.hasNext());
		try {
			it.next();
			throw new AssertionError("next must throw a NoSuchElementException");
		} catch (NoSuchElementException ex) {
		}
		it = i.iterator();
		Assert.assertFalse(it.hasNext());
	}
	
	@Test
	public void tests() {
		Assert.assertTrue(CollectionsUtil.equals(createList(), createList()));
		List<Integer> list = createList();
		list.add(Integer.valueOf(100));
		Assert.assertFalse(CollectionsUtil.equals(createList(), list));
		list = createList();
		list.remove(2);
		Assert.assertFalse(CollectionsUtil.equals(createList(), list));
		list = createList();
		list.set(2, Integer.valueOf(51));
		Assert.assertFalse(CollectionsUtil.equals(createList(), list));
		
		list = createList();
		CollectionsUtil.addAll(list, CollectionsUtil.enumeration(createList().iterator()));
		Assert.assertEquals(18, list.size());
	}
	
	@Test
	public void testMap() {
		List<Integer> input = createList();
		List<Long> output = CollectionsUtil.map(input, i -> Long.valueOf(i.longValue()));
		Assert.assertEquals(input.size(), output.size());
		for (int i = 0; i < input.size(); ++i) {
			Integer intValue = input.get(i);
			Object out = output.get(i);
			Assert.assertTrue(out instanceof Long);
			Assert.assertEquals(intValue.longValue(), ((Long)out).longValue());
		}
	}
	
	@Test
	public void testFindElement() {
		List<Integer> list = createList();
		for (Integer val : list)
			Assert.assertEquals(val, CollectionsUtil.findElement(list, i -> i.intValue() == val.intValue()));
		Assert.assertNull(CollectionsUtil.findElement(list, i -> false));
	}
	
	@Test
	public void testFilterSingle() {
		List<Integer> list = createList();
		for (Integer val : list)
			Assert.assertEquals(val, CollectionsUtil.filterSingle((Integer i) -> i.intValue() == val.intValue()).apply(list));
		Assert.assertNull(CollectionsUtil.filterSingle((Integer i) -> false).apply(list));
	}
	
	@Test
	public void testAddAllFromIterator() {
		LinkedList<Integer> target = new LinkedList<>();
		List<Integer> source = createList();
		CollectionsUtil.addAll(target, source.iterator());
		Assert.assertEquals(source.size(), target.size());
	}
	
	@Test
	public void testContainsInstance() {
		List<Integer> list = createList();
		Assert.assertTrue(CollectionsUtil.containsInstance(list, list.get(0)));
		Assert.assertTrue(CollectionsUtil.containsInstance(list, list.get(1)));
		Assert.assertFalse(CollectionsUtil.containsInstance(list, Integer.valueOf(-1)));
	}
	
}
