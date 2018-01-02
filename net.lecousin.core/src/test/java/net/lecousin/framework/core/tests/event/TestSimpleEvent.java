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
		Assert.assertFalse(e.hasListeners());
		e.fire();
		Assert.assertEquals(0, called.get());
		Assert.assertFalse(e.hasListeners());
		
		e.addListener(listener);
		Assert.assertEquals(0, called.get());
		Assert.assertTrue(e.hasListeners());
		e.fire();
		Assert.assertEquals(1, called.get());
		Assert.assertTrue(e.hasListeners());
		
		e.addListener(listener);
		Assert.assertEquals(1, called.get());
		e.fire();
		Assert.assertEquals(3, called.get());
		Assert.assertTrue(e.hasListeners());
		
		e.removeListener(listener);
		Assert.assertTrue(e.hasListeners());
		Assert.assertEquals(3, called.get());
		e.fire();
		Assert.assertEquals(4, called.get());
		Assert.assertTrue(e.hasListeners());
		
		e.removeListener(listener);
		Assert.assertFalse(e.hasListeners());
		Assert.assertEquals(4, called.get());
		e.fire();
		Assert.assertEquals(4, called.get());
		Assert.assertFalse(e.hasListeners());

		e.removeListener(listener);
		e.fire();
		Assert.assertEquals(4, called.get());
		e.listen(listener);
		Assert.assertEquals(4, called.get());
		e.fire();
		Assert.assertEquals(5, called.get());
		e.unlisten(listener);
		Assert.assertEquals(5, called.get());
		e.fire();
		Assert.assertEquals(5, called.get());
	}
	
}
