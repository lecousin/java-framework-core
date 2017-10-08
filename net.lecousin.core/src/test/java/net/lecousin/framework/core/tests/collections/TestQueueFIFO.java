package net.lecousin.framework.core.tests.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;

public abstract class TestQueueFIFO extends TestCollection {

	@Override
	public abstract Queue<Long> createLongCollection();

	@SuppressWarnings({ "boxing" })
	@Test
	public void testPushAndPop() {
		Queue<Long> col = createLongCollection();
		testQueueEmpty(col);
		
		// offer, poll
		col.offer(Long.valueOf(1));
		col.offer(Long.valueOf(2));
		col.offer(Long.valueOf(3));
		checkQueue(col, 1, 2, 3);
		Assert.assertTrue(col.poll() == 1);
		checkQueue(col, 2, 3);
		Assert.assertTrue(col.poll() == 2);
		checkQueue(col, 3);
		Assert.assertTrue(col.poll() == 3);
		testQueueEmpty(col);

		// offer, peek, poll
		col.offer(Long.valueOf(11));
		col.offer(Long.valueOf(12));
		col.offer(Long.valueOf(13));
		checkQueue(col, 11, 12, 13);
		col.offer(Long.valueOf(14));
		col.offer(Long.valueOf(15));
		checkQueue(col, 11, 12, 13, 14, 15);
		Assert.assertTrue(col.peek() == 11);
		Assert.assertTrue(col.poll() == 11);
		checkQueue(col, 12, 13, 14, 15);
		Assert.assertTrue(col.peek() == 12);
		Assert.assertTrue(col.poll() == 12);
		checkQueue(col, 13, 14, 15);
		Assert.assertTrue(col.peek() == 13);
		Assert.assertTrue(col.poll() == 13);
		checkQueue(col, 14, 15);
		Assert.assertTrue(col.peek() == 14);
		Assert.assertTrue(col.poll() == 14);
		checkQueue(col, 15);
		Assert.assertTrue(col.peek() == 15);
		Assert.assertTrue(col.poll() == 15);
		testQueueEmpty(col);
		
		// offer, element, poll
		col.offer(Long.valueOf(21));
		col.offer(Long.valueOf(22));
		col.offer(Long.valueOf(23));
		checkQueue(col, 21, 22, 23);
		col.offer(Long.valueOf(24));
		col.offer(Long.valueOf(25));
		col.offer(Long.valueOf(26));
		checkQueue(col, 21, 22, 23, 24, 25, 26);
		Assert.assertTrue(col.element() == 21);
		Assert.assertTrue(col.poll() == 21);
		checkQueue(col, 22, 23, 24, 25, 26);
		Assert.assertTrue(col.element() == 22);
		Assert.assertTrue(col.poll() == 22);
		checkQueue(col, 23, 24, 25, 26);
		Assert.assertTrue(col.element() == 23);
		Assert.assertTrue(col.poll() == 23);
		checkQueue(col, 24, 25, 26);
		Assert.assertTrue(col.element() == 24);
		Assert.assertTrue(col.poll() == 24);
		checkQueue(col, 25, 26);
		Assert.assertTrue(col.element() == 25);
		Assert.assertTrue(col.poll() == 25);
		checkQueue(col, 26);
		Assert.assertTrue(col.poll() == 26);
		testQueueEmpty(col);

		// offer, remove, poll
		col.offer(Long.valueOf(31));
		col.offer(Long.valueOf(32));
		col.offer(Long.valueOf(33));
		checkQueue(col, 31, 32, 33);
		Assert.assertTrue(col.poll() == 31);
		checkQueue(col, 32, 33);
		col.offer(Long.valueOf(34));
		checkQueue(col, 32, 33, 34);
		col.offer(Long.valueOf(35));
		col.offer(Long.valueOf(36));
		checkQueue(col, 32, 33, 34, 35, 36);
		Assert.assertTrue(col.remove() == 32);
		checkQueue(col, 33, 34, 35, 36);
		col.offer(Long.valueOf(37));
		col.offer(Long.valueOf(38));
		col.offer(Long.valueOf(39));
		checkQueue(col, 33, 34, 35, 36, 37, 38, 39);
		Assert.assertTrue(col.poll() == 33);
		checkQueue(col, 34, 35, 36, 37, 38, 39);
		Assert.assertTrue(col.peek() == 34);
		Assert.assertTrue(col.remove() == 34);
		checkQueue(col, 35, 36, 37, 38, 39);
		Assert.assertTrue(col.poll() == 35);
		checkQueue(col, 36, 37, 38, 39);
		Assert.assertTrue(col.remove() == 36);
		checkQueue(col, 37, 38, 39);
		col.offer(Long.valueOf(40));
		col.offer(Long.valueOf(41));
		col.offer(Long.valueOf(42));
		col.offer(Long.valueOf(43));
		col.offer(Long.valueOf(44));
		col.offer(Long.valueOf(45));
		checkQueue(col, 37, 38, 39, 40, 41, 42, 43, 44, 45);
		Assert.assertTrue(col.poll() == 37);
		checkQueue(col, 38, 39, 40, 41, 42, 43, 44, 45);
		Assert.assertTrue(col.poll() == 38);
		checkQueue(col, 39, 40, 41, 42, 43, 44, 45);
		Assert.assertTrue(col.peek() == 39);
		Assert.assertTrue(col.remove() == 39);
		checkQueue(col, 40, 41, 42, 43, 44, 45);
		Assert.assertTrue(col.poll() == 40);
		checkQueue(col, 41, 42, 43, 44, 45);
		Assert.assertTrue(col.remove() == 41);
		checkQueue(col, 42, 43, 44, 45);
		Assert.assertTrue(col.remove() == 42);
		checkQueue(col, 43, 44, 45);
		Assert.assertTrue(col.remove() == 43);
		checkQueue(col, 44, 45);
		Assert.assertTrue(col.poll() == 44);
		checkQueue(col, 45);
		Assert.assertTrue(col.remove() == 45);
		testQueueEmpty(col);
	}
	
	protected static void testQueueEmpty(Queue<Long> col) {
		testCollectionEmpty(col);
		assertException(() -> { col.remove(); }, NoSuchElementException.class);
		assertException(() -> { col.element(); }, NoSuchElementException.class);
		Assert.assertNull(col.poll());
		Assert.assertNull(col.peek());
	}
	
	protected static void checkQueue(Queue<Long> col, long... values) {
		Assert.assertEquals(values.length, col.size());
		Assert.assertFalse(col.isEmpty());
		Assert.assertEquals(col.peek(), Long.valueOf(values[0]));
		for (int i = 0; i < values.length; ++i)
			Assert.assertTrue("Contains " + values[i] + " return false, elements are " + Arrays.toString(values), col.contains(Long.valueOf(values[i])));
		Assert.assertTrue(ArrayUtil.contains(values, 18469) == col.contains(Long.valueOf(18469)));
		Iterator<Long> it = col.iterator();
		for (int i = 0; i < values.length; ++i) {
			Assert.assertTrue(it.hasNext());
			Assert.assertEquals(it.next(), Long.valueOf(values[i]));
		}
		Assert.assertFalse(it.hasNext());
		assertException(() -> { it.next(); }, NoSuchElementException.class);
		Object[] a = col.toArray();
		Assert.assertTrue(a.length == values.length);
		for (int i = 0; i < values.length; ++i) {
			Assert.assertTrue(a[i] instanceof Long);
			Assert.assertTrue("Element at index " + i + " should be " + values[i] + " found is " + a[i], ((Long)a[i]).longValue() == values[i]);
		}
	}
}
