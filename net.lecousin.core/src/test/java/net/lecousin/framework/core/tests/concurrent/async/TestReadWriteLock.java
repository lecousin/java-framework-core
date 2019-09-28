package net.lecousin.framework.core.tests.concurrent.async;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.ReadWriteLockPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Test;

public class TestReadWriteLock extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		ReadWriteLockPoint lock = new ReadWriteLockPoint();
		lock.startRead();
		lock.startRead();
		lock.endRead();
		new Task.Cpu.FromRunnable("Test", Task.PRIORITY_IMPORTANT, () -> { lock.endRead(); }).executeIn(500).start();
		lock.startWrite();
		new Task.Cpu.FromRunnable("Test", Task.PRIORITY_IMPORTANT, () -> { lock.endWrite(); }).executeIn(500).start();
		lock.startRead();
		lock.endRead();
	}
	
}
