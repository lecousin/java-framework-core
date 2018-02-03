package net.lecousin.framework.core.tests.progress;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.progress.MultiTaskProgress;
import net.lecousin.framework.progress.WorkProgress;

import org.junit.Assert;
import org.junit.Test;

public class TestMultiTaskProgress extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		MultiTaskProgress p = new MultiTaskProgress("Test");
		WorkProgress t1 = p.createTaskProgress(1000, "Task1");
		WorkProgress t2 = p.createTaskProgress(100, "Task2");
		WorkProgress t3 = p.createTaskProgress(300, "Task3");
		Assert.assertEquals(1400, p.getAmount());
		Assert.assertEquals(0, p.getPosition());
		Assert.assertEquals(1400, p.getRemainingWork());
		t1.progress(150);
		Assert.assertEquals(1400, p.getAmount());
		Assert.assertEquals(150, p.getPosition());
		Assert.assertEquals(1250, p.getRemainingWork());
		Assert.assertEquals(1000, t1.getAmount());
		Assert.assertEquals(150, t1.getPosition());
		Assert.assertEquals(850, t1.getRemainingWork());
		t2.done();
		Assert.assertEquals(1400, p.getAmount());
		Assert.assertEquals(250, p.getPosition());
		Assert.assertEquals(1150, p.getRemainingWork());
		Assert.assertEquals(100, t2.getAmount());
		Assert.assertEquals(100, t2.getPosition());
		Assert.assertEquals(0, t2.getRemainingWork());
		t3.progress(300);
		Assert.assertEquals(1400, p.getAmount());
		Assert.assertEquals(550, p.getPosition());
		Assert.assertEquals(850, p.getRemainingWork());
		Assert.assertEquals(300, t3.getAmount());
		Assert.assertEquals(300, t3.getPosition());
		Assert.assertEquals(0, t3.getRemainingWork());
		t3.setAmount(600);
		t3.setPosition(400);
		Assert.assertEquals(1400, p.getAmount());
		Assert.assertEquals(450, p.getPosition());
		Assert.assertEquals(950, p.getRemainingWork());
		Assert.assertEquals(600, t3.getAmount());
		Assert.assertEquals(400, t3.getPosition());
		Assert.assertEquals(200, t3.getRemainingWork());
	}
	
}
