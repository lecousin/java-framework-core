package net.lecousin.framework.core.tests.mutable;

import net.lecousin.framework.mutable.MutableInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestMutableInteger {

	@Test(timeout=30000)
	public void test() {
		MutableInteger i = new MutableInteger(10);
		Assert.assertEquals(10, i.get());
		i.set(20);
		Assert.assertEquals(20, i.get());
		Assert.assertEquals(21, i.inc());
		Assert.assertEquals(21, i.get());
		Assert.assertEquals(26, i.add(5));
		Assert.assertEquals(26, i.get());
		Assert.assertEquals(25, i.dec());
		Assert.assertEquals(25, i.get());
		Assert.assertEquals(18, i.sub(7));
		Assert.assertEquals(18, i.get());
	}
	
}
