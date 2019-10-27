package net.lecousin.framework.core.tests.mutable;

import net.lecousin.framework.mutable.MutableLong;

import org.junit.Assert;
import org.junit.Test;

public class TestMutableLong {

	@Test
	public void test() {
		MutableLong i = new MutableLong(10);
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
