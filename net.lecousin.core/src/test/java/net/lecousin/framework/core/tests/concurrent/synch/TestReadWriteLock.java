package net.lecousin.framework.core.tests.concurrent.synch;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ReadWriteLockPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Test;

public class TestReadWriteLock extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		ReadWriteLockPoint lock = new ReadWriteLockPoint();
		lock.startRead();
		lock.startRead();
		lock.endRead();
		new Task.Cpu.FromRunnable("Test", Task.PRIORITY_IMPORTANT, () -> { lock.endRead(); }).start();
		lock.startWrite();
		new Task.Cpu.FromRunnable("Test", Task.PRIORITY_IMPORTANT, () -> { lock.endWrite(); }).start();
		lock.startRead();
		lock.endRead();
	}
	
}
