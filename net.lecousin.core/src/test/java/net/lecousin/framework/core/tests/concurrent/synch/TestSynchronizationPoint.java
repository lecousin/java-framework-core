package net.lecousin.framework.core.tests.concurrent.synch;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.mutable.MutableInteger;

import org.junit.Assert;
import org.junit.Test;

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
		sp.unblock();
		Assert.assertEquals(9, ok.get());
		
		sp = new SynchronizationPoint<>();
		sp.onError(onError);
		sp.error(new Exception());
		Assert.assertEquals(6, error.get());
		
		sp = new SynchronizationPoint<>();
		sp.onException(onError);
		sp.error(new Exception());
		Assert.assertEquals(7, error.get());
		
		sp = new SynchronizationPoint<>();
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
	}
	
}
