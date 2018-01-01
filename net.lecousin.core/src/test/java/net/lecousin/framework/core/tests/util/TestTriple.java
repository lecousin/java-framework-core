package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.Triple;

import org.junit.Assert;
import org.junit.Test;

public class TestTriple extends LCCoreAbstractTest {

	@Test
	public void test() {
		Triple<Object, Object, Object> t;
		t = new Triple<>(Integer.valueOf(10), Integer.valueOf(20), Integer.valueOf(30));
		Assert.assertEquals(Integer.valueOf(10), t.getValue1());
		Assert.assertEquals(Integer.valueOf(20), t.getValue2());
		Assert.assertEquals(Integer.valueOf(30), t.getValue3());
		t.setValue1(Integer.valueOf(100));
		Assert.assertEquals(Integer.valueOf(100), t.getValue1());
		t.setValue2(Integer.valueOf(200));
		Assert.assertEquals(Integer.valueOf(200), t.getValue2());
		t.setValue3(Integer.valueOf(300));
		Assert.assertEquals(Integer.valueOf(300), t.getValue3());
		Assert.assertTrue(t.equals(new Triple<>(Integer.valueOf(100), Integer.valueOf(200), Integer.valueOf(300))));
		t.hashCode();
		t.toString();
	}
	
}
