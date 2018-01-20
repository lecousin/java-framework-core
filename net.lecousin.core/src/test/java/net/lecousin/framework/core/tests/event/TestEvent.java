package net.lecousin.framework.core.tests.event;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.event.Event;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.mutable.MutableInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestEvent extends LCCoreAbstractTest {

	@Test
	public void test() {
		MutableInteger called1 = new MutableInteger(0);
		Runnable runnable1 = () -> { called1.inc(); };
		MutableInteger called2 = new MutableInteger(0);
		Runnable runnable2 = () -> { called2.inc(); };
		MutableInteger val1 = new MutableInteger(0);
		Listener<Integer> listener1 = new Listener<Integer>() {
			@Override
			public void fire(Integer value) {
				val1.set(value.intValue());
			}
		};
		MutableInteger val2 = new MutableInteger(0);
		Listener<Integer> listener2 = new Listener<Integer>() {
			@Override
			public void fire(Integer value) {
				val2.set(value.intValue());
			}
		};
		
		Event<Integer> e = new Event<>();
		
		Assert.assertEquals(0, called1.get());
		Assert.assertEquals(0, called2.get());
		Assert.assertEquals(0, val1.get());
		Assert.assertEquals(0, val2.get());
		Assert.assertFalse(e.hasListeners());
		e.fire(Integer.valueOf(10));
		Assert.assertEquals(0, called1.get());
		Assert.assertEquals(0, called2.get());
		Assert.assertEquals(0, val1.get());
		Assert.assertEquals(0, val2.get());
		Assert.assertFalse(e.hasListeners());
		
		e.addListener(runnable1);
		Assert.assertTrue(e.hasListeners());
		e.addListener(runnable2);
		Assert.assertTrue(e.hasListeners());
		e.addListener(listener1);
		Assert.assertTrue(e.hasListeners());
		e.addListener(listener2);
		Assert.assertTrue(e.hasListeners());
		Assert.assertEquals(0, called1.get());
		Assert.assertEquals(0, called2.get());
		Assert.assertEquals(0, val1.get());
		Assert.assertEquals(0, val2.get());
		Assert.assertTrue(e.hasListeners());
		e.fire(Integer.valueOf(20));
		Assert.assertEquals(1, called1.get());
		Assert.assertEquals(1, called2.get());
		Assert.assertEquals(20, val1.get());
		Assert.assertEquals(20, val2.get());
		Assert.assertTrue(e.hasListeners());
		
		e.removeListener(runnable1);
		e.removeListener(listener1);
		Assert.assertTrue(e.hasListeners());
		Assert.assertEquals(1, called1.get());
		Assert.assertEquals(1, called2.get());
		Assert.assertEquals(20, val1.get());
		Assert.assertEquals(20, val2.get());
		e.fire(Integer.valueOf(30));
		Assert.assertTrue(e.hasListeners());
		Assert.assertEquals(1, called1.get());
		Assert.assertEquals(2, called2.get());
		Assert.assertEquals(20, val1.get());
		Assert.assertEquals(30, val2.get());
		
		e.removeListener(runnable2);
		e.removeListener(listener2);
		Assert.assertFalse(e.hasListeners());
		Assert.assertEquals(1, called1.get());
		Assert.assertEquals(2, called2.get());
		Assert.assertEquals(20, val1.get());
		Assert.assertEquals(30, val2.get());
		e.fire(Integer.valueOf(40));
		Assert.assertFalse(e.hasListeners());
		Assert.assertEquals(1, called1.get());
		Assert.assertEquals(2, called2.get());
		Assert.assertEquals(20, val1.get());
		Assert.assertEquals(30, val2.get());
		
		e.removeListener(listener1);
		Assert.assertFalse(e.hasListeners());
		e.removeListener(runnable1);
		Assert.assertFalse(e.hasListeners());
		Assert.assertEquals(1, called1.get());
		Assert.assertEquals(2, called2.get());
		Assert.assertEquals(20, val1.get());
		Assert.assertEquals(30, val2.get());
		e.fire(Integer.valueOf(50));
		Assert.assertFalse(e.hasListeners());
		Assert.assertEquals(1, called1.get());
		Assert.assertEquals(2, called2.get());
		Assert.assertEquals(20, val1.get());
		Assert.assertEquals(30, val2.get());
		
		Event<Integer> e2 = new Event<Integer>();
		e.addListener(runnable1);
		e.addListener(listener1);
		e2.addListener(Event.createListenerToFire(e));
		Assert.assertTrue(e.hasListeners());
		Assert.assertTrue(e2.hasListeners());
		Assert.assertEquals(1, called1.get());
		Assert.assertEquals(2, called2.get());
		Assert.assertEquals(20, val1.get());
		Assert.assertEquals(30, val2.get());
		e2.fire(Integer.valueOf(60));
		Assert.assertTrue(e.hasListeners());
		Assert.assertTrue(e2.hasListeners());
		Assert.assertEquals(2, called1.get());
		Assert.assertEquals(2, called2.get());
		Assert.assertEquals(60, val1.get());
		Assert.assertEquals(30, val2.get());
	}
	
}
