package net.lecousin.framework.core.test.collections;

import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestDeque extends TestQueueFIFO {

	@Override
	public abstract Deque<Long> createLongCollection();
	
	@Test
	public void testDeque() {
		Deque<Long> c = createLongCollection();
		testDequeEmpty(c);
		
		// addFirst, removeFirst
		c.addFirst(Long.valueOf(1));
		c.addFirst(Long.valueOf(2));
		c.addFirst(Long.valueOf(3));
		checkDeque(c, 3, 2, 1);
		Assert.assertTrue(c.removeFirst().longValue() == 3);
		checkDeque(c, 2, 1);
		Assert.assertTrue(c.removeFirst().longValue() == 2);
		checkDeque(c, 1);
		Assert.assertTrue(c.removeFirst().longValue() == 1);
		testDequeEmpty(c);
		
		// addFirst, removeLast
		c.addFirst(Long.valueOf(11));
		c.addFirst(Long.valueOf(12));
		c.addFirst(Long.valueOf(13));
		c.addFirst(Long.valueOf(14));
		c.addFirst(Long.valueOf(15));
		c.addFirst(Long.valueOf(16));
		c.addFirst(Long.valueOf(17));
		c.addFirst(Long.valueOf(18));
		c.addFirst(Long.valueOf(19));
		checkDeque(c, 19, 18, 17, 16, 15, 14, 13, 12, 11);
		Assert.assertTrue(c.removeLast().longValue() == 11);
		checkDeque(c, 19, 18, 17, 16, 15, 14, 13, 12);
		Assert.assertTrue(c.removeLast().longValue() == 12);
		checkDeque(c, 19, 18, 17, 16, 15, 14, 13);
		Assert.assertTrue(c.removeFirst().longValue() == 19);
		checkDeque(c, 18, 17, 16, 15, 14, 13);
		Assert.assertTrue(c.removeLast().longValue() == 13);
		checkDeque(c, 18, 17, 16, 15, 14);
		Assert.assertTrue(c.removeFirst().longValue() == 18);
		checkDeque(c, 17, 16, 15, 14);
		
		// addLast
		c.addLast(Long.valueOf(21));
		c.addLast(Long.valueOf(22));
		c.addLast(Long.valueOf(23));
		checkDeque(c, 17, 16, 15, 14, 21, 22, 23);
		Assert.assertTrue(c.removeFirst().longValue() == 17);
		checkDeque(c, 16, 15, 14, 21, 22, 23);
		Assert.assertTrue(c.removeLast().longValue() == 23);
		checkDeque(c, 16, 15, 14, 21, 22);
		
		// offerFirst, offerLast
		c.offerFirst(Long.valueOf(31));
		c.offerFirst(Long.valueOf(32));
		c.offerFirst(Long.valueOf(33));
		checkDeque(c, 33, 32, 31, 16, 15, 14, 21, 22);
		c.offerLast(Long.valueOf(34));
		c.offerLast(Long.valueOf(35));
		c.offerLast(Long.valueOf(36));
		checkDeque(c, 33, 32, 31, 16, 15, 14, 21, 22, 34, 35, 36);
		
		// push, pop
		c.push(Long.valueOf(-1));
		c.push(Long.valueOf(-2));
		checkDeque(c, -2, -1, 33, 32, 31, 16, 15, 14, 21, 22, 34, 35, 36);
		Assert.assertEquals(-2, c.pop().longValue());
		Assert.assertEquals(-1, c.pop().longValue());
		checkDeque(c, 33, 32, 31, 16, 15, 14, 21, 22, 34, 35, 36);
		
		// pollFirst, pollLast
		Assert.assertTrue(c.pollFirst().longValue() == 33);
		Assert.assertTrue(c.pollFirst().longValue() == 32);
		Assert.assertTrue(c.pollLast().longValue() == 36);
		Assert.assertTrue(c.pollLast().longValue() == 35);
		checkDeque(c, 31, 16, 15, 14, 21, 22, 34);
		
		// add some more
		c.addFirst(Long.valueOf(41));
		c.addFirst(Long.valueOf(42));
		c.offerFirst(Long.valueOf(43));
		c.addFirst(Long.valueOf(44));
		c.offerFirst(Long.valueOf(45));
		c.offerFirst(Long.valueOf(46));
		c.addFirst(Long.valueOf(47));
		c.offerFirst(Long.valueOf(48));
		c.addFirst(Long.valueOf(49));
		checkDeque(c, 49, 48, 47, 46, 45, 44, 43, 42, 41, 31, 16, 15, 14, 21, 22, 34);
		c.addLast(Long.valueOf(51));
		c.addLast(Long.valueOf(52));
		c.offerLast(Long.valueOf(53));
		c.addLast(Long.valueOf(54));
		c.offerLast(Long.valueOf(55));
		c.offerLast(Long.valueOf(56));
		c.addLast(Long.valueOf(57));
		c.offerLast(Long.valueOf(58));
		c.addLast(Long.valueOf(59));
		checkDeque(c, 49, 48, 47, 46, 45, 44, 43, 42, 41, 31, 16, 15, 14, 21, 22, 34, 51, 52, 53, 54, 55, 56, 57, 58, 59);
		
		// various remove
		Assert.assertTrue(c.removeFirst().longValue() == 49);
		Assert.assertTrue(c.removeLast().longValue() == 59);
		Assert.assertTrue(c.pollFirst().longValue() == 48);
		Assert.assertTrue(c.pollLast().longValue() == 58);
		checkDeque(c, 47, 46, 45, 44, 43, 42, 41, 31, 16, 15, 14, 21, 22, 34, 51, 52, 53, 54, 55, 56, 57);
		Assert.assertTrue(c.removeFirstOccurrence(Long.valueOf(15)));
		Assert.assertFalse(c.removeFirstOccurrence(Long.valueOf(15)));
		checkDeque(c, 47, 46, 45, 44, 43, 42, 41, 31, 16, 14, 21, 22, 34, 51, 52, 53, 54, 55, 56, 57);
		Assert.assertTrue(c.removeLastOccurrence(Long.valueOf(52)));
		Assert.assertFalse(c.removeLastOccurrence(Long.valueOf(52)));
		checkDeque(c, 47, 46, 45, 44, 43, 42, 41, 31, 16, 14, 21, 22, 34, 51, 53, 54, 55, 56, 57);
		Assert.assertTrue(c.removeLastOccurrence(Long.valueOf(47)));
		Assert.assertFalse(c.removeLastOccurrence(Long.valueOf(47)));
		checkDeque(c, 46, 45, 44, 43, 42, 41, 31, 16, 14, 21, 22, 34, 51, 53, 54, 55, 56, 57);
		Assert.assertTrue(c.removeFirstOccurrence(Long.valueOf(57)));
		Assert.assertFalse(c.removeFirstOccurrence(Long.valueOf(57)));
		checkDeque(c, 46, 45, 44, 43, 42, 41, 31, 16, 14, 21, 22, 34, 51, 53, 54, 55, 56);
		
		// add some duplicates
		c.addFirst(Long.valueOf(44));
		c.addFirst(Long.valueOf(34));
		c.addLast(Long.valueOf(43));
		c.addLast(Long.valueOf(53));
		checkDeque(c, 34, 44, 46, 45, 44, 43, 42, 41, 31, 16, 14, 21, 22, 34, 51, 53, 54, 55, 56, 43, 53);
		Assert.assertTrue(c.removeFirstOccurrence(Long.valueOf(44)));
		checkDeque(c, 34, 46, 45, 44, 43, 42, 41, 31, 16, 14, 21, 22, 34, 51, 53, 54, 55, 56, 43, 53);
		Assert.assertTrue(c.removeLastOccurrence(Long.valueOf(34)));
		checkDeque(c, 34, 46, 45, 44, 43, 42, 41, 31, 16, 14, 21, 22, 51, 53, 54, 55, 56, 43, 53);
		
		c.clear();
		testDequeEmpty(c);
	}
	
	protected static void testDequeEmpty(Deque<Long> c) {
		testQueueEmpty(c);
		assertException(() -> { c.getFirst(); }, NoSuchElementException.class);
		assertException(() -> { c.getLast(); }, NoSuchElementException.class);
		assertException(() -> { c.removeFirst(); }, NoSuchElementException.class);
		assertException(() -> { c.removeLast(); }, NoSuchElementException.class);
		assertException(() -> { c.pop(); }, NoSuchElementException.class);
		assertException(() -> { c.descendingIterator().next(); }, NoSuchElementException.class);
		Assert.assertNull(c.poll());
		Assert.assertNull(c.pollFirst());
		Assert.assertNull(c.pollLast());
		Assert.assertNull(c.peek());
		Assert.assertNull(c.peekFirst());
		Assert.assertNull(c.peekLast());
		Assert.assertFalse(c.removeFirstOccurrence(Long.valueOf(1)));
		Assert.assertFalse(c.removeLastOccurrence(Long.valueOf(1)));
		Assert.assertFalse(c.descendingIterator().hasNext());
	}
	
	protected static void checkDeque(Deque<Long> c, long... values) {
		checkQueue(c, values);
		Assert.assertTrue(c.getFirst().longValue() == values[0]);
		Assert.assertTrue(c.peek().longValue() == values[0]);
		Assert.assertTrue(c.peekFirst().longValue() == values[0]);
		Assert.assertTrue(c.getLast().longValue() == values[values.length - 1]);
		Assert.assertTrue(c.peekLast().longValue() == values[values.length - 1]);
		Iterator<Long> it = c.descendingIterator();
		for (int i = values.length - 1; i >= 0; --i) {
			Assert.assertTrue(it.hasNext());
			Assert.assertEquals(it.next(), Long.valueOf(values[i]));
		}
		Assert.assertFalse(it.hasNext());
		assertException(() -> { it.next(); }, NoSuchElementException.class);
	}
	
	protected static <T> void checkDeque(Deque<T> c, @SuppressWarnings("unchecked") T... values) {
		checkQueue(c, values);
		Assert.assertTrue(c.getFirst() == values[0]);
		Assert.assertTrue(c.peek() == values[0]);
		Assert.assertTrue(c.peekFirst() == values[0]);
		Assert.assertTrue(c.getLast() == values[values.length - 1]);
		Assert.assertTrue(c.peekLast() == values[values.length - 1]);
		Iterator<T> it = c.descendingIterator();
		for (int i = values.length - 1; i >= 0; --i) {
			Assert.assertTrue(it.hasNext());
			Assert.assertEquals(it.next(), values[i]);
		}
		Assert.assertFalse(it.hasNext());
		assertException(() -> { it.next(); }, NoSuchElementException.class);
	}
	
}
