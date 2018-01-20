package net.lecousin.framework.core.tests.event;

import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.event.AsyncEvent;

import org.junit.Assert;
import org.junit.Test;

public class TestAsyncEvent extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		Runnable listener = () -> { sp.unblock(); };

		AsyncEvent e = new AsyncEvent();
		Assert.assertFalse(e.hasListeners());
		
		e.addListener(listener);
		e.fire();
		sp.block(0);
		sp.reset();
		e.fire();
		sp.block(0);
		sp.reset();
		e.removeListener(listener);
		e.fire();
		sp.block(3000);
		Assert.assertFalse(sp.isUnblocked());
	}
	
}
