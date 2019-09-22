package net.lecousin.framework.core.tests.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.ListOfArrays;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public class TestListOfArrays extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void testEmpty() {
		ListOfArrays<Integer> l = new ListOfArrays<>();
		Assert.assertEquals(0, l.getArrays().size());
		Iterator<Integer> it = l.iterator();
		Assert.assertFalse(it.hasNext());
		try {
			it.next();
			throw new AssertionError("NoSuchElement");
		} catch (NoSuchElementException e) {
			// ok
		}
	}

	@Test(timeout=30000)
	public void testOneArray() {
		ListOfArrays<Integer> l = new ListOfArrays<>();
		Integer[] a = new Integer[10];
		for (int i = 0; i < a.length; ++i)
			a[i] = Integer.valueOf(i * 2);
		l.add(a);
		Assert.assertEquals(1, l.getArrays().size());
		Iterator<Integer> it = l.iterator();
		for (int i = 0; i < a.length; ++i) {
			Assert.assertTrue(it.hasNext());
			Assert.assertEquals(i * 2, it.next().intValue());
		}
		Assert.assertFalse(it.hasNext());
		try {
			it.next();
			throw new AssertionError("NoSuchElement");
		} catch (NoSuchElementException e) {
			// ok
		}
	}

	@Test(timeout=30000)
	public void testSeveralArrays() {
		ListOfArrays<Integer> l = new ListOfArrays<>();
		Integer[] a = new Integer[10];
		for (int i = 0; i < a.length; ++i)
			a[i] = Integer.valueOf(i * 2);
		l.add(a);
		Integer[] b = new Integer[5];
		for (int i = 0; i < b.length; ++i)
			b[i] = Integer.valueOf(100 + i * 2);
		l.add(b);
		Integer[] c = new Integer[8];
		for (int i = 0; i < c.length; ++i)
			c[i] = Integer.valueOf(1000 + i * 2);
		l.add(c);
		Assert.assertEquals(3, l.getArrays().size());
		Iterator<Integer> it = l.iterator();
		for (int i = 0; i < a.length; ++i) {
			Assert.assertTrue(it.hasNext());
			Assert.assertEquals(i * 2, it.next().intValue());
		}
		for (int i = 0; i < b.length; ++i) {
			Assert.assertTrue(it.hasNext());
			Assert.assertEquals(100 + i * 2, it.next().intValue());
		}
		for (int i = 0; i < c.length; ++i) {
			Assert.assertTrue(it.hasNext());
			Assert.assertEquals(1000 + i * 2, it.next().intValue());
		}
		Assert.assertFalse(it.hasNext());
		try {
			it.next();
			throw new AssertionError("NoSuchElement");
		} catch (NoSuchElementException e) {
			// ok
		}
	}
	
}
