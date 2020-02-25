package net.lecousin.framework.core.tests.concurrent.threads;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.TaskManagerMonitor.Configuration;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

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
	
}
