package net.lecousin.framework.core.tests.concurrent.synch;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.MutualExclusion;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestMutualExclusion extends LCCoreAbstractTest {

	@Test(timeout=120000)
	public void test() {
		MutualExclusion<Exception> mutex = new MutualExclusion<>();
		Assert.assertTrue(mutex.isUnblocked());
		mutex.lock();
		Assert.assertFalse(mutex.isUnblocked());
		mutex.unlock();
		Assert.assertTrue(mutex.isUnblocked());
		mutex.lock();
		Assert.assertFalse(mutex.isUnblocked());
		mutex.lock();
		Assert.assertFalse(mutex.isUnblocked());
		mutex.unlock();
		Assert.assertFalse(mutex.isUnblocked());
		mutex.unlock();
		Assert.assertTrue(mutex.isUnblocked());
		mutex.lock();
		Assert.assertFalse(mutex.isUnblocked());
		
		mutex.hasError();
		mutex.error(new Exception("test"));
		mutex.hasError();
		mutex.getError();
		Assert.assertTrue(mutex.isUnblocked());
		mutex.lock();
		Assert.assertTrue(mutex.isUnblocked());
		mutex.unlock();
		Assert.assertTrue(mutex.isUnblocked());
		
		mutex = new MutualExclusion<>();
		Assert.assertTrue(mutex.isUnblocked());
		mutex.lock();
		Assert.assertFalse(mutex.isUnblocked());
		mutex.isCancelled();
		mutex.cancel(new CancelException("test"));
		mutex.isCancelled();
		mutex.getCancelEvent();
		Assert.assertTrue(mutex.isUnblocked());
		mutex.lock();
		Assert.assertTrue(mutex.isUnblocked());
		mutex.unlock();
		Assert.assertTrue(mutex.isUnblocked());
		
		mutex = new MutualExclusion<>();
		mutex.getAllListeners();
		mutex.listenInline(() -> {});
		mutex.lock();
		mutex.listenInline(() -> {});
		mutex.getAllListeners();
		mutex.unlock();
	}
	
}
