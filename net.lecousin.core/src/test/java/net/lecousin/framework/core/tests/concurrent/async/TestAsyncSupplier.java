package net.lecousin.framework.core.tests.concurrent.async;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.AsyncSupplier.Listener;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.log.Logger.Level;
import net.lecousin.framework.log.LoggerFactory;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.mutable.MutableInteger;
import net.lecousin.framework.util.Runnables.ConsumerThrows;
import net.lecousin.framework.util.Runnables.FunctionThrows;

import org.junit.Assert;
import org.junit.Test;

public class TestAsyncSupplier extends LCCoreAbstractTest {

	@Test
	public void testStatus() throws NoException, CancelException {
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
		ok.listen(Listener.from((Consumer<Integer>)null, null, null, "Test"));
		ok.listen(Listener.from((Runnable)null, null, null, "Test"));
		
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
		
		aw = new AsyncSupplier<>();
		aw.listen(Listener.from((Consumer<Integer>)null, null, null, "Test"));
		aw.listen(Listener.from((Runnable)null, null, null, "Test"));
		aw.listen(Listener.from(() -> {}, null, null, "Test"));
		for (Object o : aw.getAllListeners())
			o.toString();
		
		AsyncSupplier<Integer, NoException> awi = new AsyncSupplier<>();
		FunctionThrows<Integer, Long, NoException> converter = i -> Long.valueOf(i.longValue());
		AsyncSupplier<Long, NoException> awl = awi.thenStart("converter", Task.Priority.NORMAL, converter, true);
		awi.unblockSuccess(Integer.valueOf(51));
		Assert.assertEquals(51L, awl.blockResult(5000).longValue());
		awi = new AsyncSupplier<>();
		awl = awi.thenStart("converter", Task.Priority.NORMAL, converter, false);
		awi.unblockSuccess(Integer.valueOf(51));
		Assert.assertEquals(51L, awl.blockResult(5000).longValue());
		
		awi = new AsyncSupplier<>();
		okResult.set(0);
		Async<NoException> onErrorOrCancel = new Async<>();
		awi.thenDoOrStart("test", Task.Priority.NORMAL, i -> okResult.set(i.intValue()), onErrorOrCancel);
		Assert.assertEquals(0, okResult.get());
		awi.unblockSuccess(Integer.valueOf(51));
		while (okResult.get() != 51) {
			new Async<Exception>().block(100);
		}

		awi = new AsyncSupplier<>();
		okResult.set(0);
		awi.unblockSuccess(Integer.valueOf(51));
		awi.thenDoOrStart("test", Task.Priority.NORMAL, i -> okResult.set(i.intValue()), onErrorOrCancel);
		Assert.assertEquals(51, okResult.get());
		
		
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
	
	@Test
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
	
	@Test
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
	
	@Test
	public void testConvertErrorOk() {
		AsyncSupplier<Integer, Exception> as1 = new AsyncSupplier<>();
		AsyncSupplier<Integer, IOException> as2 = as1.convertError(e -> new IOException(e.getMessage()));
		as1.unblockSuccess(Integer.valueOf(15));
		Assert.assertTrue(as2.isSuccessful());
		Assert.assertEquals(Integer.valueOf(15), as2.getResult());
	}
	
	@Test
	public void testConvertErrorError() {
		AsyncSupplier<Integer, Exception> as1 = new AsyncSupplier<>();
		AsyncSupplier<Integer, IOException> as2 = as1.convertError(e -> new IOException(e.getMessage()));
		as1.error(new Exception("test conversion"));
		Assert.assertTrue(as2.hasError());
		Assert.assertEquals("test conversion", as2.getError().getMessage());
	}
	
	@SuppressWarnings("boxing")
	@Test
	public void testThenStart() throws Exception {
		AsyncSupplier<Void, NoException> as = new AsyncSupplier<>();
		
		MutableBoolean b1 = new MutableBoolean(false);
		FunctionThrows<Void, Integer, NoException> f1 = v -> { b1.set(true); return 1; };
		AsyncSupplier<Integer, NoException> r1 = as.thenStart("f1", null, f1, true);
		
		MutableBoolean b2 = new MutableBoolean(false);
		FunctionThrows<Void, Integer, NoException> f2 = v -> { b2.set(true); return 2; };
		AsyncSupplier<Integer, NoException> r2 = as.thenStart("f2", null, f2, false);
		
		MutableBoolean b3 = new MutableBoolean(false);
		FunctionThrows<Void, Integer, NoException> f3 = v -> { b3.set(true); return 3; };
		AsyncSupplier<Integer, NoException> r3 = as.thenStart("f3", null, f3, new Async<>());
		
		MutableBoolean b4 = new MutableBoolean(false);
		ConsumerThrows<Void, NoException> f4 = v -> { b4.set(true); };
		IAsync<NoException> r4 = as.thenStart("f4", null, f4, true);
		
		MutableBoolean b5 = new MutableBoolean(false);
		ConsumerThrows<Void, NoException> f5 = v -> { b5.set(true); };
		IAsync<NoException> r5 = as.thenStart("f5", null, f5, false);
		
		MutableBoolean b6 = new MutableBoolean(false);
		ConsumerThrows<Void, NoException> f6 = v -> { b6.set(true); };
		IAsync<NoException> r6 = as.thenStart("f6", null, f6, new Async<>());
		
		as.unblockSuccess(null);
		
		Assert.assertEquals(1, r1.blockResult(0).intValue());
		Assert.assertTrue(b1.get());
		
		Assert.assertEquals(2, r2.blockResult(0).intValue());
		Assert.assertTrue(b2.get());
		
		Assert.assertEquals(3, r3.blockResult(0).intValue());
		Assert.assertTrue(b3.get());
		
		r4.blockThrow(0);
		Assert.assertTrue(b4.get());
		
		r5.blockThrow(0);
		Assert.assertTrue(b5.get());
		
		r6.blockThrow(0);
		Assert.assertTrue(b6.get());
	}
	
	@Test
	public void testListenerError() {
		AsyncSupplier<Void, Exception> as;
		AsyncSupplier.Listener<Void, Exception> listener1 = new AsyncSupplier.Listener<Void, Exception>() {

			@Override
			public void ready(Void result) {
				throw new RuntimeException("test error");
			}

			@Override
			public void error(Exception error) {
				throw new RuntimeException("test error");
			}

			@Override
			public void cancelled(CancelException event) {
				throw new RuntimeException("test error");
			}
			
		};
		AsyncSupplier.Listener<Void, Exception> listener2 = new AsyncSupplier.Listener<Void, Exception>() {

			@Override
			public void ready(Void result) {
			}

			@Override
			public void error(Exception error) {
			}

			@Override
			public void cancelled(CancelException event) {
			}
			
		};
		
		// success with debug
		LoggerFactory.get(AsyncSupplier.class).setLevel(Level.DEBUG);
		as = new AsyncSupplier<>();
		as.listen(listener1);
		as.listen(listener2);
		as.unblockSuccess(null);
		
		// success without debug
		LoggerFactory.get(AsyncSupplier.class).setLevel(Level.ERROR);
		as = new AsyncSupplier<>();
		as.listen(listener1);
		as.listen(listener2);
		as.unblockSuccess(null);
		
		// error with debug
		LoggerFactory.get(AsyncSupplier.class).setLevel(Level.DEBUG);
		as = new AsyncSupplier<>();
		as.listen(listener1);
		as.listen(listener2);
		as.unblockError(new Exception(""));
		
		// error without debug
		LoggerFactory.get(AsyncSupplier.class).setLevel(Level.ERROR);
		as = new AsyncSupplier<>();
		as.listen(listener1);
		as.listen(listener2);
		as.unblockError(new Exception(""));
		
		// cancel with debug
		LoggerFactory.get(AsyncSupplier.class).setLevel(Level.DEBUG);
		as = new AsyncSupplier<>();
		as.listen(listener1);
		as.listen(listener2);
		as.unblockCancel(null);
		
		// cancel without debug
		LoggerFactory.get(AsyncSupplier.class).setLevel(Level.ERROR);
		as = new AsyncSupplier<>();
		as.listen(listener1);
		as.listen(listener2);
		as.unblockCancel(null);

		LoggerFactory.get(AsyncSupplier.class).setLevel(null);
	}
	
}
