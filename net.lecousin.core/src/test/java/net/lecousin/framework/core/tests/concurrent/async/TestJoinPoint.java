package net.lecousin.framework.core.tests.concurrent.async;

import java.io.IOException;
import java.util.Collections;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.mutable.MutableInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestJoinPoint extends LCCoreAbstractTest {

	@Test
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
	
	@Test
	public void testAddToJoinWithErrorConverterOk() {
		JoinPoint<IOException> jp = new JoinPoint<>();
		AsyncSupplier<Integer, Exception> as = new AsyncSupplier<>();
		jp.addToJoin(as, e -> new IOException(e.getMessage()));
		jp.start();
		as.unblockSuccess(Integer.valueOf(33));
		Assert.assertTrue(jp.isSuccessful());
	}
	
	@Test
	public void testAddToJoinWithErrorConverterError() {
		JoinPoint<IOException> jp = new JoinPoint<>();
		AsyncSupplier<Integer, Exception> as = new AsyncSupplier<>();
		jp.addToJoin(as, e -> new IOException(e.getMessage()));
		jp.start();
		as.error(new Exception("ex join"));
		Assert.assertTrue(jp.hasError());
		Assert.assertEquals("ex join", jp.getError().getMessage());
	}
	
	@Test
	public void testAddToJoinWithErrorConverterCancel() {
		JoinPoint<IOException> jp = new JoinPoint<>();
		AsyncSupplier<Integer, Exception> as = new AsyncSupplier<>();
		jp.addToJoin(as, e -> new IOException(e.getMessage()));
		jp.start();
		as.cancel(new CancelException("cancel"));
		Assert.assertTrue(jp.isCancelled());
	}
	
	@Test
	public void testWithTimeout() {
		JoinPoint<Exception> jp = new JoinPoint<>();
		AsyncSupplier<Integer, Exception> as = new AsyncSupplier<>();
		jp.addToJoin(as);
		MutableInteger timedout = new MutableInteger(0);
		jp.timeout(1, timedout::inc);
		new Task.Cpu.FromRunnable("test", Task.PRIORITY_NORMAL, () -> {
			as.unblockSuccess(Integer.valueOf(11));
		}).executeIn(5000).start();
		jp.start();
		jp.block(10000);
		Assert.assertTrue(jp.isDone());
		Assert.assertEquals(1, timedout.get());
	}
	
	@Test
	public void testListenTime() {
		JoinPoint<Exception> jp = new JoinPoint<>();
		AsyncSupplier<Integer, Exception> as = new AsyncSupplier<>();
		jp.addToJoin(as);
		MutableInteger timedout = new MutableInteger(0);
		jp.listenTime(1, timedout::inc);
		new Task.Cpu.FromRunnable("test", Task.PRIORITY_NORMAL, () -> {
			as.unblockSuccess(Integer.valueOf(11));
		}).executeIn(2500).start();
		jp.start();
		jp.block(10000);
		Assert.assertTrue(jp.isDone());
		Assert.assertEquals(1, timedout.get());
	}
	
	@Test
	public void testJoinOnDoneThenDo() {
		MutableInteger i = new MutableInteger(0);
		AsyncSupplier<Integer, Exception> as = new AsyncSupplier<>();
		JoinPoint.joinThenDo(i::inc, as);
		Assert.assertEquals(0, i.get());
		as.error(new Exception());
		Assert.assertEquals(1, i.get());
	}
	
}
