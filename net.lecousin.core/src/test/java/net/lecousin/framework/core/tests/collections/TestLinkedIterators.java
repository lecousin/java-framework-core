package net.lecousin.framework.core.tests.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.LinkedIterators;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public class TestLinkedIterators extends LCCoreAbstractTest {

	@Test
	public void test() {
		ArrayList<Integer> c1 = new ArrayList<Integer>();
		ArrayList<Integer> c2 = new ArrayList<Integer>();
		ArrayList<Integer> c3 = new ArrayList<Integer>();
		ArrayList<Integer> c4 = new ArrayList<Integer>();
		
		c1.add(Integer.valueOf(1));
		c1.add(Integer.valueOf(2));
		c2.add(Integer.valueOf(10));
		c2.add(Integer.valueOf(20));
		c2.add(Integer.valueOf(30));
		c4.add(Integer.valueOf(100));
		c4.add(Integer.valueOf(200));
		
		LinkedIterators<Integer> it;
		
		it = new LinkedIterators<>();
		it.addIterator(c1.iterator());
		it.addIterator(c2.iterator());
		it.addIterator(c3.iterator());
		it.addIterator(c4.iterator());
		check(it);
		
		it = new LinkedIterators<>(Arrays.asList(c1.iterator(), c2.iterator(), c3.iterator(), c4.iterator()));
		check(it);
		
		it = new LinkedIterators<>(c1.iterator(), c2.iterator(), c3.iterator(), c4.iterator());
		check(it);
		
		it = new LinkedIterators<>(c1.iterator(), c2.iterator(), c3.iterator(), c4.iterator());
		try {
			it.remove();
			throw new AssertionError("Must throw IllegalStateException");
		} catch (IllegalStateException e) {
			// ok
		}
		while (it.hasNext()) {
			it.next();
			it.remove();
		}
		Assert.assertTrue(c1.isEmpty());
		Assert.assertTrue(c2.isEmpty());
		Assert.assertTrue(c3.isEmpty());
		Assert.assertTrue(c4.isEmpty());
		try {
			it.remove();
			throw new AssertionError("Must throw IllegalStateException");
		} catch (IllegalStateException e) {
			// ok
		}
	}
	
	private static void check(Iterator<Integer> it) {
		Assert.assertEquals(Integer.valueOf(1), it.next());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(Integer.valueOf(2), it.next());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(Integer.valueOf(10), it.next());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(Integer.valueOf(20), it.next());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(Integer.valueOf(30), it.next());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(Integer.valueOf(100), it.next());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals(Integer.valueOf(200), it.next());
		Assert.assertFalse(it.hasNext());
		try {
			it.next();
			throw new AssertionError("Must throw NoSuchElementException");
		} catch (NoSuchElementException e) {
			// ok
		}
	}
	
}
