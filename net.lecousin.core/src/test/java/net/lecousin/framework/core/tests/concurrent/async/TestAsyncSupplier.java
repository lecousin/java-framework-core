package net.lecousin.framework.core.tests.concurrent.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.AsyncSupplier.Listener;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestAsyncSupplier extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void testStatus() {
		Exception error = new Exception("test");
		CancelException cancel = new CancelException("test");
		AsyncSupplier<Integer, Exception> blocked = new AsyncSupplier<>();
		AsyncSupplier<Integer, Exception> ok = new AsyncSupplier<>(Integer.valueOf(123), null);
		AsyncSupplier<Integer, Exception> failed = new AsyncSupplier<>(null, error);
		AsyncSupplier<Integer, Exception> cancelled = new AsyncSupplier<>(null, null, cancel);
		
		Assert.assertFalse(blocked.isDone());
		Assert.assertTrue(ok.isDone());
		Assert.assertTrue(failed.isDone());
		Assert.assertTrue(cancelled.isDone());
		
		Assert.assertEquals(0, blocked.getAllListeners().size());
		Assert.assertEquals(0, ok.getAllListeners().size());
		Assert.assertEquals(0, failed.getAllListeners().size());
		Assert.assertEquals(0, cancelled.getAllListeners().size());
		
		Mutable<AssertionError> result = new Mutable<>(new AssertionError("Listener not called"));
		ok.listen(new Listener<Integer, Exception>() {
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
		failed.listen(new Listener<Integer, Exception>() {
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
		cancelled.listen(new Listener<Integer, Exception>() {
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
		
		AsyncSupplier<Integer, Exception> aw;

		aw = new AsyncSupplier<>();
		blocked.forward(aw);
		Assert.assertFalse(aw.isDone());

		aw = new AsyncSupplier<>();
		ok.forward(aw);
		Assert.assertTrue(aw.isDone());
		Assert.assertTrue(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		Assert.assertEquals(123, aw.getResult().intValue());

		aw = new AsyncSupplier<>();
		failed.forward(aw);
		Assert.assertTrue(aw.isDone());
		Assert.assertFalse(aw.isSuccessful());
		Assert.assertTrue(aw.hasError());
		Assert.assertFalse(aw.isCancelled());

		aw = new AsyncSupplier<>();
		cancelled.forward(aw);
		Assert.assertTrue(aw.isDone());
		Assert.assertFalse(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertTrue(aw.isCancelled());

		aw.reset();
		Assert.assertFalse(aw.isDone());
		Assert.assertFalse(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		failed.forward(aw);
		Assert.assertTrue(aw.isDone());
		Assert.assertFalse(aw.isSuccessful());
		Assert.assertTrue(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		
		Async<Exception> sp;
		
		sp = new Async<>();
		blocked.onDone(sp);
		Assert.assertFalse(sp.isDone());

		sp = new Async<>();
		ok.onDone(sp);
		Assert.assertTrue(sp.isDone());
		Assert.assertFalse(sp.hasError());
		Assert.assertFalse(sp.isCancelled());

		sp = new Async<>();
		failed.onDone(sp);
		Assert.assertTrue(sp.isDone());
		Assert.assertTrue(sp.hasError());
		Assert.assertFalse(sp.isCancelled());

		sp = new Async<>();
		cancelled.onDone(sp);
		Assert.assertTrue(sp.isDone());
		Assert.assertFalse(sp.hasError());
		Assert.assertTrue(sp.isCancelled());
		
		MutableInteger okResult = new MutableInteger(0);
		Consumer<Integer> onok = event -> okResult.set(event.intValue());
		Mutable<Exception> errorResult = new Mutable<>(null);
		Consumer<Exception> onerror = event -> errorResult.set(event);
		Mutable<CancelException> cancelResult = new Mutable<>(null);
		Consumer<CancelException> oncancel = event -> cancelResult.set(event);
		
		blocked.onDone(onok, onerror, oncancel);
		Assert.assertEquals(0, okResult.get());
		Assert.assertNull(errorResult.get());
		Assert.assertNull(cancelResult.get());
		
		okResult.set(0);
		errorResult.set(null);
		cancelResult.set(null);
		ok.onDone(onok, onerror, oncancel);
		Assert.assertEquals(123, okResult.get());
		Assert.assertNull(errorResult.get());
		Assert.assertNull(cancelResult.get());
		
		okResult.set(0);
		errorResult.set(null);
		cancelResult.set(null);
		failed.onDone(onok, onerror, oncancel);
		Assert.assertEquals(0, okResult.get());
		Assert.assertNotNull(errorResult.get());
		Assert.assertNull(cancelResult.get());
		
		okResult.set(0);
		errorResult.set(null);
		cancelResult.set(null);
		cancelled.onDone(onok, onerror, oncancel);
		Assert.assertEquals(0, okResult.get());
		Assert.assertNull(errorResult.get());
		Assert.assertNotNull(cancelResult.get());

		okResult.set(0);
		sp = new Async<>();
		blocked.onDone(onok, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertFalse(sp.isDone());

		okResult.set(0);
		sp = new Async<>();
		ok.onDone(onok, sp);
		Assert.assertEquals(123, okResult.get());
		Assert.assertFalse(sp.isDone());

		okResult.set(0);
		sp = new Async<>();
		failed.onDone(onok, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertTrue(sp.isDone());
		Assert.assertTrue(sp.hasError());
		Assert.assertFalse(sp.isCancelled());

		okResult.set(0);
		sp = new Async<>();
		cancelled.onDone(onok, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertTrue(sp.isDone());
		Assert.assertFalse(sp.hasError());
		Assert.assertTrue(sp.isCancelled());
		
		Runnable run = new Runnable() {
			@Override
			public void run() {
				okResult.set(51);
			}
		};

		okResult.set(0);
		sp = new Async<>();
		blocked.onDone(run, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertFalse(sp.isDone());

		okResult.set(0);
		sp = new Async<>();
		ok.onDone(run, sp);
		Assert.assertEquals(51, okResult.get());
		Assert.assertFalse(sp.isDone());

		okResult.set(0);
		sp = new Async<>();
		failed.onDone(run, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertTrue(sp.isDone());
		Assert.assertTrue(sp.hasError());
		Assert.assertFalse(sp.isCancelled());

		okResult.set(0);
		sp = new Async<>();
		cancelled.onDone(run, sp);
		Assert.assertEquals(0, okResult.get());
		Assert.assertTrue(sp.isDone());
		Assert.assertFalse(sp.hasError());
		Assert.assertTrue(sp.isCancelled());
		
		aw = new AsyncSupplier<>();
		result.set(new AssertionError("Listener not called"));
		aw.onDone((res) -> {
			if (res != null && res.intValue() == 51)
				result.set(null);
			else
				result.set(new AssertionError("Listener received wrong result: " + res));
		});
		aw.unblockSuccess(Integer.valueOf(51));
		if (result.get() != null)
			throw result.get();
		
		aw = new AsyncSupplier<>();
		result.set(null);
		aw.onDone((res) -> { result.set(new AssertionError("Listener onSuccess called")); });
		aw.error(new Exception());
		if (result.get() != null)
			throw result.get();

		aw = new AsyncSupplier<>();
		result.set(null);
		aw.onDone((res) -> { result.set(new AssertionError("Listener onSuccess called")); });
		aw.cancel(new CancelException("test"));
		if (result.get() != null)
			throw result.get();

		try {
			AsyncSupplier<Integer, Exception> aw1 = new AsyncSupplier<>();
			aw1.onDone(() -> { aw1.onDone(() -> {}); });
			aw1.error(new Exception());
			
			AsyncSupplier<Integer, Exception> aw2 = new AsyncSupplier<>();
			aw2.onDone(() -> { aw2.onDone(() -> {}); });
			aw2.cancel(new CancelException("test"));
		} finally {
		}
		
		Logger log = LCCore.get().getApplication().getLoggerFactory().getLogger(Async.class);
		log.setLevel(Level.INFO);
		
		try {
			
			AsyncSupplier<Integer, Exception> aw1 = new AsyncSupplier<>();
			aw1.onDone(() -> { aw1.onDone(() -> {}); });
			aw1.error(new Exception());
			
			AsyncSupplier<Integer, Exception> aw2 = new AsyncSupplier<>();
			aw2.onDone(() -> { aw2.onDone(() -> {}); });
			for (Object l : aw2.getAllListeners()) l.toString();
			aw2.cancel(new CancelException("test"));
			
			try {
				aw2.blockResult(0);
				throw new AssertionError("Should be cancelled");
			} catch (CancelException e) {
				// ok
			} catch (Throwable t) {
				throw new AssertionError("Should be cancelled", t);
			}
			
		} finally {
			log.setLevel(Level.DEBUG);
		}
	}
	
	@Test(timeout=30000)
	public void testFuture() {
		AsyncSupplier<Integer, Exception> aw = new AsyncSupplier<>();
		Assert.assertFalse(aw.isDone());
		aw.unblockSuccess(Integer.valueOf(51));
		try {
			Assert.assertEquals(51, aw.get().intValue());
			Assert.assertEquals(51, aw.get(1, TimeUnit.SECONDS).intValue());
			Assert.assertTrue(aw.isDone());
			Assert.assertFalse(aw.cancel(true));
		} catch (Exception e) {
			throw new AssertionError(e);
		}
		
		aw = new AsyncSupplier<>();
		Assert.assertFalse(aw.isDone());
		aw.cancel(new CancelException("test"));
		try {
			aw.get();
			throw new AssertionError();
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause() instanceof CancelException);
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
		try {
			aw.get(1, TimeUnit.SECONDS);
			throw new AssertionError();
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause() instanceof CancelException);
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
		Assert.assertTrue(aw.isDone());
		Assert.assertFalse(aw.cancel(true));
		
		aw = new AsyncSupplier<>();
		Assert.assertTrue(aw.cancel(true));
		Assert.assertTrue(aw.isDone());

		aw = new AsyncSupplier<>();
		Assert.assertFalse(aw.isDone());
		aw.error(new Exception("test"));
		try {
			aw.get();
			throw new AssertionError();
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause() instanceof Exception);
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
		try {
			aw.get(1, TimeUnit.SECONDS);
			throw new AssertionError();
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause() instanceof Exception);
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
		Assert.assertTrue(aw.isDone());
		Assert.assertFalse(aw.cancel(true));
	}
	
	@Test(timeout=30000)
	public void testUnblock() {
		AsyncSupplier<Integer, Exception> aw;
		aw = new AsyncSupplier<>();
		Assert.assertEquals(0, aw.getAllListeners().size());
		aw.listen(new Listener<Integer, Exception>() {
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

		Assert.assertFalse(aw.isDone());
		Assert.assertFalse(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		aw.unblockSuccess(Integer.valueOf(10));
		Assert.assertTrue(aw.isDone());
		Assert.assertTrue(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		aw.error(new Exception());
		Assert.assertTrue(aw.isDone());
		Assert.assertTrue(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
		aw.cancel(new CancelException("test"));
		Assert.assertTrue(aw.isDone());
		Assert.assertTrue(aw.isSuccessful());
		Assert.assertFalse(aw.hasError());
		Assert.assertFalse(aw.isCancelled());
	}
	
}
