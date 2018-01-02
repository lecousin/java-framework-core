package net.lecousin.framework.core.tests.event;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.event.SimpleEvent;
import net.lecousin.framework.mutable.MutableInteger;

public class TestSimpleEvent extends LCCoreAbstractTest {

	@Test
	public void test() {
		MutableInteger called = new MutableInteger(0);
		Runnable listener = () -> { called.inc(); };
		SimpleEvent e = new SimpleEvent();
		Assert.assertEquals(0, called.get());
		e.fire();
		Assert.assertEquals(0, called.get());
		
		e.addListener(listener);
		Assert.assertEquals(0, called.get());
		e.fire();
		Assert.assertEquals(1, called.get());
		
		e.addListener(listener);
		Assert.assertEquals(1, called.get());
		e.fire();
		Assert.assertEquals(3, called.get());
		
		e.removeListener(listener);
		Assert.assertEquals(3, called.get());
		e.fire();
		Assert.assertEquals(4, called.get());
		
		e.removeListener(listener);
		Assert.assertEquals(4, called.get());
		e.fire();
		Assert.assertEquals(4, called.get());
	}
	
}
