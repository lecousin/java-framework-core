package net.lecousin.framework.core.tests.concurrent.synch;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.mutable.MutableInteger;

public class TestSynchronizationPoint extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() throws Exception {
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		Assert.assertEquals(0, sp.getAllListeners().size());
		sp.listenInline(() -> {});
		Assert.assertEquals(1, sp.getAllListeners().size());
		
		MutableInteger ok = new MutableInteger(0);
		MutableInteger error = new MutableInteger(0);
		MutableInteger cancel = new MutableInteger(0);
		
		Runnable onOk = () -> { ok.inc(); };
		Listener<Exception> onError = (err) -> { error.inc(); };
		Listener<CancelException> onCancel = (err) -> { cancel.inc(); };
		
		sp = new SynchronizationPoint<>();
		sp.listenInline(onOk, onError, onCancel);
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		sp.unblock();
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());

		sp = new SynchronizationPoint<>();
		sp.listenInline(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(1, error.get());
		Assert.assertEquals(0, cancel.get());

		sp = new SynchronizationPoint<>();
		sp.listenInline(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(1, error.get());
		Assert.assertEquals(1, cancel.get());
		try { sp.blockThrow(0); throw new AssertionError(); }
		catch (CancelException e) {
			Assert.assertEquals("test", e.getMessage());
		}
		
		SynchronizationPoint<Exception> sp2;
		
		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInline(onOk, sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(2, ok.get());
		Assert.assertEquals(1, error.get());
		Assert.assertEquals(1, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInline(onOk, sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(2, ok.get());
		Assert.assertEquals(2, error.get());
		Assert.assertEquals(1, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInline(onOk, sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(2, ok.get());
		Assert.assertEquals(2, error.get());
		Assert.assertEquals(2, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInline(sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(3, ok.get());
		Assert.assertEquals(2, error.get());
		Assert.assertEquals(2, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInline(sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(3, ok.get());
		Assert.assertEquals(3, error.get());
		Assert.assertEquals(2, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInline(sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(3, ok.get());
		Assert.assertEquals(3, error.get());
		Assert.assertEquals(3, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInlineSP(onOk, sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(4, ok.get());
		Assert.assertEquals(3, error.get());
		Assert.assertEquals(3, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInlineSP(onOk, sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(4, ok.get());
		Assert.assertEquals(4, error.get());
		Assert.assertEquals(3, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInlineSP(onOk, sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(4, ok.get());
		Assert.assertEquals(4, error.get());
		Assert.assertEquals(4, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInlineSP(sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(5, ok.get());
		Assert.assertEquals(4, error.get());
		Assert.assertEquals(4, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInlineSP(sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(5, ok.get());
		Assert.assertEquals(5, error.get());
		Assert.assertEquals(4, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenInlineSP(sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(5, ok.get());
		Assert.assertEquals(5, error.get());
		Assert.assertEquals(5, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.synchWithNoError(sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(6, ok.get());
		Assert.assertEquals(5, error.get());
		Assert.assertEquals(5, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.synchWithNoError(sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(7, ok.get());
		Assert.assertEquals(5, error.get());
		Assert.assertEquals(5, cancel.get());

		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.synchWithNoError(sp2);
		sp2.listenInline(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(8, ok.get());
		Assert.assertEquals(5, error.get());
		Assert.assertEquals(5, cancel.get());
		
		sp = new SynchronizationPoint<>();
		sp.onSuccess(onOk);
		sp.onError(onError);
		sp.onException(onError);
		sp.onCancel(onCancel);
		sp.unblock();
		Assert.assertEquals(9, ok.get());
		
		sp = new SynchronizationPoint<>();
		sp.onSuccess(onOk);
		sp.onError(onError);
		sp.onCancel(onCancel);
		sp.error(new Exception());
		Assert.assertEquals(6, error.get());
		
		sp = new SynchronizationPoint<>();
		sp.onSuccess(onOk);
		sp.onException(onError);
		sp.onCancel(onCancel);
		sp.error(new Exception());
		Assert.assertEquals(7, error.get());
		
		sp = new SynchronizationPoint<>();
		sp.onSuccess(onOk);
		sp.onError(onError);
		sp.onException(onError);
		sp.onCancel(onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(6, cancel.get());
		
		AsyncWork<Void, Exception> aw;
		sp = new SynchronizationPoint<>();
		aw = sp.toAsyncWorkVoid();
		aw.listenInline(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(10, ok.get());

		sp = new SynchronizationPoint<>();
		aw = sp.toAsyncWorkVoid();
		aw.listenInline(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(8, error.get());

		sp = new SynchronizationPoint<>();
		aw = sp.toAsyncWorkVoid();
		aw.listenInline(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(7, cancel.get());
		
		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenAsync(new Task.Cpu<Void, Exception>("test", Task.PRIORITY_NORMAL) {
			@Override
			public Void run() throws CancelException {
				throw new CancelException("test");
			}
		}, sp2);
		sp.unblock();
		sp2.block(0);
		Assert.assertTrue(sp2.isCancelled());
		
		sp = new SynchronizationPoint<>();
		sp2 = new SynchronizationPoint<>();
		sp.listenAsyncSP(new Task.Cpu<Void, Exception>("test", Task.PRIORITY_NORMAL) {
			@Override
			public Void run() throws CancelException {
				throw new CancelException("test");
			}
		}, sp2);
		sp.unblock();
		sp2.block(0);
		Assert.assertTrue(sp2.isCancelled());
		
		sp = new SynchronizationPoint<>(new CancelException("test"));
		Assert.assertTrue(sp.isCancelled());
		
		SynchronizationPoint<Exception> spbp1 = new SynchronizationPoint<>();
		SynchronizationPoint<Exception> spbp2 = new SynchronizationPoint<>();
		new Task.Cpu.FromRunnable(() -> {
			spbp1.unblock();
			spbp2.blockPause(5000);
		}, "test", Task.PRIORITY_NORMAL).start();
		spbp1.block(0);
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		spbp2.unblock();
		spbp2.blockPause(1);
	}
	
	@Test(timeout=30000)
	public void testFuture() {
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		Assert.assertFalse(sp.isDone());
		sp.unblock();
		try {
			Assert.assertNull(sp.get());
			Assert.assertNull(sp.get(1, TimeUnit.SECONDS));
			Assert.assertTrue(sp.isDone());
			Assert.assertFalse(sp.cancel(true));
		} catch (Exception e) {
			throw new AssertionError(e);
		}
		
		sp = new SynchronizationPoint<>();
		Assert.assertFalse(sp.isDone());
		sp.cancel(new CancelException("test"));
		try {
			sp.get();
			throw new AssertionError();
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause() instanceof CancelException);
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
		try {
			sp.get(1, TimeUnit.SECONDS);
			throw new AssertionError();
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause() instanceof CancelException);
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
		Assert.assertTrue(sp.isDone());
		Assert.assertFalse(sp.cancel(true));
		
		sp = new SynchronizationPoint<>();
		Assert.assertTrue(sp.cancel(true));
		Assert.assertTrue(sp.isDone());

		sp = new SynchronizationPoint<>();
		Assert.assertFalse(sp.isDone());
		sp.error(new Exception("test"));
		try {
			sp.get();
			throw new AssertionError();
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause() instanceof Exception);
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
		try {
			sp.get(1, TimeUnit.SECONDS);
			throw new AssertionError();
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause() instanceof Exception);
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
		Assert.assertTrue(sp.isDone());
		Assert.assertFalse(sp.cancel(true));
	}
	
}
