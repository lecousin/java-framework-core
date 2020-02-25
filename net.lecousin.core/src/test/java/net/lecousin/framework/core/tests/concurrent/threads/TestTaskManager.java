package net.lecousin.framework.core.tests.concurrent.threads;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.TaskManagerMonitor.Configuration;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.exception.NoException;

import org.junit.Assert;
import org.junit.Test;

public class TestTaskManager extends LCCoreAbstractTest {

	@Test
	public void testPutAsideAndKill() {
		Task<?, ?> task = Task.unmanaged("Test never ending task", () -> {
			Thread.sleep(60000);
			return null;
		});
		Configuration previousConfig = Threading.getUnmanagedTaskManager().getMonitor().getConfiguration();
		Configuration newConfig = new Configuration(1, 1000, 3000, false);
		Threading.setUnmanagedMonitorConfiguration(newConfig);
		task.start();
		task.getOutput().block(0);
		Assert.assertTrue(task.isCancelling());
		Assert.assertTrue(task.isCancelled());
		Assert.assertNotNull(task.getCancelEvent());
		Assert.assertFalse(task.isSuccessful());
		Assert.assertTrue(task.isDone());
		Assert.assertTrue(task.isStarted());
		Assert.assertFalse(task.isRunning());
		Threading.setUnmanagedMonitorConfiguration(previousConfig);
	}
	
	@Test
	public void testBlockInUnmanaged() throws Exception {
		Async<NoException> a = new Async<>();
		Async<NoException> b = new Async<>();
		Task<?, ?> task = Task.unmanaged("Test blocking task", () -> {
			b.unblock();
			a.block(0);
			return null;
		});
		task.start();
		b.blockThrow(0);
		Thread.sleep(500);
		a.unblock();
	}
	
}
