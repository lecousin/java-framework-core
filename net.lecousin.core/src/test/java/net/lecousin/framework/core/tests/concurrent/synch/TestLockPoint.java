package net.lecousin.framework.core.tests.concurrent.synch;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.LockPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestLockPoint extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		LockPoint<Exception> lp = new LockPoint<>();
		lp.error(new Exception());
		lp.getError();
		Assert.assertFalse(lp.isCancelled());
		Assert.assertTrue(lp.hasError());
		Assert.assertFalse(lp.isSuccessful());
		Assert.assertTrue(lp.isUnblocked());
		Assert.assertEquals(0, lp.getAllListeners().size());
		lp.lock();
		
		lp = new LockPoint<>();
		lp.cancel(new CancelException("test"));
		lp.getCancelEvent();
		Assert.assertTrue(lp.isCancelled());
		Assert.assertFalse(lp.hasError());
		Assert.assertFalse(lp.isSuccessful());
		Assert.assertTrue(lp.isUnblocked());
		Assert.assertEquals(0, lp.getAllListeners().size());
		lp.lock();

		lp = new LockPoint<>();
		lp.lock();
		Assert.assertEquals(0, lp.getAllListeners().size());
		lp.listenInline(() -> {});
		Assert.assertEquals(1, lp.getAllListeners().size());
		
		lp = new LockPoint<>();
		lp.blockPause(1);
		lp.block(1);
		
		LockPoint<Exception> lp2 = new LockPoint<>();
		lp2.lock();
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		new Task.Cpu.FromRunnable(() -> {
			sp.unblock();
			lp2.blockPause(5000);
		}, "test", Task.PRIORITY_NORMAL).start();
		sp.block(0);
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		lp2.unlock();
		
		LockPoint<Exception> lp3 = new LockPoint<>();
		lp3.lock();
		SynchronizationPoint<Exception> sp3 = new SynchronizationPoint<>();
		new Task.Cpu.FromRunnable(() -> {
			sp3.unblock();
			lp3.block(5000);
		}, "test", Task.PRIORITY_NORMAL).start();
		sp3.block(0);
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		lp3.unlock();
	}
	
}
