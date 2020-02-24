package net.lecousin.framework.core.tests.concurrent.threads;

import net.lecousin.framework.concurrent.threads.Task;
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
		Threading.getUnmanagedTaskManager().getMonitor().getConfiguration().taskExecutionMillisecondsBeforeToWarn = 1;
		Threading.getUnmanagedTaskManager().getMonitor().getConfiguration().taskExecutionMillisecondsBeforeToPutAside = 1000;
		Threading.getUnmanagedTaskManager().getMonitor().getConfiguration().taskExecutionMillisecondsBeforeToKill = 3000;
		task.start();
		Threading.getUnmanagedTaskManager().getMonitor().checkNow();
		task.getOutput().block(0);
		Assert.assertTrue(task.isCancelled());
		Threading.getUnmanagedTaskManager().getMonitor().getConfiguration().taskExecutionMillisecondsBeforeToWarn = 60 * 1000;
		Threading.getUnmanagedTaskManager().getMonitor().getConfiguration().taskExecutionMillisecondsBeforeToPutAside = 5 * 60 * 1000;
		Threading.getUnmanagedTaskManager().getMonitor().getConfiguration().taskExecutionMillisecondsBeforeToKill = 15 * 60 * 1000;
	}
	
}
