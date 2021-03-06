package net.lecousin.framework.core.tests.progress;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.progress.MultiTaskProgress;
import net.lecousin.framework.progress.WorkProgress;
import net.lecousin.framework.progress.WorkProgress.MultiTask.SubTask;
import net.lecousin.framework.progress.WorkProgressImpl;

import org.junit.Assert;
import org.junit.Test;

public class TestMultiTaskProgress extends LCCoreAbstractTest {

	@Test
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
		
		p = new MultiTaskProgress("Test");
		t1 = new WorkProgressImpl(1000);
		p.addTask(t1, 150);
		p.doneOnSubTasksDone();
		t2 = new WorkProgressImpl(2000);
		p.addTask(t2, 200);
		t3 = p.createTaskProgress(300, "Task3");
		Assert.assertEquals(650, p.getAmount());
		Assert.assertFalse(p.getSynch().isDone());
		Assert.assertEquals(0, p.getPosition());
		t1.done();
		try { Thread.sleep(1000); }
		catch (InterruptedException e) {}
		Assert.assertFalse(p.getSynch().isDone());
		Assert.assertEquals(150, p.getPosition());
		t2.done();
		try { Thread.sleep(1000); }
		catch (InterruptedException e) {}
		Assert.assertFalse(p.getSynch().isDone());
		Assert.assertEquals(350, p.getPosition());
		t3.done();
		try { Thread.sleep(1000); }
		catch (InterruptedException e) {}
		Assert.assertTrue(p.getSynch().isDone());
		Assert.assertEquals(650, p.getPosition());
		
		p = new MultiTaskProgress("Test");
		Assert.assertEquals(0, p.getTasks().size());
		t1 = new WorkProgressImpl(1000);
		SubTask st = p.addTask(t1, 150);
		Assert.assertEquals(1, p.getTasks().size());
		p.removeTask(st);
		Assert.assertEquals(0, p.getTasks().size());
		t1 = new WorkProgressImpl(1000);
		st = p.addTask(t1, 150);
		p.doneOnSubTasksDone();
		p.doneOnSubTasksDone();
		Assert.assertEquals(1, p.getTasks().size());
		p.removeTask(st);
		Assert.assertEquals(0, p.getTasks().size());
		Assert.assertTrue(p.getSynch().isDone());
		
		p = new MultiTaskProgress("Test");
		t1 = new WorkProgressImpl(1000);
		st = p.addTask(t1, 150);
		p.doneOnSubTasksDone();
		t2 = new WorkProgressImpl(2000);
		st = p.addTask(t2, 150);
		t1.done();
		p.removeTask(st);
		Assert.assertTrue(p.getSynch().isDone());
		Assert.assertFalse(p.getSynch().hasError());
		
		p = new MultiTaskProgress("Test");
		t1 = new WorkProgressImpl(1000);
		st = p.addTask(t1, 150);
		p.doneOnSubTasksDone();
		t2 = new WorkProgressImpl(2000);
		st = p.addTask(t2, 150);
		t2.done();
		p.removeTask(st);
		Assert.assertFalse(p.getSynch().isDone());
		t1.error(new Exception());
		Assert.assertTrue(p.getSynch().isDone());
		Assert.assertTrue(p.getSynch().hasError());
	}
	
}
