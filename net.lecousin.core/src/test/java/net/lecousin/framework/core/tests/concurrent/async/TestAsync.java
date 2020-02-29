package net.lecousin.framework.core.tests.concurrent.async;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.mutable.MutableInteger;

public class TestAsync extends LCCoreAbstractTest {

	Async<Exception> sp = new Async<>();
	Async<Exception> sp2 = new Async<>();

	MutableInteger ok = new MutableInteger(0);
	MutableInteger error = new MutableInteger(0);
	MutableInteger cancel = new MutableInteger(0);
	
	Runnable onOk = () -> { ok.inc(); };
	Consumer<Exception> onError = (err) -> { error.inc(); };
	Consumer<CancelException> onCancel = (err) -> { cancel.inc(); };
	
	@Test
	public void testAddListener() {
		Assert.assertEquals(0, sp.getAllListeners().size());
		sp.onDone(() -> {});
		Assert.assertEquals(1, sp.getAllListeners().size());
		testListenersToString();
	}
	
	@Test
	public void testOnDoneOk() {
		sp.onDone(onOk, onError, onCancel);
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		sp.unblock();
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}

	@Test
	public void testOnDoneError() {
		sp.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.error(new Exception());
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(1, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}

	@Test
	public void testOnDoneCancel() throws Exception {
		sp.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(1, cancel.get());
		try { sp.blockThrow(0); throw new AssertionError(); }
		catch (CancelException e) {
			Assert.assertEquals("test", e.getMessage());
		}
		testListenersToString();
	}
		
	@Test
	public void testOnDoneOk2SP() {
		sp.onDone(onOk, sp2);
		sp2.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.unblock();
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}

	@Test
	public void testOnDoneErrorTransferWithErrorOrCancel() {
		sp.onDone(onOk, sp2);
		sp2.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.error(new Exception());
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(1, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}

	@Test
	public void testOnDoneErrorTransfer() {
		sp.onDone(sp2);
		sp2.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.error(new Exception());
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(1, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}

	@Test
	public void testOnDoneCancelTransferWithErrorOrCancel() {
		sp.onDone(onOk, sp2);
		sp2.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(1, cancel.get());
		testListenersToString();
	}

	@Test
	public void testOnDoneCancelTransfer() {
		sp.onDone(sp2);
		sp2.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(1, cancel.get());
		testListenersToString();
	}
	
	@Test
	public void testOnDoneOkTransfer() {
		sp.onDone(sp2);
		sp2.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.unblock();
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}

	@Test
	public void testOnDoneOkWithErrorOrCancel() {
		sp.onDone(onOk, sp2);
		sp2.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.unblock();
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}

	@Test
	public void testOnDoneOkRunnable() {
		sp.onDone(sp2::unblock);
		sp2.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.unblock();
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}

	@Test
	public void testOnDoneErrorRunnable() {
		sp.onDone(sp2::unblock);
		sp2.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.error(new Exception());
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}

	@Test
	public void testOnDoneCancelRunnable() {
		sp.onDone(sp2::unblock);
		sp2.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}
	
	@Test
	public void testCancelWithErrorConverter() {
		sp.onDone(sp2, e -> e);
		testListenersToString();
		sp.cancel(new CancelException("test"));
		Assert.assertTrue(sp2.isCancelled());
		testListenersToString();
	}

	@Test
	public void testErrorWithConverter() {
		sp.onDone(sp2, e -> e);
		testListenersToString();
		sp.error(new Exception("test"));
		Assert.assertTrue(sp2.hasError());
		testListenersToString();
	}

	@Test
	public void testListenersOk() {
		sp.onSuccess(onOk);
		sp.onError(onError);
		sp.onCancel(onCancel);
		testListenersToString();
		sp.unblock();
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}
	
	@Test
	public void testListenersError() {
		sp.onSuccess(onOk);
		sp.onError(onError);
		sp.onCancel(onCancel);
		testListenersToString();
		sp.error(new Exception());
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(1, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}
	
	@Test
	public void testListenersCancel() {
		sp.onSuccess(onOk);
		sp.onError(onError);
		sp.onCancel(onCancel);
		testListenersToString();
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(1, cancel.get());
		testListenersToString();
	}
	
	@Test
	public void testToAsyncSupplierOk() {
		AsyncSupplier<Void, Exception> aw = sp.toAsyncSupplier();
		aw.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.unblock();
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}

	@Test
	public void testToAsyncSupplierError() {
		AsyncSupplier<Void, Exception> aw = sp.toAsyncSupplier();
		aw.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.error(new Exception());
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(1, error.get());
		Assert.assertEquals(0, cancel.get());
		testListenersToString();
	}

	@Test
	public void testToAsyncSupplierCancel() {
		AsyncSupplier<Void, Exception> aw = sp.toAsyncSupplier();
		aw.onDone(onOk, onError, onCancel);
		testListenersToString();
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(0, error.get());
		Assert.assertEquals(1, cancel.get());
		testListenersToString();
	}
	
	@Test
	public void testThenStartOkThenCancel() {
		sp.thenStart(Task.cpu("test", Task.Priority.NORMAL, t -> {
			throw new CancelException("test");
		}), sp2);
		testListenersToString();
		sp.unblock();
		sp2.block(0);
		Assert.assertTrue(sp2.isCancelled());
		testListenersToString();
	}
	
	@Test
	public void testThenStartOk() {
		sp.thenStart(Task.cpu("test", Task.Priority.NORMAL, t -> { ok.inc(); sp2.unblock(); return null; }), error::inc);
		testListenersToString();
		sp.unblock();
		sp2.block(10000);
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		testListenersToString();
	}
	
	@Test
	public void testThenStartError() {
		sp.thenStart(Task.cpu("test", Task.Priority.NORMAL, t -> { ok.inc(); sp2.unblock(); return null; }), error::inc);
		testListenersToString();
		sp.error(new Exception());
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(1, error.get());
		testListenersToString();
	}
	
	@Test
	public void testThenStartCancel() {
		sp.thenStart(Task.cpu("test", Task.Priority.NORMAL, t -> { ok.inc(); sp2.unblock(); return null; }), error::inc);
		testListenersToString();
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(0, ok.get());
		Assert.assertEquals(1, error.get());
		testListenersToString();
	}
	
	@Test
	public void testThenStartEvenOnErrorOk() {
		sp.thenStart("test", Task.Priority.NORMAL, () -> { ok.inc(); sp2.unblock(); }, true);
		testListenersToString();
		sp.unblock();
		sp2.block(10000);
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		testListenersToString();
	}
	
	@Test
	public void testThenStartEvenOnErrorError() {
		sp.thenStart("test", Task.Priority.NORMAL, () -> { ok.inc(); sp2.unblock(); }, true);
		testListenersToString();
		sp.error(new Exception());
		sp2.block(10000);
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		testListenersToString();
	}
	
	@Test
	public void testThenStartEvenOnErrorCancel() {
		sp.thenStart("test", Task.Priority.NORMAL, () -> { ok.inc(); sp2.unblock(); }, true);
		testListenersToString();
		sp.cancel(new CancelException("test"));
		sp2.block(10000);
		Assert.assertEquals(1, ok.get());
		Assert.assertEquals(0, error.get());
		testListenersToString();
	}
	
	@Test
	public void testThenStartRunnableOk() {
		Async<Exception> sp3 = new Async<>();
		sp.thenStart("test", Task.Priority.NORMAL, () -> { ok.inc(); sp2.unblock(); }, sp3);
		testListenersToString();
		sp.unblock();
		sp2.block(10000);
		Assert.assertEquals(1, ok.get());
		Assert.assertFalse(sp3.isDone());
		testListenersToString();
	}
	
	@Test
	public void testThenStartRunnableError() {
		Async<Exception> sp3 = new Async<>();
		sp.thenStart("test", Task.Priority.NORMAL, () -> { ok.inc(); sp2.unblock(); }, sp3);
		testListenersToString();
		sp.error(new Exception());
		sp3.block(10000);
		Assert.assertEquals(0, ok.get());
		Assert.assertTrue(sp3.hasError());
		testListenersToString();
	}
	
	@Test
	public void testThenStartRunnableCancel() {
		Async<Exception> sp3 = new Async<>();
		sp.thenStart("test", Task.Priority.NORMAL, () -> { ok.inc(); sp2.unblock(); }, sp3);
		testListenersToString();
		sp.cancel(new CancelException("test"));
		sp3.block(10000);
		Assert.assertEquals(0, ok.get());
		Assert.assertTrue(sp3.isCancelled());
		testListenersToString();
	}
	
	@Test
	public void testThenDoOrStartRunnableOkAsync() {
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); });
		Assert.assertEquals(0, ok.get());
		testListenersToString();
		sp.unblock();
		sp2.block(0);
		Assert.assertEquals(51, ok.get());
		testListenersToString();
	}
	
	@Test
	public void testThenDoOrStartRunnableOkSync() {
		Assert.assertEquals(0, ok.get());
		sp.unblock();
		testListenersToString();
		Assert.assertEquals(0, ok.get());
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); });
		Assert.assertEquals(51, ok.get());
		Assert.assertTrue(sp2.isSuccessful());
		testListenersToString();
	}
	
	@Test
	public void testThenDoOrStartRunnableErrorAsync() {
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); });
		Assert.assertEquals(0, ok.get());
		testListenersToString();
		sp.error(new Exception());
		sp2.block(0);
		Assert.assertEquals(51, ok.get());
		testListenersToString();
	}
	
	@Test
	public void testThenDoOrStartRunnableErrorSync() {
		Assert.assertEquals(0, ok.get());
		sp.error(new Exception());
		Assert.assertEquals(0, ok.get());
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); });
		Assert.assertEquals(51, ok.get());
		Assert.assertTrue(sp2.isSuccessful());
	}
	
	@Test
	public void testThenDoOrStartRunnableCancelAsync() {
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); });
		Assert.assertEquals(0, ok.get());
		testListenersToString();
		sp.cancel(new CancelException("test"));
		sp2.block(0);
		Assert.assertEquals(51, ok.get());
		testListenersToString();
	}
	
	@Test
	public void testThenDoOrStartRunnableCancelSync() {
		Assert.assertEquals(0, ok.get());
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(0, ok.get());
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); });
		Assert.assertEquals(51, ok.get());
		Assert.assertTrue(sp2.isSuccessful());
	}
	
	@Test
	public void testThenDoOrStartRunnableWithOnErrorOrCancelOkAsync() {
		Async<Exception> sp3 = new Async<>();
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3);
		Assert.assertEquals(0, ok.get());
		testListenersToString();
		sp.unblock();
		sp2.block(0);
		Assert.assertEquals(51, ok.get());
		Assert.assertFalse(sp3.isDone());
		testListenersToString();
	}
	
	@Test
	public void testThenDoOrStartRunnableWithOnErrorOrCancelOkSync() {
		Async<Exception> sp3 = new Async<>();
		Assert.assertEquals(0, ok.get());
		sp.unblock();
		Assert.assertEquals(0, ok.get());
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3);
		Assert.assertEquals(51, ok.get());
		Assert.assertTrue(sp2.isSuccessful());
		Assert.assertFalse(sp3.isDone());
	}
	
	@Test
	public void testThenDoOrStartRunnableWithOnErrorOrCancelErrorAsync() {
		Async<Exception> sp3 = new Async<>();
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3);
		Assert.assertEquals(0, ok.get());
		testListenersToString();
		sp.error(new Exception());
		Assert.assertEquals(0, ok.get());
		Assert.assertTrue(sp3.hasError());
		testListenersToString();
	}
	
	@Test
	public void testThenDoOrStartRunnableWithOnErrorOrCancelErrorSync() {
		Async<Exception> sp3 = new Async<>();
		Assert.assertEquals(0, ok.get());
		sp.error(new Exception());
		Assert.assertEquals(0, ok.get());
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3);
		Assert.assertEquals(0, ok.get());
		Assert.assertTrue(sp3.hasError());
	}
	
	@Test
	public void testThenDoOrStartRunnableeWithOnErrorOrCancelCancelAsync() {
		Async<Exception> sp3 = new Async<>();
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3);
		Assert.assertEquals(0, ok.get());
		testListenersToString();
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(0, ok.get());
		Assert.assertTrue(sp3.isCancelled());
		testListenersToString();
	}
	
	@Test
	public void testThenDoOrStartRunnableeWithOnErrorOrCancelCancelSync() {
		Async<Exception> sp3 = new Async<>();
		Assert.assertEquals(0, ok.get());
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(0, ok.get());
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3);
		Assert.assertEquals(0, ok.get());
		Assert.assertTrue(sp3.isCancelled());
	}
	
	@Test
	public void testThenDoOrStartRunnableWithOnErrorOrCancelAndErrorConverterOkAsync() {
		Async<IOException> sp3 = new Async<>();
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3, e -> new IOException());
		Assert.assertEquals(0, ok.get());
		testListenersToString();
		sp.unblock();
		sp2.block(0);
		Assert.assertEquals(51, ok.get());
		Assert.assertFalse(sp3.isDone());
		testListenersToString();
	}
	
	@Test
	public void testThenDoOrStartRunnableWithOnErrorOrCancelAndErrorConverterOkSync() {
		Async<IOException> sp3 = new Async<>();
		Assert.assertEquals(0, ok.get());
		sp.unblock();
		Assert.assertEquals(0, ok.get());
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3, e -> new IOException());
		Assert.assertEquals(51, ok.get());
		Assert.assertTrue(sp2.isSuccessful());
		Assert.assertFalse(sp3.isDone());
	}
	
	@Test
	public void testThenDoOrStartRunnableWithOnErrorOrCancelAndErrorConverterErrorAsync() {
		Async<IOException> sp3 = new Async<>();
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3, e -> new IOException());
		Assert.assertEquals(0, ok.get());
		testListenersToString();
		sp.error(new Exception());
		Assert.assertEquals(0, ok.get());
		Assert.assertTrue(sp3.hasError());
		testListenersToString();
	}
	
	@Test
	public void testThenDoOrStartRunnableWithOnErrorOrCancelAndErrorConverterErrorSync() {
		Async<IOException> sp3 = new Async<>();
		Assert.assertEquals(0, ok.get());
		sp.error(new Exception());
		Assert.assertEquals(0, ok.get());
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3, e -> new IOException());
		Assert.assertEquals(0, ok.get());
		Assert.assertTrue(sp3.hasError());
	}
	
	@Test
	public void testThenDoOrStartRunnableeWithOnErrorOrCancelAndErrorConverterCancelAsync() {
		Async<IOException> sp3 = new Async<>();
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3, e -> new IOException());
		Assert.assertEquals(0, ok.get());
		testListenersToString();
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(0, ok.get());
		Assert.assertTrue(sp3.isCancelled());
		testListenersToString();
	}
	
	@Test
	public void testThenDoOrStartRunnableeWithOnErrorOrCancelAndErrorConverterCancelSync() {
		Async<IOException> sp3 = new Async<>();
		Assert.assertEquals(0, ok.get());
		sp.cancel(new CancelException("test"));
		Assert.assertEquals(0, ok.get());
		sp.thenDoOrStart("test", Task.Priority.NORMAL, () -> { ok.set(51); sp2.unblock(); }, sp3, e -> new IOException());
		Assert.assertEquals(0, ok.get());
		Assert.assertTrue(sp3.isCancelled());
	}
	
	@Test
	public void testConstructors() {
		sp = new Async<>(new CancelException("test"));
		Assert.assertTrue(sp.isCancelled());
	}
	
	@Test
	public void testUnblockAsynchronously() {
		Task.cpu("test", Task.Priority.NORMAL, t -> {
			sp.unblock();
			sp2.blockPause(5000);
			return null;
		}).start();
		sp.block(0);
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		sp2.unblock();
		sp2.blockPause(1);
	}
	
	@Test
	public void testOnErrorAndCancelWithError() {
		sp.onError(e -> {});
		sp.onErrorOrCancel(() -> ok.set(51));
		Assert.assertEquals(0, ok.get());
		testListenersToString();
		sp.error(new Exception());
		Assert.assertEquals(51, ok.get());
		testListenersToString();
	}
	
	@Test
	public void testOnErrorOrCancelWithOk() {
		sp.onErrorOrCancel(() -> ok.set(51));
		Assert.assertEquals(0, ok.get());
		testListenersToString();
		sp.unblock();
		Assert.assertEquals(0, ok.get());
		testListenersToString();
	}
	
	@Test
	public void testOnDoneAsyncSupplierWithSupplier() {
		AsyncSupplier<Integer, Exception> as = new AsyncSupplier<>();
		sp.onDone(as, () -> Integer.valueOf(567));
		testListenersToString();
		sp.unblock();
		Assert.assertTrue(as.isDone());
		Assert.assertEquals(567, as.getResult().intValue());
		testListenersToString();
	}
	
	@Test
	public void testFutureOk() {
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
	}
	
	@Test
	public void testFutureCancel() {
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
	}
	
	@Test
	public void testFutureError() {
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
	
	private void testListenersToString() {
		for (Object o : sp.getAllListeners())
			o.toString();
	}
	
	@Test
	public void testBlockPauseWithWarning() {
		Async<Exception> a = new Async<>();
		Task.cpu("unblock", Priority.IMPORTANT, t -> {
			a.unblock();
			return null;
		}).executeIn(5).start();
		a.blockPause(1);
	}
	
	@Test
	public void testConvertError() {
		Async<IOException> a = new Async<>();
		Async<RuntimeException> b = new Async<>(a, e -> new RuntimeException("ok", e));
		a.error(new IOException());
		Assert.assertNotNull(b.getError());
		Assert.assertEquals("ok", b.getError().getMessage());
	}
	
}
