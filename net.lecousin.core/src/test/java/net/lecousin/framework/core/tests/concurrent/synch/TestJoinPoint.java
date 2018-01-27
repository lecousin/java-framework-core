package net.lecousin.framework.core.tests.concurrent.synch;

import java.io.IOException;
import java.util.Collections;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.exception.NoException;

import org.junit.Assert;
import org.junit.Test;

public class TestJoinPoint extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		JoinPoint<Exception> jp = new JoinPoint<>();
		Assert.assertEquals(0, jp.getToJoin());
		jp.addToJoin(1);
		Assert.assertEquals(1, jp.getToJoin());
		SynchronizationPoint<Exception> spOk = new SynchronizationPoint<>();
		jp.addToJoin(spOk);
		Assert.assertEquals(2, jp.getToJoin());
		SynchronizationPoint<Exception> spError = new SynchronizationPoint<>();
		jp.addToJoin(spError);
		Assert.assertEquals(3, jp.getToJoin());
		SynchronizationPoint<Exception> spCancel = new SynchronizationPoint<>();
		jp.addToJoin(spCancel);
		Assert.assertEquals(4, jp.getToJoin());
		
		spOk.unblock();
		Assert.assertEquals(3, jp.getToJoin());
		Assert.assertFalse(jp.isUnblocked());
		spError.error(new Exception());
		Assert.assertTrue(jp.isUnblocked());
		Assert.assertTrue(jp.hasError());
		
		jp = new JoinPoint<>();
		jp.addToJoin(spCancel);
		Assert.assertEquals(1, jp.getToJoin());
		Assert.assertFalse(jp.isUnblocked());
		spCancel.cancel(new CancelException("test"));
		Assert.assertTrue(jp.isUnblocked());
		Assert.assertTrue(jp.isCancelled());
		
		jp = JoinPoint.fromSynchronizationPoints(new SynchronizationPoint<IOException>(false), new SynchronizationPoint<IOException>(new IOException("test")));
		Assert.assertTrue(jp.isUnblocked());
		Assert.assertTrue(jp.hasError());
		
		jp = new JoinPoint<>();
		Task<Integer, Exception> task = new Task.Cpu<Integer, Exception>("Test", Task.PRIORITY_NORMAL) {
			@Override
			public Integer run() {
				return Integer.valueOf(51);
			}
		};
		jp.addToJoin(task);
		Assert.assertEquals(1, jp.getToJoin());
		Assert.assertFalse(jp.isUnblocked());
		
		jp = JoinPoint.fromTasks(task);
		Assert.assertEquals(1, jp.getToJoin());
		Assert.assertFalse(jp.isUnblocked());
		
		jp = JoinPoint.fromTasks(Collections.singletonList(task));
		Assert.assertEquals(1, jp.getToJoin());
		Assert.assertFalse(jp.isUnblocked());
		
		JoinPoint<NoException> jp2 = JoinPoint.onAllDone(Collections.singletonList(task));
		Assert.assertEquals(1, jp2.getToJoin());
		Assert.assertFalse(jp2.isUnblocked());
		
		JoinPoint.listenInlineOnAllDone(() -> {}, spOk, spError, spCancel);
	}
	
}
