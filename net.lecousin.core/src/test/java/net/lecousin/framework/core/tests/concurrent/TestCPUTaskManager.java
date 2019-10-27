package net.lecousin.framework.core.tests.concurrent;

import net.lecousin.framework.concurrent.CPUTaskManager;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestCPUTaskManager extends LCCoreAbstractTest {

	@Test
	public void test() {
		CPUTaskManager cpu = (CPUTaskManager)Threading.getCPUTaskManager();
		
		Assert.assertEquals(Threading.CPU, Threading.getCPUTaskManager().getResource());
		
		String name = cpu.getName();
		cpu.setName("test");
		Assert.assertEquals("test", cpu.getName());
		cpu.setName(name);
	}
	
}
