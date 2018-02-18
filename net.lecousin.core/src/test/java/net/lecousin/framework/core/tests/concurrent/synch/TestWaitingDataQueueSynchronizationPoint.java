package net.lecousin.framework.core.tests.concurrent.synch;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.WaitingDataQueueSynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestWaitingDataQueueSynchronizationPoint extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void test() {
		WaitingDataQueueSynchronizationPoint<Integer, Exception> wdq;
		
		wdq = new WaitingDataQueueSynchronizationPoint<>();
		Assert.assertTrue(wdq.isUnblocked());
		wdq.cancel(new CancelException("test"));
		Assert.assertNull(wdq.waitForData(0));
		Assert.assertTrue(wdq.isUnblocked());
		Assert.assertTrue(wdq.isCancelled());
		Assert.assertFalse(wdq.hasError());
		wdq.getCancelEvent();
		wdq.block(0);
		wdq.blockPause(1);

		wdq = new WaitingDataQueueSynchronizationPoint<>();
		wdq.error(new Exception("test"));
		Assert.assertNull(wdq.waitForData(0));
		Assert.assertTrue(wdq.isUnblocked());
		Assert.assertFalse(wdq.isCancelled());
		Assert.assertTrue(wdq.hasError());
		wdq.getError();
		wdq.block(0);
		wdq.blockPause(1);

		wdq = new WaitingDataQueueSynchronizationPoint<>();
		wdq.endOfData();
		Assert.assertNull(wdq.waitForData(0));
		Assert.assertEquals(0, wdq.getAllListeners().size());
		Assert.assertTrue(wdq.isUnblocked());
		Assert.assertFalse(wdq.isCancelled());
		Assert.assertFalse(wdq.hasError());
		wdq.block(0);
		wdq.blockPause(1);
		
		wdq = new WaitingDataQueueSynchronizationPoint<>();
		wdq.listenInline(() -> {});
		Assert.assertEquals(1, wdq.getAllListeners().size());
		wdq.endOfData();
		Assert.assertEquals(0, wdq.getAllListeners().size());
	}
	
}
