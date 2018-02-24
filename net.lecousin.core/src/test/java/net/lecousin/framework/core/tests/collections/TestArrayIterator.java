package net.lecousin.framework.core.tests.collections;

import java.util.NoSuchElementException;

import net.lecousin.framework.collections.ArrayIterator;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestArrayIterator extends LCCoreAbstractTest {

	@Test
	public void test() {
		ArrayIterator.Generic it = new ArrayIterator.Generic(null);
		Assert.assertFalse(it.hasNext());
		try {
			it.next();
			throw new AssertionError("Iterator must throw NoSuchElementException");
		} catch (NoSuchElementException e) {
			// ok
		}
	}
	
}
