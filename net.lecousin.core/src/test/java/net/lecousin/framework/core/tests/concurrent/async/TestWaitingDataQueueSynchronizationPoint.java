package net.lecousin.framework.core.tests.concurrent.async;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.WaitingDataQueueSynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.mutable.MutableBoolean;

import org.junit.Assert;
import org.junit.Test;

public class TestWaitingDataQueueSynchronizationPoint extends LCCoreAbstractTest {

	@Test
	public void test() {
		WaitingDataQueueSynchronizationPoint<Integer, Exception> wdq;
		
		wdq = new WaitingDataQueueSynchronizationPoint<>();
		Assert.assertFalse(wdq.isDone());
		wdq.cancel(new CancelException("test"));
		Assert.assertNull(wdq.waitForData(0));
		Assert.assertTrue(wdq.isDone());
		Assert.assertTrue(wdq.isCancelled());
		Assert.assertFalse(wdq.hasError());
		wdq.getCancelEvent();
		wdq.block(0);
		wdq.blockPause(1);

		wdq = new WaitingDataQueueSynchronizationPoint<>();
		wdq.error(new Exception("test"));
		Assert.assertNull(wdq.waitForData(0));
		Assert.assertTrue(wdq.isDone());
		Assert.assertFalse(wdq.isCancelled());
		Assert.assertTrue(wdq.hasError());
		wdq.getError();
		wdq.block(0);
		wdq.blockPause(1);

		wdq = new WaitingDataQueueSynchronizationPoint<>();
		wdq.endOfData();
		Assert.assertNull(wdq.waitForData(0));
		Assert.assertEquals(0, wdq.getAllListeners().size());
		Assert.assertTrue(wdq.isDone());
		Assert.assertFalse(wdq.isCancelled());
		Assert.assertFalse(wdq.hasError());
		wdq.block(0);
		wdq.blockPause(1);
		try {
			wdq.newDataReady(Integer.valueOf(0));
			throw new AssertionError("Must throw IllegalStateException");
		} catch (IllegalStateException e) {
			// ok
		}
		
		wdq = new WaitingDataQueueSynchronizationPoint<>();
		wdq.onDone(() -> {});
		Assert.assertEquals(1, wdq.getAllListeners().size());
		wdq.endOfData();
		Assert.assertEquals(0, wdq.getAllListeners().size());

		wdq = new WaitingDataQueueSynchronizationPoint<>();
		MutableBoolean called = new MutableBoolean(false);
		Assert.assertFalse(called.get());
		wdq.newDataReady(Integer.valueOf(0));
		Assert.assertFalse(called.get());
		wdq.onDone(() -> called.set(true));
		Assert.assertTrue(called.get());

		wdq = new WaitingDataQueueSynchronizationPoint<>();
		called.set(false);
		wdq.onDone(() -> called.set(true));
		Assert.assertFalse(called.get());
		wdq.newDataReady(Integer.valueOf(0));
		Assert.assertTrue(called.get());
}
	
}
