package net.lecousin.framework.core.tests.concurrent.util;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.util.AsyncTimeoutManager;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.mutable.MutableBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class TestAsyncTimeout extends LCCoreAbstractTest {

	@Test
	public void testNoTimeout() {
		Async<Exception> a = new Async<>();
		MutableBoolean timedOut = new MutableBoolean(false);
		AsyncTimeoutManager.timeout(a, 1000, () -> timedOut.set(true));
		a.unblock();
		try { Thread.sleep(2000); }
		catch (InterruptedException e) {}
		Assert.assertFalse(timedOut.get());
	}

	@Test
	public void testAlreadyUnblocked() {
		Async<Exception> a = new Async<>();
		a.unblock();
		MutableBoolean timedOut = new MutableBoolean(false);
		AsyncTimeoutManager.timeout(a, 1, () -> timedOut.set(true));
		try { Thread.sleep(1000); }
		catch (InterruptedException e) {}
		Assert.assertFalse(timedOut.get());
	}

	@Test
	public void testTimeout() {
		Async<Exception> a = new Async<>();
		MutableBoolean timedOut = new MutableBoolean(false);
		AsyncTimeoutManager.timeout(a, 100, () -> timedOut.set(true));
		try { Thread.sleep(2000); }
		catch (InterruptedException e) {}
		Assert.assertTrue(timedOut.get());
		a.unblock();
	}

	@Test
	public void test2Timeouts() {
		Async<Exception> a = new Async<>();
		MutableBoolean timedOut1 = new MutableBoolean(false);
		AsyncTimeoutManager.timeout(a, 1000, () -> timedOut1.set(true));
		Async<Exception> b = new Async<>();
		MutableBoolean timedOut2 = new MutableBoolean(false);
		AsyncTimeoutManager.timeout(b, 1, () -> timedOut2.set(true));
		try { Thread.sleep(250); }
		catch (InterruptedException e) {}
		Assert.assertFalse(timedOut1.get());
		Assert.assertTrue(timedOut2.get());
		try { Thread.sleep(1500); }
		catch (InterruptedException e) {}
		Assert.assertTrue(timedOut1.get());
		Assert.assertTrue(timedOut2.get());
	}
	
}
