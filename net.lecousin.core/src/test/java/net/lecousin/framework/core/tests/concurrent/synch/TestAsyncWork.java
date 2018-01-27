package net.lecousin.framework.core.tests.concurrent.synch;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.AsyncWork.AsyncWorkListener;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestAsyncWork extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void testStatus() {
		Exception error = new Exception("test");
		CancelException cancel = new CancelException("test");
		AsyncWork<Integer, Exception> blocked = new AsyncWork<>();
		AsyncWork<Integer, Exception> ok = new AsyncWork<>(Integer.valueOf(123), null);
		AsyncWork<Integer, Exception> failed = new AsyncWork<>(null, error);
		AsyncWork<Integer, Exception> cancelled = new AsyncWork<>(null, null, cancel);
		
		Assert.assertFalse(blocked.isUnblocked());
		Assert.assertTrue(ok.isUnblocked());
		Assert.assertTrue(failed.isUnblocked());
		Assert.assertTrue(cancelled.isUnblocked());
		
		Assert.assertEquals(0, blocked.getAllListeners().size());
		Assert.assertEquals(0, ok.getAllListeners().size());
		Assert.assertEquals(0, failed.getAllListeners().size());
		Assert.assertEquals(0, cancelled.getAllListeners().size());
		
		Mutable<AssertionError> result = new Mutable<>(new AssertionError("Listener not called"));
		ok.listenInline(new AsyncWorkListener<Integer, Exception>() {
			@Override
			public void ready(Integer res) {
				if (res.intValue() != 123)
					result.set(new AssertionError("Invalid result"));
				else
					result.set(null);
			}
			@Override
			public void error(Exception error) {
				result.set(new AssertionError("Unexpected error", error));
			}
			@Override
			public void cancelled(CancelException event) {
				result.set(new AssertionError("Unexpected cancel", cancel));
			}
		});
		if (result.get() != null)
			throw result.get();
		
		result.set(new AssertionError("Listener not called"));
		failed.listenInline(new AsyncWorkListener<Integer, Exception>() {
			@Override
			public void ready(Integer res) {
				result.set(new AssertionError("Unexpected result"));
			}
			@Override
			public void error(Exception error) {
				result.set(null);
			}
			@Override
			public void cancelled(CancelException event) {
				result.set(new AssertionError("Unexpected cancel", cancel));
			}
		});
		if (result.get() != null)
			throw result.get();
		
		result.set(new AssertionError("Listener not called"));
		cancelled.listenInline(new AsyncWorkListener<Integer, Exception>() {
			@Override
			public void ready(Integer res) {
				result.set(new AssertionError("Unexpected result"));
			}
			@Override
			public void error(Exception error) {
				result.set(new AssertionError("Unexpected error", error));
			}
			@Override
			public void cancelled(CancelException event) {
				result.set(null);
			}
		});
		if (result.get() != null)
			throw result.get();
		
		AsyncWork<Integer, Exception> aw;

		aw = new AsyncWork<>();
		blocked.listenInline(aw);
		Assert.assertFalse(aw.isUnblocked());

		aw = new AsyncWork<>();
		ok.listenInline(aw);
		Assert.assertTrue(aw.isUnblocked());
		Assert.assertTrue(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		Assert.assertEquals(123, aw.getResult().intValue());

		aw = new AsyncWork<>();
		failed.listenInline(aw);
		Assert.assertTrue(aw.isUnblocked());
		Assert.assertFalse(aw.isSuccessful());
		Assert.assertTrue(aw.hasError());
		Assert.assertFalse(aw.isCancelled());

		aw = new AsyncWork<>();
		cancelled.listenInline(aw);
		Assert.assertTrue(aw.isUnblocked());
		Assert.assertFalse(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertTrue(aw.isCancelled());

		aw.reset();
		Assert.assertFalse(aw.isUnblocked());
		Assert.assertFalse(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		failed.listenInline(aw);
		Assert.assertTrue(aw.isUnblocked());
		Assert.assertFalse(aw.isSuccessful());
		Assert.assertTrue(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		
		SynchronizationPoint<Exception> sp;
		
		sp = new SynchronizationPoint<>();
		blocked.listenInline(sp);
		Assert.assertFalse(sp.isUnblocked());

		sp = new SynchronizationPoint<>();
		ok.listenInline(sp);
		Assert.assertTrue(sp.isUnblocked());
		Assert.assertFalse(sp.hasError());
		Assert.assertFalse(sp.isCancelled());

		sp = new SynchronizationPoint<>();
		failed.listenInline(sp);
		Assert.assertTrue(sp.isUnblocked());
		Assert.assertTrue(sp.hasError());
		Assert.assertFalse(sp.isCancelled());

		sp = new SynchronizationPoint<>();
		cancelled.listenInline(sp);
		Assert.assertTrue(sp.isUnblocked());
		Assert.assertFalse(sp.hasError());
		Assert.assertTrue(sp.isCancelled());
		
		MutableInteger okResult = new MutableInteger(0);
		Listener<Integer> onok = new Listener<Integer>() {
			@Override
			public void fire(Integer event) {
				okResult.set(event.intValue());
			}
		};
		Mutable<Exception> errorResult = new Mutable<>(null);
		Listener<Exception> onerror = new Listener<Exception>() {
			@Override
			public void fire(Exception event) {
				errorResult.set(event);
			}
		};
		Mutable<CancelException> cancelResult = new Mutable<>(null);
		Listener<CancelException> oncancel = new Listener<CancelException>() {
			@Override
			public void fire(CancelException event) {
				cancelResult.set(event);
			}
		};
		
		blocked.listenInline(onok, onerror, oncancel);
		Assert.assertEquals(0, okResult.get());
		Assert.assertNull(errorResult.get());
		Assert.assertNull(cancelResult.get());
		
		okResult.set(0);
		errorResult.set(null);
		cancelResult.set(null);
		ok.listenInline(onok, onerror, oncancel);
		Assert.assertEquals(123, okResult.get());
		Assert.assertNull(errorResult.get());
		Assert.assertNull(cancelResult.get());
		
		okResult.set(0);
		errorResult.set(null);
		cancelResult.set(null);
		failed.listenInline(onok, onerror, oncancel);
		Assert.assertEquals(0, okResult.get());
		Assert.assertNotNull(errorResult.get());
		Assert.assertNull(cancelResult.get());
		
		okResult.set(0);
		errorResult.set(null);
		cancelResult.set(null);
		cancelled.listenInline(onok, onerror, oncancel);
		Assert.assertEquals(0, okResult.get());
		Assert.assertNull(errorResult.get());
		Assert.assertNotNull(cancelResult.get());

		okResult.set(0);
		sp = new SynchronizationPoint<>();
		blocked.listenInline(onok, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertFalse(sp.isUnblocked());

		okResult.set(0);
		sp = new SynchronizationPoint<>();
		ok.listenInline(onok, sp);
		Assert.assertEquals(123, okResult.get());
		Assert.assertFalse(sp.isUnblocked());

		okResult.set(0);
		sp = new SynchronizationPoint<>();
		failed.listenInline(onok, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertTrue(sp.isUnblocked());
		Assert.assertTrue(sp.hasError());
		Assert.assertFalse(sp.isCancelled());

		okResult.set(0);
		sp = new SynchronizationPoint<>();
		cancelled.listenInline(onok, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertTrue(sp.isUnblocked());
		Assert.assertFalse(sp.hasError());
		Assert.assertTrue(sp.isCancelled());
		
		Runnable run = new Runnable() {
			@Override
			public void run() {
				okResult.set(51);
			}
		};

		okResult.set(0);
		sp = new SynchronizationPoint<>();
		blocked.listenInline(run, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertFalse(sp.isUnblocked());

		okResult.set(0);
		sp = new SynchronizationPoint<>();
		ok.listenInline(run, sp);
		Assert.assertEquals(51, okResult.get());
		Assert.assertFalse(sp.isUnblocked());

		okResult.set(0);
		sp = new SynchronizationPoint<>();
		failed.listenInline(run, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertTrue(sp.isUnblocked());
		Assert.assertTrue(sp.hasError());
		Assert.assertFalse(sp.isCancelled());

		okResult.set(0);
		sp = new SynchronizationPoint<>();
		cancelled.listenInline(run, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertTrue(sp.isUnblocked());
		Assert.assertFalse(sp.hasError());
		Assert.assertTrue(sp.isCancelled());
	}
	
	@Test(timeout=30000)
	public void testUnblock() {
		AsyncWork<Integer, Exception> aw;
		aw = new AsyncWork<>();
		Assert.assertEquals(0, aw.getAllListeners().size());
		aw.listenInline(new AsyncWorkListener<Integer, Exception>() {
			@Override
			public void ready(Integer result) {
			}
			@Override
			public void cancelled(CancelException event) {
			}
			@Override
			public void error(Exception error) {
			}
		});
		Assert.assertEquals(1, aw.getAllListeners().size());

		Assert.assertFalse(aw.isUnblocked());
		Assert.assertFalse(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		aw.unblockSuccess(Integer.valueOf(10));
		Assert.assertTrue(aw.isUnblocked());
		Assert.assertTrue(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		aw.error(new Exception());
		Assert.assertTrue(aw.isUnblocked());
		Assert.assertTrue(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		aw.cancel(new CancelException("test"));
		Assert.assertTrue(aw.isUnblocked());
		Assert.assertTrue(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
	}
	
}
