package net.lecousin.framework.core.tests.concurrent.synch;

import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestSynchronizationPoint extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		Assert.assertEquals(0, sp.getAllListeners().size());
		sp.listenInline(() -> {});
		Assert.assertEquals(1, sp.getAllListeners().size());
	}
	
}
