package net.lecousin.framework.core.tests.concurrent.async;

import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.MutualExclusion;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestMutualExclusion extends LCCoreAbstractTest {

	@Test
	public void test() {
		MutualExclusion<Exception> mutex = new MutualExclusion<>();
		Assert.assertTrue(mutex.isDone());
		mutex.lock();
		Assert.assertFalse(mutex.isDone());
		mutex.unlock();
		Assert.assertTrue(mutex.isDone());
		mutex.lock();
		Assert.assertFalse(mutex.isDone());
		mutex.lock();
		Assert.assertFalse(mutex.isDone());
		mutex.unlock();
		Assert.assertFalse(mutex.isDone());
		mutex.unlock();
		Assert.assertTrue(mutex.isDone());
		mutex.lock();
		Assert.assertFalse(mutex.isDone());
		
		mutex.hasError();
		mutex.error(new Exception("test"));
		mutex.hasError();
		mutex.getError();
		Assert.assertTrue(mutex.isDone());
		mutex.lock();
		Assert.assertTrue(mutex.isDone());
		mutex.unlock();
		Assert.assertTrue(mutex.isDone());
		
		mutex = new MutualExclusion<>();
		Assert.assertTrue(mutex.isDone());
		mutex.lock();
		Assert.assertFalse(mutex.isDone());
		mutex.isCancelled();
		mutex.cancel(new CancelException("test"));
		mutex.isCancelled();
		mutex.getCancelEvent();
		Assert.assertTrue(mutex.isDone());
		mutex.lock();
		Assert.assertTrue(mutex.isDone());
		mutex.unlock();
		Assert.assertTrue(mutex.isDone());
		
		mutex = new MutualExclusion<>();
		mutex.getAllListeners();
		mutex.onDone(() -> {});
		mutex.lock();
		mutex.onDone(() -> {});
		mutex.getAllListeners();
		mutex.unlock();
	}
	
}
