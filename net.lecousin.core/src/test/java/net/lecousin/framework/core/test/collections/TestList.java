package net.lecousin.framework.core.test.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestList extends TestCollection {

	@Override
	public abstract List<Long> createLongCollection();
	
	public boolean supportNullValue() { return true; }
	
	@SuppressWarnings("boxing")
	@Test(timeout=120000)
	public void testList() {
		List<Long> l = createLongCollection();
		check(l);
		
		try {
			l.get(10);
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		try {
			l.remove(10);
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		try {
			l.set(3, Long.valueOf(10));
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		
		
		l.add(0, Long.valueOf(1));
		l.add(1, Long.valueOf(2));
		l.add(2, Long.valueOf(3));
		check(l, 1, 2, 3);
	
		try {
			l.remove(-10);
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		try {
			l.remove(3);
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		try {
			l.set(-2, Long.valueOf(11));
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		try {
			l.set(3, Long.valueOf(11));
			throw new AssertionError("Expected exception: IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException e) {}
		
		Iterator<Long> it = l.iterator();
		Assert.assertTrue(it.hasNext());
		Assert.assertTrue(it.next().equals(Long.valueOf(1)));
		Assert.assertTrue(it.hasNext());
		Assert.assertTrue(it.next().equals(Long.valueOf(2)));
		Assert.assertTrue(it.hasNext());
		Assert.assertTrue(it.next().equals(Long.valueOf(3)));
		Assert.assertFalse(it.hasNext());
		
		l.add(0, Long.valueOf(0));
		check (l, 0, 1, 2, 3);

		l.add(2, Long.valueOf(100));
		check (l, 0, 1, 100, 2, 3);
		
		Assert.assertEquals(0, l.subList(0, 0).size());
		check(l.subList(0, 2), 0, 1);
		check(l.subList(1, 1));
		check(l.subList(1, 4), 1, 100, 2);
		check(l.subList(3, 5), 2, 3);
		
		l.clear();
		Assert.assertTrue(l.size() == 0);
		
		ArrayList<Long> arr = new ArrayList<Long>();
		for (long i = 0; i < 1000; ++i) {
			int index;
			if ((i%3) == 0) index = (int)(i/2);
			else if ((i%2) == 0) index = arr.size();
			else index = 0;
			arr.add(index, Long.valueOf(i));
			l.add(index, Long.valueOf(i));
		}
		Assert.assertEquals(arr.size(), l.size());
		
		// compare indexes
		for (long i = 0; i < 1000; ++i) {
			int i1 = arr.indexOf(Long.valueOf(i));
			int i2 = l.indexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
			i2 = arr.lastIndexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
			i2 = l.lastIndexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
		}
		Assert.assertTrue(l.indexOf(Long.valueOf(99999)) == -1);
		Assert.assertTrue(l.lastIndexOf(Long.valueOf(99999)) == -1);
		
		// compare iterators
		ListIterator<Long> lit1 = l.listIterator();
		ListIterator<Long> lit2 = arr.listIterator();
		do {
			Assert.assertEquals(lit1.nextIndex(), lit2.nextIndex());
			Assert.assertEquals(lit1.previousIndex(), lit2.previousIndex());
			Assert.assertEquals(lit1.hasNext(), lit2.hasNext());
			if (!lit1.hasNext()) break;
			Assert.assertEquals(lit1.next(), lit2.next());
		} while (true);

		lit1 = l.listIterator(arr.size()/2);
		lit2 = arr.listIterator(arr.size()/2);
		do {
			Assert.assertEquals(lit1.nextIndex(), lit2.nextIndex());
			Assert.assertEquals(lit1.previousIndex(), lit2.previousIndex());
			Assert.assertEquals(lit1.hasPrevious(), lit2.hasPrevious());
			if (!lit1.hasPrevious()) break;
			Assert.assertEquals(lit1.previous(), lit2.previous());
		} while (true);
		do {
			Assert.assertEquals(lit1.nextIndex(), lit2.nextIndex());
			Assert.assertEquals(lit1.previousIndex(), lit2.previousIndex());
			Assert.assertEquals(lit1.hasNext(), lit2.hasNext());
			if (!lit1.hasNext()) break;
			Assert.assertEquals(lit1.next(), lit2.next());
		} while (true);
		do {
			Assert.assertEquals(lit1.nextIndex(), lit2.nextIndex());
			Assert.assertEquals(lit1.previousIndex(), lit2.previousIndex());
			Assert.assertEquals(lit1.hasPrevious(), lit2.hasPrevious());
			if (!lit1.hasPrevious()) break;
			Assert.assertEquals(lit1.previous(), lit2.previous());
		} while (true);
		
		lit1 = l.listIterator(arr.size());
		lit2 = arr.listIterator(arr.size());
		do {
			Assert.assertEquals(lit1.nextIndex(), lit2.nextIndex());
			Assert.assertEquals(lit1.previousIndex(), lit2.previousIndex());
			Assert.assertEquals(lit1.hasPrevious(), lit2.hasPrevious());
			if (!lit1.hasPrevious()) break;
			Assert.assertEquals(lit1.previous(), lit2.previous());
		} while (true);
		
		// add and remove
		Assert.assertTrue(l.indexOf(Long.valueOf(100000)) == -1);
		Assert.assertTrue(l.lastIndexOf(Long.valueOf(100000)) == -1);
		l.add(3, Long.valueOf(100000));
		Assert.assertEquals(l.size(), arr.size() + 1);
		Assert.assertTrue(l.indexOf(Long.valueOf(100000)) == 3);
		Assert.assertTrue(l.lastIndexOf(Long.valueOf(100000)) == 3);
		l.remove(3);
		Assert.assertEquals(l.size(), arr.size());
		Assert.assertTrue(l.indexOf(Long.valueOf(100000)) == -1);
		Assert.assertTrue(l.lastIndexOf(Long.valueOf(100000)) == -1);
		l.add(10, Long.valueOf(100000));
		Assert.assertEquals(l.size(), arr.size() + 1);
		Assert.assertTrue(l.indexOf(Long.valueOf(100000)) == 10);
		Assert.assertTrue(l.lastIndexOf(Long.valueOf(100000)) == 10);
		l.remove(Long.valueOf(100000));
		Assert.assertEquals(l.size(), arr.size());
		Assert.assertTrue(l.indexOf(Long.valueOf(100000)) == -1);
		Assert.assertTrue(l.lastIndexOf(Long.valueOf(100000)) == -1);
		lit1 = l.listIterator(arr.size());
		lit2 = arr.listIterator(arr.size());
		do {
			Assert.assertEquals(lit1.nextIndex(), lit2.nextIndex());
			Assert.assertEquals(lit1.previousIndex(), lit2.previousIndex());
			Assert.assertEquals(lit1.hasPrevious(), lit2.hasPrevious());
			if (!lit1.hasPrevious()) break;
			Assert.assertEquals("At index " + lit1.previousIndex(), lit1.previous(), lit2.previous());
		} while (true);
		
		// null value
		if (supportNullValue()) {
			Assert.assertTrue(l.indexOf(null) == -1);
			Assert.assertTrue(l.lastIndexOf(null) == -1);
			l.add(3, null);
			Assert.assertTrue(l.indexOf(null) == 3);
			Assert.assertTrue(l.lastIndexOf(null) == 3);
			l.remove(3);
			Assert.assertTrue(l.indexOf(null) == -1);
			Assert.assertTrue(l.lastIndexOf(null) == -1);
			l.add(10, null);
			Assert.assertTrue(l.indexOf(null) == 10);
			Assert.assertTrue(l.lastIndexOf(null) == 10);
			l.remove(null);
			Assert.assertTrue(l.indexOf(null) == -1);
			Assert.assertTrue(l.lastIndexOf(null) == -1);
			lit1 = l.listIterator();
			lit2 = arr.listIterator();
			do {
				Assert.assertEquals(lit1.nextIndex(), lit2.nextIndex());
				Assert.assertEquals(lit1.previousIndex(), lit2.previousIndex());
				Assert.assertEquals(lit1.hasNext(), lit2.hasNext());
				if (!lit1.hasNext()) break;
				Assert.assertEquals(lit1.next(), lit2.next());
			} while (true);
		}

		// addAll
		ArrayList<Long> toAdd = new ArrayList<Long>();
		for (long i = 1000; i < 2000; ++i) toAdd.add(Long.valueOf(i));
		arr.addAll(toAdd);
		l.addAll(toAdd);
		Assert.assertEquals(arr.size(), l.size());
		// compare indexes
		for (long i = 0; i < 2000; ++i) {
			int i1 = arr.indexOf(Long.valueOf(i));
			int i2 = l.indexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
			i2 = arr.lastIndexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
			i2 = l.lastIndexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
		}
		// compare iterators
		lit1 = l.listIterator();
		lit2 = arr.listIterator();
		do {
			Assert.assertEquals(lit1.nextIndex(), lit2.nextIndex());
			Assert.assertEquals(lit1.previousIndex(), lit2.previousIndex());
			Assert.assertEquals(lit1.hasNext(), lit2.hasNext());
			if (!lit1.hasNext()) break;
			Assert.assertEquals(lit1.next(), lit2.next());
		} while (true);

		toAdd = new ArrayList<Long>();
		for (long i = 2000; i < 3000; ++i) toAdd.add(Long.valueOf(i));
		arr.addAll(10, toAdd);
		l.addAll(10, toAdd);
		Assert.assertEquals(arr.size(), l.size());
		// compare indexes
		for (long i = 0; i < 2000; ++i) {
			int i1 = arr.indexOf(Long.valueOf(i));
			int i2 = l.indexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
			i2 = arr.lastIndexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
			i2 = l.lastIndexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
		}
		// compare iterators
		lit1 = l.listIterator();
		lit2 = arr.listIterator();
		do {
			Assert.assertEquals(lit1.nextIndex(), lit2.nextIndex());
			Assert.assertEquals(lit1.previousIndex(), lit2.previousIndex());
			Assert.assertEquals(lit1.hasNext(), lit2.hasNext());
			if (!lit1.hasNext()) break;
			Assert.assertEquals(lit1.next(), lit2.next());
		} while (true);
		
		// get(index)
		for (int i = 0; i < arr.size(); ++i)
			Assert.assertEquals("At index " + i, arr.get(i), l.get(i));
		try { l.get(l.size() + 1); throw new RuntimeException("get(index) must throw IndexOutOfBoundsException"); }
		catch (IndexOutOfBoundsException e) {}
		catch (Throwable t) { throw new RuntimeException("get(index) must throw IndexOutOfBoundsException", t); }
		try { l.get(-10); throw new RuntimeException("get(index) must throw IndexOutOfBoundsException"); }
		catch (IndexOutOfBoundsException e) {}
		catch (Throwable t) { throw new RuntimeException("get(index) must throw IndexOutOfBoundsException", t); }

		// remove(index)
		for (int i = 0; i < arr.size(); i += 7)
			Assert.assertEquals(arr.remove(i), l.remove(i));
		Assert.assertEquals(arr.size(), l.size());
		// compare indexes
		for (long i = 0; i < 2000; ++i) {
			int i1 = arr.indexOf(Long.valueOf(i));
			int i2 = l.indexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
			i2 = arr.lastIndexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
			i2 = l.lastIndexOf(Long.valueOf(i));
			Assert.assertEquals(i1, i2);
		}
		// compare iterators
		lit1 = l.listIterator();
		lit2 = arr.listIterator();
		do {
			Assert.assertEquals(lit1.nextIndex(), lit2.nextIndex());
			Assert.assertEquals(lit1.previousIndex(), lit2.previousIndex());
			Assert.assertEquals(lit1.hasNext(), lit2.hasNext());
			if (!lit1.hasNext()) break;
			Assert.assertEquals(lit1.next(), lit2.next());
		} while (true);
		
		// iterator.add
		lit1 = l.listIterator();
		lit2 = arr.listIterator();
		while (lit1.hasNext()) {
			Assert.assertEquals(lit1.next(), lit2.next());
			if ((lit1.nextIndex() % 7) == 0) {
				lit1.add(Long.valueOf(lit1.nextIndex()));
				lit2.add(Long.valueOf(lit2.nextIndex()));
			}
		}
		Assert.assertEquals(arr.size(), l.size());
		for (int i = 0; i < arr.size(); ++i)
			Assert.assertEquals("At index " + i, arr.get(i), l.get(i));
		
		// iterator.set
		lit1 = l.listIterator();
		lit2 = arr.listIterator();
		while (lit1.hasNext()) {
			Assert.assertEquals(lit1.next(), lit2.next());
			if ((lit1.nextIndex() % 9) == 0) {
				lit1.set(Long.valueOf(lit1.nextIndex()));
				lit2.set(Long.valueOf(lit2.nextIndex()));
			}
		}
		Assert.assertEquals(arr.size(), l.size());
		for (int i = 0; i < arr.size(); ++i)
			Assert.assertEquals("At index " + i, arr.get(i), l.get(i));
		
		// set(index)
		for (int i = 0; i < arr.size(); ++i) {
			arr.set(i, Long.valueOf(i));
			l.set(i, Long.valueOf(i));
		}
		Assert.assertEquals(arr.size(), l.size());
		for (int i = 0; i < arr.size(); ++i)
			Assert.assertEquals("At index " + i, arr.get(i), l.get(i));
	}
	
	protected void check(List<Long> l, long... expectedValues) {
		Assert.assertEquals(expectedValues.length, l.size());
		Iterator<Long> it = l.iterator();
		for (int i = 0; i < expectedValues.length; ++i) {
			Assert.assertTrue(it.hasNext());
			Assert.assertTrue(it.next().equals(Long.valueOf(expectedValues[i])));
		}
		Assert.assertFalse(it.hasNext());
	}

}
