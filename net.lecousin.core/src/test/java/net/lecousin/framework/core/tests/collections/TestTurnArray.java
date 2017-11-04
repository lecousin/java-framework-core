package net.lecousin.framework.core.tests.collections;

import org.junit.Assert;
import org.junit.Test;

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
	
	@Test
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
	
	@Test
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
		q.add(Long.valueOf(11));
		q.add(Long.valueOf(12));
		q.add(Long.valueOf(13));
		q.add(Long.valueOf(14));
		q.remove(Long.valueOf(11));
		q.remove(Long.valueOf(12));
		checkDeque(q, 13, 14);
		q.clear();
		testQueueEmpty(q);
		
		// removeAny
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
	}
	
}
