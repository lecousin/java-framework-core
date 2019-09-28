package net.lecousin.framework.core.tests.concurrent.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.mutable.MutableInteger;

public class TestAsync extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() throws Exception {
		Async<Exception> sp = new Async<>();
		Assert.assertEquals(0, sp.getAllListeners().size());
		sp.onDone(() -> {});
		Assert.assertEquals(1, sp.getAllListeners().size());
		
		MutableInteger ok = new MutableInteger(0);
		MutableInteger error = new MutableInteger(0);
		MutableInteger cancel = new MutableInteger(0);
		
		Runnable onOk = () -> { ok.inc(); };
		Consumer<Exception> onError = (err) -> { error.inc(); };
		Consumer<CancelException> onCancel = (err) -> { cancel.inc(); };
		
		sp = new Async<>();
		sp.onDone(onOk, onError, onCancel);
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		sp.unblock();
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());

		sp = new Async<>();
		sp.onDone(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(1, error.get());
		Assert.assertEquals(0, cancel.get());

		sp = new Async<>();
		sp.onDone(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(1, error.get());
		Assert.assertEquals(1, cancel.get());
		try { sp.blockThrow(0); throw new AssertionError(); }
		catch (CancelException e) {
			Assert.assertEquals("test", e.getMessage());
		}
		
		Async<Exception> sp2;
		
		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(onOk, sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(2, ok.get());
		Assert.assertEquals(1, error.get());
		Assert.assertEquals(1, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(onOk, sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(2, ok.get());
		Assert.assertEquals(2, error.get());
		Assert.assertEquals(1, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(onOk, sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(2, ok.get());
		Assert.assertEquals(2, error.get());
		Assert.assertEquals(2, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(3, ok.get());
		Assert.assertEquals(2, error.get());
		Assert.assertEquals(2, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(3, ok.get());
		Assert.assertEquals(3, error.get());
		Assert.assertEquals(2, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(3, ok.get());
		Assert.assertEquals(3, error.get());
		Assert.assertEquals(3, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(onOk, sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(4, ok.get());
		Assert.assertEquals(3, error.get());
		Assert.assertEquals(3, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(onOk, sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(4, ok.get());
		Assert.assertEquals(4, error.get());
		Assert.assertEquals(3, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(onOk, sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(4, ok.get());
		Assert.assertEquals(4, error.get());
		Assert.assertEquals(4, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(5, ok.get());
		Assert.assertEquals(4, error.get());
		Assert.assertEquals(4, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(5, ok.get());
		Assert.assertEquals(5, error.get());
		Assert.assertEquals(4, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(sp2);
		sp2.onDone(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(5, ok.get());
		Assert.assertEquals(5, error.get());
		Assert.assertEquals(5, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(sp2::unblock);
		sp2.onDone(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(6, ok.get());
		Assert.assertEquals(5, error.get());
		Assert.assertEquals(5, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(sp2::unblock);
		sp2.onDone(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(7, ok.get());
		Assert.assertEquals(5, error.get());
		Assert.assertEquals(5, cancel.get());

		sp = new Async<>();
		sp2 = new Async<>();
		sp.onDone(sp2::unblock);
		sp2.onDone(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(8, ok.get());
		Assert.assertEquals(5, error.get());
		Assert.assertEquals(5, cancel.get());
		
		sp = new Async<>();
		sp.onSuccess(onOk);
		sp.onError(onError);
		sp.onCancel(onCancel);
		sp.unblock();
		Assert.assertEquals(9, ok.get());
		
		sp = new Async<>();
		sp.onSuccess(onOk);
		sp.onError(onError);
		sp.onCancel(onCancel);
		sp.error(new Exception());
		Assert.assertEquals(6, error.get());
		
		sp = new Async<>();
		sp.onSuccess(onOk);
		sp.onError(onError);
		sp.onCancel(onCancel);
		sp.error(new Exception());
		Assert.assertEquals(7, error.get());
		
		sp = new Async<>();
		sp.onSuccess(onOk);
		sp.onError(onError);
		sp.onCancel(onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(6, cancel.get());
		
		AsyncSupplier<Void, Exception> aw;
		sp = new Async<>();
		aw = sp.toAsyncSupplier();
		aw.onDone(onOk, onError, onCancel);
		sp.unblock();
		Assert.assertEquals(10, ok.get());

		sp = new Async<>();
		aw = sp.toAsyncSupplier();
		aw.onDone(onOk, onError, onCancel);
		sp.error(new Exception());
		Assert.assertEquals(8, error.get());

		sp = new Async<>();
		aw = sp.toAsyncSupplier();
		aw.onDone(onOk, onError, onCancel);
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(7, cancel.get());
		
		sp = new Async<>();
		sp2 = new Async<>();
		sp.thenStart(new Task.Cpu<Void, Exception>("test", Task.PRIORITY_NORMAL) {
			@Override
			public Void run() throws CancelException {
				throw new CancelException("test");
			}
		}, sp2);
		sp.unblock();
		sp2.block(0);
		Assert.assertTrue(sp2.isCancelled());
		
		sp = new Async<>();
		sp2 = new Async<>();
		sp.thenStart(new Task.Cpu<Void, Exception>("test", Task.PRIORITY_NORMAL) {
			@Override
			public Void run() throws CancelException {
				throw new CancelException("test");
			}
		}, sp2);
		sp.unblock();
		sp2.block(0);
		Assert.assertTrue(sp2.isCancelled());
		
		sp = new Async<>(new CancelException("test"));
		Assert.assertTrue(sp.isCancelled());
		
		Async<Exception> spbp1 = new Async<>();
		Async<Exception> spbp2 = new Async<>();
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
		Async<Exception> sp = new Async<>();
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
		
		sp = new Async<>();
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
		
		sp = new Async<>();
		Assert.assertTrue(sp.cancel(true));
		Assert.assertTrue(sp.isDone());

		sp = new Async<>();
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
