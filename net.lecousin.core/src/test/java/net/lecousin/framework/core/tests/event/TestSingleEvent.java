package net.lecousin.framework.core.tests.event;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.event.SingleEvent;
import net.lecousin.framework.mutable.MutableInteger;

public class TestSingleEvent extends LCCoreAbstractTest {

	@Test
	public void test() {
		MutableInteger called1 = new MutableInteger(0);
		MutableInteger called2 = new MutableInteger(0);
		MutableInteger received = new MutableInteger(0);
		Listener<Integer> listener1 = (value) -> { called1.inc(); received.set(value.intValue()); };
		Listener<Integer> listener11 = (value) -> { called1.inc(); received.set(value.intValue()); };
		Runnable listener2 = () -> { called2.inc(); };
		Runnable listener22 = () -> { called2.inc(); };
		SingleEvent<Integer> event = new SingleEvent<>();
		
		Assert.assertEquals(0, called1.get());
		Assert.assertEquals(0, called2.get());
		Assert.assertEquals(0, received.get());

		event.fire(Integer.valueOf(10));
		Assert.assertEquals(0, called1.get());
		Assert.assertEquals(0, called2.get());
		Assert.assertEquals(0, received.get());
		
		event.addListener(listener1);
		Assert.assertEquals(1, called1.get());
		Assert.assertEquals(0, called2.get());
		Assert.assertEquals(10, received.get());
		event.addListener(listener2);
		Assert.assertEquals(1, called1.get());
		Assert.assertEquals(1, called2.get());
		Assert.assertEquals(10, received.get());
		
		try {
			event.fire(Integer.valueOf(20));
			throw new AssertionError("SingleEvent can be fired several times");
		} catch (IllegalStateException e) {}
		
		event = new SingleEvent<>();
		called1.set(0);
		called2.set(0);
		received.set(0);
		Assert.assertEquals(0, called1.get());
		Assert.assertEquals(0, called2.get());
		Assert.assertEquals(0, received.get());

		event.addListener(listener1);
		event.addListener(listener11);
		event.addListener(listener2);
		event.addListener(listener22);
		Assert.assertEquals(0, called1.get());
		Assert.assertEquals(0, called2.get());
		Assert.assertEquals(0, received.get());
		event.fire(Integer.valueOf(123));
		Assert.assertEquals(2, called1.get());
		Assert.assertEquals(2, called2.get());
		Assert.assertEquals(123, received.get());

		event = new SingleEvent<>();
		called1.set(0);
		called2.set(0);
		received.set(0);
		Assert.assertEquals(0, called1.get());
		Assert.assertEquals(0, called2.get());
		Assert.assertEquals(0, received.get());

		event.addListener(listener1);
		event.addListener(listener2);
		Assert.assertEquals(0, called1.get());
		Assert.assertEquals(0, called2.get());
		Assert.assertEquals(0, received.get());
		Assert.assertTrue(event.hasListeners());
		event.removeListener(listener1);
		Assert.assertTrue(event.hasListeners());
		event.removeListener(listener2);
		Assert.assertFalse(event.hasListeners());
		Assert.assertFalse(event.occured());
		event.fire(Integer.valueOf(456));
		Assert.assertEquals(0, called1.get());
		Assert.assertEquals(0, called2.get());
		Assert.assertEquals(0, received.get());
		Assert.assertTrue(event.occured());
	}
	
}
