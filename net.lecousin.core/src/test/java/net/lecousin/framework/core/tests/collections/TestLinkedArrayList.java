package net.lecousin.framework.core.tests.collections;

import java.util.Iterator;

import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.core.test.collections.TestList;

import org.junit.Assert;
import org.junit.Test;

public class TestLinkedArrayList extends TestList {

	@Override
	public LinkedArrayList<Long> createLongCollection() {
		return new LinkedArrayList<Long>(5);
	}
	
	@Test(timeout=120000)
	public void testLong() {
		LinkedArrayList<Long> l = createLongCollection();
		l.addlong(0, Long.valueOf(1));
		l.addlong(1, Long.valueOf(2));
		l.addlong(2, Long.valueOf(3));
		Assert.assertTrue(l.size() == 3);
		
		Iterator<Long> it = l.iterator();
		Assert.assertTrue(it.hasNext());
		Assert.assertTrue(it.next().equals(Long.valueOf(1)));
		Assert.assertTrue(it.hasNext());
		Assert.assertTrue(it.next().equals(Long.valueOf(2)));
		Assert.assertTrue(it.hasNext());
		Assert.assertTrue(it.next().equals(Long.valueOf(3)));
		Assert.assertFalse(it.hasNext());
		
		Assert.assertEquals(Long.valueOf(1), l.getlong(0));
		Assert.assertEquals(Long.valueOf(2), l.getlong(1));
		Assert.assertEquals(Long.valueOf(3), l.getlong(2));
		
		Assert.assertEquals(Long.valueOf(2), l.removelong(1));
		Assert.assertTrue(l.size() == 2);
		Assert.assertEquals(Long.valueOf(1), l.getlong(0));
		Assert.assertEquals(Long.valueOf(3), l.getlong(1));

		Assert.assertEquals(Long.valueOf(3), l.removeLast());
		Assert.assertTrue(l.longsize() == 1);
		Assert.assertEquals(Long.valueOf(1), l.getlong(0));
	}
	
}
