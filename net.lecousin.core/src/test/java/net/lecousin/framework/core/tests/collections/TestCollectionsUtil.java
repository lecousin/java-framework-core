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
	
	@Test(timeout=30000)
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
	
	@Test(timeout=30000)
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

	@Test(timeout=30000)
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
	
	@Test(timeout=30000)
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
	
}
