package net.lecousin.framework.core.tests.concurrent.async;

import net.lecousin.framework.concurrent.async.ReadWriteLockPoint;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Test;

public class TestReadWriteLock extends LCCoreAbstractTest {

	@Test
	public void test() {
		ReadWriteLockPoint lock = new ReadWriteLockPoint();
		lock.startRead();
		lock.startRead();
		lock.endRead();
		Task.cpu("Test", Task.Priority.IMPORTANT, t -> { lock.endRead(); return null; }).executeIn(500).start();
		lock.startWrite();
		Task.cpu("Test", Task.Priority.IMPORTANT, t -> { lock.endWrite(); return null; }).executeIn(500).start();
		lock.startRead();
		lock.endRead();
	}
	
}
