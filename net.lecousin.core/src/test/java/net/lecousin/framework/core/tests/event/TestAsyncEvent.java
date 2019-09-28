package net.lecousin.framework.core.tests.event;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.event.AsyncEvent;

import org.junit.Assert;
import org.junit.Test;

public class TestAsyncEvent extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		Async<Exception> sp = new Async<>();
		Runnable listener = () -> { sp.unblock(); };

		AsyncEvent e = new AsyncEvent();
		Assert.assertFalse(e.hasListeners());
		
		e.addListener(listener);
		Assert.assertTrue(e.hasListeners());
		e.fire();
		sp.block(0);
		sp.reset();
		e.fire();
		sp.block(0);
		sp.reset();
		e.removeListener(listener);
		Assert.assertFalse(e.hasListeners());
		e.fire();
		sp.block(3000);
		Assert.assertFalse(sp.isDone());
	}
	
}
