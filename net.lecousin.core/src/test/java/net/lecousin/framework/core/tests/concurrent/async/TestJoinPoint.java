package net.lecousin.framework.core.tests.concurrent.async;

import java.io.IOException;
import java.util.Collections;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.exception.NoException;

import org.junit.Assert;
import org.junit.Test;

public class TestJoinPoint extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		Threading.debugSynchronization = true;
		JoinPoint<Exception> jp = new JoinPoint<>();
		Assert.assertEquals(0, jp.getToJoin());
		jp.addToJoin(1);
		Assert.assertEquals(1, jp.getToJoin());
		Async<Exception> spOk = new Async<>();
		jp.addToJoin(spOk);
		Assert.assertEquals(2, jp.getToJoin());
		Async<Exception> spError = new Async<>();
		jp.addToJoin(spError);
		Assert.assertEquals(3, jp.getToJoin());
		Async<Exception> spCancel = new Async<>();
		jp.addToJoin(spCancel);
		Assert.assertEquals(4, jp.getToJoin());
		jp.start();
		jp.timeout(10000, () -> {});
		
		spOk.unblock();
		Assert.assertEquals(3, jp.getToJoin());
		Assert.assertFalse(jp.isDone());
		spError.error(new Exception());
		Assert.assertTrue(jp.isDone());
		Assert.assertTrue(jp.hasError());
		jp.timeout(10000, () -> {});
		
		jp = new JoinPoint<>();
		jp.addToJoin(spCancel);
		Assert.assertEquals(1, jp.getToJoin());
		Assert.assertFalse(jp.isDone());
		spCancel.cancel(new CancelException("test"));
		Assert.assertTrue(jp.isDone());
		Assert.assertTrue(jp.isCancelled());
		
		jp = JoinPoint.from(new Async<IOException>(false), new Async<IOException>(new IOException("test")));
		Assert.assertTrue(jp.isDone());
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
		Assert.assertFalse(jp.isDone());
		jp.timeout(1, () -> {});
		
		jp = JoinPoint.fromTasks(task);
		Assert.assertEquals(1, jp.getToJoin());
		Assert.assertFalse(jp.isDone());
		
		jp = JoinPoint.fromTasks(Collections.singletonList(task));
		Assert.assertEquals(1, jp.getToJoin());
		Assert.assertFalse(jp.isDone());
		
		JoinPoint<NoException> jp2 = JoinPoint.fromTasksNoErrorOrCancel(Collections.singletonList(task));
		Assert.assertEquals(1, jp2.getToJoin());
		Assert.assertFalse(jp2.isDone());

		task.cancel(new CancelException("test"));
		Assert.assertTrue(jp.isDone());
		Assert.assertTrue(jp.isDone());
		
		Threading.debugSynchronization = false;
	}
	
}