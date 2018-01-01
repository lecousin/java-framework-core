package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.Pair;

import org.junit.Assert;
import org.junit.Test;

public class TestPair extends LCCoreAbstractTest {

	@Test
	public void test() {
		Pair<Object, Object> p;
		p = new Pair<>(Integer.valueOf(10), Integer.valueOf(20));
		Assert.assertEquals(Integer.valueOf(10), p.getValue1());
		Assert.assertEquals(Integer.valueOf(20), p.getValue2());
		p.setValue1(Integer.valueOf(100));
		Assert.assertEquals(Integer.valueOf(100), p.getValue1());
		p.setValue2(Integer.valueOf(200));
		Assert.assertEquals(Integer.valueOf(200), p.getValue2());
		Assert.assertTrue(p.equals(new Pair<>(Integer.valueOf(100), Integer.valueOf(200))));
		p.hashCode();
		p.toString();
	}
	
}
