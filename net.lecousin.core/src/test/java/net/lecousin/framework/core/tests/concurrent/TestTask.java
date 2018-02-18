package net.lecousin.framework.core.tests.concurrent;

import java.io.File;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestTask extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void test() throws Exception {
		Task.Cpu.Parameter<Integer, Long, Exception> cpuParam = new Task.Cpu.Parameter<Integer, Long, Exception>("test", Task.PRIORITY_NORMAL) {
			@Override
			public Long run() {
				return Long.valueOf(getParameter().longValue());
			}
		};
		cpuParam.setParameter(Integer.valueOf(51));
		Assert.assertEquals(51, cpuParam.getParameter().intValue());
		cpuParam.start();
		Assert.assertEquals(51, cpuParam.getOutput().blockResult(0).longValue());

		cpuParam = new Task.Cpu.Parameter<Integer, Long, Exception>("test", Task.PRIORITY_NORMAL, (p) -> {}) {
			@Override
			public Long run() {
				return Long.valueOf(getParameter().longValue());
			}
		};
		cpuParam.setParameter(Integer.valueOf(51));
		Assert.assertEquals(51, cpuParam.getParameter().intValue());
		Assert.assertFalse(cpuParam.isStarted());
		Assert.assertFalse(cpuParam.isRunning());
		Assert.assertFalse(cpuParam.isCancelled());
		Assert.assertFalse(cpuParam.isCancelling());
		Assert.assertFalse(cpuParam.isSuccessful());
		Assert.assertFalse(cpuParam.isDone());
		Assert.assertFalse(cpuParam.hasError());
		cpuParam.start();
		Assert.assertEquals(51, cpuParam.getOutput().blockResult(0).longValue());
		Assert.assertEquals("test", cpuParam.getDescription());
		Assert.assertTrue(cpuParam.isStarted());
		Assert.assertFalse(cpuParam.isRunning());
		Assert.assertFalse(cpuParam.isCancelled());
		Assert.assertFalse(cpuParam.isCancelling());
		Assert.assertTrue(cpuParam.isSuccessful());
		Assert.assertTrue(cpuParam.isDone());
		Assert.assertNull(cpuParam.getError());
		Assert.assertNull(cpuParam.getCancelEvent());
		Assert.assertEquals(Task.PRIORITY_NORMAL, cpuParam.getPriority());
		Assert.assertEquals(cpuParam, cpuParam.getOutput().getTask());
		
		Task.Done<Integer, Exception> done = new Task.Done<Integer, Exception>(Integer.valueOf(10), null);
		Assert.assertTrue(done.isDone());
		Assert.assertEquals(10, done.getOutput().blockResult(0).intValue());
		done.start();
		done.run();

		done = new Task.Done<Integer, Exception>(null, new Exception());
		Assert.assertTrue(done.isDone());
		Assert.assertTrue(done.hasError());
		
		Task<Object,Exception> task = new Task<Object,Exception>(Threading.CPU, "test", Task.PRIORITY_NORMAL) {
			@Override
			public Object run() {
				return null;
			}
		}.start();
		Assert.assertEquals(Threading.getCPUTaskManager(), task.getTaskManager());


		new Task<Object,Exception>(Threading.CPU, "test", Task.PRIORITY_NORMAL, (p) -> {}) {
			@Override
			public Object run() {
				return null;
			}
		}.start();
		
		new Task.Cpu.FromRunnable(() -> {}, "test", Task.PRIORITY_NORMAL).start();
		
		Task.OnFile.Parameter<Integer, Long, Exception> fileParam = new Task.OnFile.Parameter<Integer, Long, Exception>(new File("."), "test", Task.PRIORITY_NORMAL) {
			@Override
			public Long run() {
				return null;
			}
		};
		fileParam.start(Integer.valueOf(1234));
		Assert.assertEquals(1234, fileParam.getParameter().intValue());
		
		fileParam = new Task.OnFile.Parameter<Integer, Long, Exception>(new File("."), "test", Task.PRIORITY_NORMAL, (p) -> {}) {
			@Override
			public Long run() {
				return null;
			}
		};
		fileParam.start(Integer.valueOf(5678));
		Assert.assertEquals(5678, fileParam.getParameter().intValue());
		
		Task.Unmanaged<Integer, Exception> unmanaged = new Task.Unmanaged<Integer, Exception>("test", Task.PRIORITY_NORMAL) {
			@Override
			public Integer run() {
				return Integer.valueOf(111);
			}
		};
		unmanaged.startOn(true);
		Assert.assertEquals(111, unmanaged.getOutput().blockResult(0).intValue());
	}
	
}
