package net.lecousin.framework.core.tests.concurrent.async;

import net.lecousin.framework.concurrent.async.LockPoint;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestLockPoint extends LCCoreAbstractTest {

	@Test
	public void test() {
		LockPoint<Exception> lp = new LockPoint<>();
		lp.error(new Exception());
		lp.getError();
		Assert.assertFalse(lp.isCancelled());
		Assert.assertTrue(lp.hasError());
		Assert.assertFalse(lp.isSuccessful());
		Assert.assertTrue(lp.isDone());
		Assert.assertEquals(0, lp.getAllListeners().size());
		lp.lock();
		
		lp = new LockPoint<>();
		lp.cancel(new CancelException("test"));
		lp.getCancelEvent();
		Assert.assertTrue(lp.isCancelled());
		Assert.assertFalse(lp.hasError());
		Assert.assertFalse(lp.isSuccessful());
		Assert.assertTrue(lp.isDone());
		Assert.assertEquals(0, lp.getAllListeners().size());
		lp.lock();

		lp = new LockPoint<>();
		lp.lock();
		Assert.assertEquals(0, lp.getAllListeners().size());
		lp.onDone(() -> {});
		Assert.assertEquals(1, lp.getAllListeners().size());
		
		lp = new LockPoint<>();
		lp.blockPause(1);
		lp.block(1);
		
		LockPoint<Exception> lp2 = new LockPoint<>();
		lp2.lock();
		Async<Exception> sp = new Async<>();
		Task.cpu("test", Task.Priority.NORMAL, () -> {
			sp.unblock();
			lp2.blockPause(5000);
			return null;
		}).start();
		sp.block(0);
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		lp2.unlock();
		
		LockPoint<Exception> lp3 = new LockPoint<>();
		lp3.lock();
		Async<Exception> sp3 = new Async<>();
		Task.cpu("test", Task.Priority.NORMAL, () -> {
			sp3.unblock();
			lp3.block(5000);
			return null;
		}).start();
		sp3.block(0);
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		lp3.unlock();
	}
	
}
