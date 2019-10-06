package net.lecousin.framework.core.tests.collections;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.core.test.collections.TestDeque;

public class TestTurnArray extends TestDeque {

	@Override
	public TurnArray<Long> createLongCollection() {
		return new TurnArray<Long>();
	}
	
	@Override
	public boolean supportRetainAll() {
		return false;
	}
	
	@Test(timeout=120000)
	public void testFullAndEmpty() {
		TurnArray<Long> q = new TurnArray<>(5);
		Assert.assertTrue(q.isEmpty());
		Assert.assertFalse(q.isFull());
		q.add(Long.valueOf(1));
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.add(Long.valueOf(2));
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.add(Long.valueOf(3));
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.add(Long.valueOf(4));
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.add(Long.valueOf(5));
		Assert.assertTrue(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.removeFirst();
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.add(Long.valueOf(6));
		Assert.assertTrue(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.removeFirst();
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.removeFirst();
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.add(Long.valueOf(7));
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.add(Long.valueOf(8));
		Assert.assertTrue(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.removeFirst();
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.removeFirst();
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.removeFirst();
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.removeFirst();
		Assert.assertFalse(q.isFull());
		Assert.assertFalse(q.isEmpty());
		q.removeFirst();
		Assert.assertFalse(q.isFull());
		Assert.assertTrue(q.isEmpty());
	}
	
	@SuppressWarnings("boxing")
	@Test(timeout=120000)
	public void testSpecificCases() {
		TurnArray<Long> q = new TurnArray<>(5);

		// make it full
		q.add(Long.valueOf(1));
		q.add(Long.valueOf(2));
		q.add(Long.valueOf(3));
		q.add(Long.valueOf(4));
		q.add(Long.valueOf(5));
		Assert.assertTrue(q.isFull());
		
		// case when full: getLast, ppekLast, removeLast, pollLast
		Assert.assertEquals(5, q.getLast().longValue());
		Assert.assertEquals(5, q.peekLast().longValue());
		Assert.assertEquals(5, q.removeLast().longValue());
		checkDeque(q, 1, 2, 3, 4);
		q.add(Long.valueOf(-1));
		checkDeque(q, 1, 2, 3, 4, -1);
		Assert.assertEquals(-1, q.pollLast().longValue());
		checkDeque(q, 1, 2, 3, 4);
		// removeLast and pollLast with end == 0
		q.add(Long.valueOf(6));
		checkDeque(q, 1, 2, 3, 4, 6);
		Assert.assertEquals(1, q.removeFirst().longValue());
		checkDeque(q, 2, 3, 4, 6);
		Assert.assertEquals(6, q.removeLast().longValue());
		checkDeque(q, 2, 3, 4);
		q.add(Long.valueOf(-1));
		checkDeque(q, 2, 3, 4, -1);
		Assert.assertEquals(-1, q.pollLast().longValue());
		checkDeque(q, 2, 3, 4);
		
		// test clear when full
		q.add(Long.valueOf(7));
		q.add(Long.valueOf(8));
		checkDeque(q, 2, 3, 4, 7, 8);
		Assert.assertTrue(q.isFull());
		q.clear();
		testQueueEmpty(q);

		// test clear when end < start
		q = new TurnArray<>(5);
		q.add(Long.valueOf(11));
		q.add(Long.valueOf(12));
		q.add(Long.valueOf(13));
		q.add(Long.valueOf(14));
		q.remove(Long.valueOf(11));
		q.remove(Long.valueOf(12));
		checkDeque(q, 13, 14);
		q.add(Long.valueOf(15));
		q.add(Long.valueOf(16));
		checkDeque(q, 13, 14, 15, 16);
		q.clear();
		testQueueEmpty(q);
		
		// increase when end < start
		q = new TurnArray<>(5);
		q.add(Long.valueOf(11));
		q.add(Long.valueOf(12));
		q.add(Long.valueOf(13));
		q.add(Long.valueOf(14));
		q.removeFirst();
		q.removeFirst();
		q.add(Long.valueOf(15));
		q.add(Long.valueOf(16));
		checkDeque(q, 13, 14, 15, 16);
		q.add(Long.valueOf(17));
		q.add(Long.valueOf(18));
		checkDeque(q, 13, 14, 15, 16, 17, 18);
		q.clear();
		testQueueEmpty(q);
		
		// increase when end > start
		q = new TurnArray<>(5);
		q.add(Long.valueOf(11));
		q.add(Long.valueOf(12));
		q.add(Long.valueOf(13));
		q.addAll(Arrays.asList(14L, 15L, 16L));
		checkDeque(q, 11, 12, 13, 14, 15, 16);
		
		try {
			new TurnArray<Integer>(-10);
			throw new AssertionError("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// ok
		}
	}
	
	@Test(timeout=120000)
	public void testIncreaseAndDecrease() {
		TurnArray<Long> q = new TurnArray<>(5);

		// make it full
		q.add(Long.valueOf(1));
		q.add(Long.valueOf(2));
		q.add(Long.valueOf(3));
		q.add(Long.valueOf(4));
		q.add(Long.valueOf(5));
		Assert.assertTrue(q.isFull());

		q.removeFirst();
		q.removeFirst();
		q.add(Long.valueOf(6));
		q.add(Long.valueOf(7));
		Assert.assertTrue(q.isFull());
		
		// increase
		for (int i = 8; i < 150; ++i)
			q.add(Long.valueOf(i));
		
		// decrease
		for (int i = 0; i < 140; ++i)
			q.removeFirst();
	}

	@SuppressWarnings("boxing")
	@Test(timeout=120000)
	public void testRemoveAny() {
		TurnArray<Long> q = new TurnArray<>(5);
		q.add(Long.valueOf(21));
		q.add(Long.valueOf(22));
		q.add(Long.valueOf(23));
		q.add(Long.valueOf(24));
		q.add(Long.valueOf(25));
		checkDeque(q, 21, 22, 23, 24, 25);
		Assert.assertTrue(q.removeAny(Long.valueOf(22)));
		Assert.assertFalse(q.removeAny(Long.valueOf(20)));
		checkDeque(q, 21, 23, 24, 25);
		Assert.assertTrue(q.removeAny(Long.valueOf(24)));
		Assert.assertFalse(q.removeAny(Long.valueOf(24)));
		checkDeque(q, 21, 23, 25);
		Assert.assertTrue(q.removeAny(Long.valueOf(21)));
		Assert.assertFalse(q.removeAny(Long.valueOf(21)));
		checkDeque(q, 23, 25);
		Assert.assertTrue(q.removeAny(Long.valueOf(25)));
		Assert.assertFalse(q.removeAny(Long.valueOf(25)));
		checkDeque(q, 23);
		Assert.assertTrue(q.removeAny(Long.valueOf(23)));
		Assert.assertFalse(q.removeAny(Long.valueOf(23)));
		testDequeEmpty(q);
		q.add(1L);
		q.add(2L);
		q.add(3L);
		q.removeFirst();
		q.removeFirst();
		q.add(4L);
		q.add(5L);
		q.add(6L);
		checkDeque(q, 3, 4, 5, 6);
		q.removeAny(5L);
		q.removeAny(3L);
		checkDeque(q, 4, 6);
		q.removeAny(3L);
		checkDeque(q, 4, 6);
		q.clear();
		q.add(1L);
		q.add(2L);
		q.add(3L);
		q.removeFirst();
		q.removeFirst();
		q.add(4L);
		q.add(5L);
		q.add(6L);
		checkDeque(q, 3, 4, 5, 6);
		q.removeAny(7L);
		checkDeque(q, 3, 4, 5, 6);
		q.add(7L);
		checkDeque(q, 3, 4, 5, 6, 7);
		q.removeAny(8L);
		checkDeque(q, 3, 4, 5, 6, 7);
	}

	@SuppressWarnings("boxing")
	@Test(timeout=120000)
	public void testAddAll() {
		TurnArray<Long> q = new TurnArray<>(5);
		q.addAll(Arrays.asList(1L, 2L, 3L));
		checkDeque(q, 1, 2, 3);
		q.removeFirst();
		q.removeFirst();
		checkDeque(q, 3);
		q.addAll(Arrays.asList(4L, 5L, 6L));
		checkDeque(q, 3, 4, 5, 6);
		q.removeFirst();
		q.removeFirst();
		checkDeque(q, 5, 6);
		q.addAll(Arrays.asList(7L, 8L, 9L));
		checkDeque(q, 5, 6, 7, 8, 9);
		q.removeFirst();
		q.removeFirst();
		checkDeque(q, 7, 8, 9);
		q.addAll(Arrays.asList(10L, 11L, 12L));
		checkDeque(q, 7, 8, 9, 10, 11, 12);
		q.clear();
	}

	@SuppressWarnings("boxing")
	@Test(timeout=120000)
	public void testRemoveFirstOccurence() {
		TurnArray<Long> q = new TurnArray<>(5);
		q.add(1L);
		q.add(2L);
		q.add(1L);
		q.removeFirstOccurrence(1L);
		checkDeque(q, 2, 1);
		q.removeFirstOccurrence(3L);
		checkDeque(q, 2, 1);
		q.removeFirstOccurrence(3L);
		checkDeque(q, 2, 1);
		q.add(3L);
		q.add(4L);
		q.add(5L);
		q.removeFirstOccurrence(3L);
		checkDeque(q, 2, 1, 4, 5);
		q.add(1L);
		checkDeque(q, 2, 1, 4, 5, 1);
		q.removeFirstOccurrence(3L);
		checkDeque(q, 2, 1, 4, 5, 1);
		q.removeFirstOccurrence(1L);
		checkDeque(q, 2, 4, 5, 1);
		q.add(5L);
		checkDeque(q, 2, 4, 5, 1, 5);
		q.removeFirst();
		checkDeque(q, 4, 5, 1, 5);
		q.add(6L);
		checkDeque(q, 4, 5, 1, 5, 6);
		q.removeFirstOccurrence(6L);
		checkDeque(q, 4, 5, 1, 5);
		q.removeFirstOccurrence(6L);
		checkDeque(q, 4, 5, 1, 5);
		q = new TurnArray<>(5);
		q.add(1L);
		q.add(2L);
		q.add(3L);
		q.add(4L);
		q.removeFirst();
		q.removeFirst();
		q.add(3L);
		q.add(4L);
		checkDeque(q, 3, 4, 3, 4);
		q.removeFirstOccurrence(4L);
		checkDeque(q, 3, 3, 4);
		q.removeFirstOccurrence(4L);
		checkDeque(q, 3, 3);
		q = new TurnArray<>(5);
		q.add(1L);
		q.add(2L);
		q.add(3L);
		q.add(4L);
		q.removeFirst();
		q.removeFirst();
		q.add(5L);
		q.add(6L);
		checkDeque(q, 3, 4, 5, 6);
		q.removeFirstOccurrence(6L);
		checkDeque(q, 3, 4, 5);
	}

	@SuppressWarnings("boxing")
	@Test(timeout=120000)
	public void testRemoveLastOccurence() {
		TurnArray<Long> q = new TurnArray<>(5);
		q.add(1L);
		q.add(2L);
		q.add(1L);
		q.removeLastOccurrence(1L);
		checkDeque(q, 1, 2);
		q.removeLastOccurrence(3L);
		checkDeque(q, 1, 2);
		q = new TurnArray<>(5);
		q.add(1L);
		q.add(2L);
		q.add(1L);
		q.add(3L);
		q.add(2L);
		q.removeLastOccurrence(1L);
		checkDeque(q, 1, 2, 3, 2);
		q.removeLastOccurrence(4L);
		checkDeque(q, 1, 2, 3, 2);
		q.removeFirst();
		q.add(3L);
		checkDeque(q, 2, 3, 2, 3);
		q.removeLastOccurrence(4L);
		checkDeque(q, 2, 3, 2, 3);
		q.removeLastOccurrence(2L);
		checkDeque(q, 2, 3, 3);
		q.add(4L);
		q.add(4L);
		checkDeque(q, 2, 3, 3, 4, 4);
		q.removeFirst();
		q.add(2L);
		checkDeque(q, 3, 3, 4, 4, 2);
		q.removeLastOccurrence(4L);
		checkDeque(q, 3, 3, 4, 2);
		q = new TurnArray<>(5);
		q.add(1L);
		q.add(2L);
		q.add(1L);
		q.add(3L);
		q.add(2L);
		q.removeFirst();
		q.removeFirst();
		q.add(3L);
		q.add(2L);
		checkDeque(q, 1, 3, 2, 3, 2);
		q.removeLastOccurrence(4L);
		checkDeque(q, 1, 3, 2, 3, 2);
		q.removeLastOccurrence(3L);
		checkDeque(q, 1, 3, 2, 2);
		q = new TurnArray<>(5);
		q.add(1L);
		q.add(2L);
		q.add(1L);
		q.add(3L);
		q.add(2L);
		q.removeFirst();
		q.removeFirst();
		q.add(1L);
		checkDeque(q, 1, 3, 2, 1);
		q.removeLastOccurrence(4L);
		checkDeque(q, 1, 3, 2, 1);
		q.removeLastOccurrence(1L);
		checkDeque(q, 1, 3, 2);
	}
	
	@Test(timeout=120000)
	public void testRemoveInstance() {
		TurnArray<Object> q = new TurnArray<>(5);
		Object o1 = new Object();
		Object o2 = new Object();
		Object o3 = new Object();
		Object o4 = new Object();
		Object o5 = new Object();
		q.add(o1);
		q.add(o2);
		q.add(o3);
		q.removeFirst();
		q.add(o4);
		q.add(o5);
		checkDeque(q, o2, o3, o4, o5);
		q.removeFirst();
		q.add(o1);
		checkDeque(q, o3, o4, o5, o1);
		q.removeInstance(o2);
		checkDeque(q, o3, o4, o5, o1);
		q.removeInstance(o1);
		checkDeque(q, o3, o4, o5);
		
		q = new TurnArray<>(5);
		q.add(o1);
		q.add(o2);
		q.add(o3);
		q.removeFirst();
		q.add(o4);
		q.add(o5);
		checkDeque(q, o2, o3, o4, o5);
		q.removeFirst();
		q.add(o1);
		q.add(o2);
		checkDeque(q, o3, o4, o5, o1, o2);
		q.removeInstance(o4);
		checkDeque(q, o3, o5, o1, o2);
	}
	
	@Test(timeout=120000)
	public void testRemoveAllNoOrder() {
		TurnArray<Object> q = new TurnArray<>(5);
		Object o1 = new Object();
		Object o2 = new Object();
		Object o3 = new Object();
		Object o4 = new Object();
		Object o5 = new Object();
		List<Object> list;

		q.add(o1);
		q.add(o2);
		list = q.removeAllNoOrder();
		Assert.assertEquals(2, list.size());
		Assert.assertTrue(list.contains(o1));
		Assert.assertTrue(list.contains(o2));
		Assert.assertTrue(q.isEmpty());

		q.add(o1);
		q.add(o2);
		q.add(o3);
		q.add(o4);
		q.add(o5);
		list = q.removeAllNoOrder();
		Assert.assertEquals(5, list.size());
		Assert.assertTrue(list.contains(o1));
		Assert.assertTrue(list.contains(o2));
		Assert.assertTrue(list.contains(o3));
		Assert.assertTrue(list.contains(o4));
		Assert.assertTrue(list.contains(o5));
		Assert.assertTrue(q.isEmpty());
		list = q.removeAllNoOrder();
		Assert.assertEquals(0, list.size());
		Assert.assertTrue(q.isEmpty());

		q.add(o1);
		q.add(o2);
		q.add(o3);
		q.add(o4);
		q.add(o5);
		q.removeFirst();
		q.removeFirst();
		q.add(o1);
		q.toArray(new Object[10]);
		list = q.removeAllNoOrder();
		Assert.assertEquals(4, list.size());
		Assert.assertTrue(list.contains(o1));
		Assert.assertTrue(list.contains(o3));
		Assert.assertTrue(list.contains(o4));
		Assert.assertTrue(list.contains(o5));
		Assert.assertTrue(q.isEmpty());


		q.add(o1);
		q.add(o2);
		q.add(o3);
		q.add(o4);
		q.add(o5);
		q.toArray(new Object[10]);
		q.removeFirst();
		q.toArray(new Object[10]);
		list = q.removeAllNoOrder();
		Assert.assertEquals(4, list.size());
		Assert.assertTrue(list.contains(o2));
		Assert.assertTrue(list.contains(o3));
		Assert.assertTrue(list.contains(o4));
		Assert.assertTrue(list.contains(o5));
		Assert.assertTrue(q.isEmpty());
	}
	
}
