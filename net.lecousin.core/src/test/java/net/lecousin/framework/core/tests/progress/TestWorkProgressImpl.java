package net.lecousin.framework.core.tests.progress;

import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.progress.WorkProgress;
import net.lecousin.framework.progress.WorkProgressImpl;

import org.junit.Assert;
import org.junit.Test;

public class TestWorkProgressImpl extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		WorkProgressImpl p = new WorkProgressImpl(1000);
		Assert.assertEquals(1000, p.getAmount());
		Assert.assertEquals(1000, p.getRemainingWork());
		Assert.assertEquals("", p.getText());
		Assert.assertEquals("", p.getSubText());
		p = new WorkProgressImpl(1000, "Hello");
		Assert.assertEquals(1000, p.getAmount());
		Assert.assertEquals(1000, p.getRemainingWork());
		Assert.assertEquals("Hello", p.getText());
		Assert.assertEquals("", p.getSubText());
		p = new WorkProgressImpl(1000, "Hello", "World");
		Assert.assertEquals(1000, p.getAmount());
		Assert.assertEquals(1000, p.getRemainingWork());
		Assert.assertEquals("Hello", p.getText());
		Assert.assertEquals("World", p.getSubText());
		p.progress(100);
		Assert.assertEquals(1000, p.getAmount());
		Assert.assertEquals(900, p.getRemainingWork());
		Assert.assertEquals(100, p.getPosition());
		p.setText("Test");
		Assert.assertEquals("Test", p.getText());
		p.setSubText("12345");
		Assert.assertEquals("12345", p.getSubText());
		Async<Exception> changed = new Async<>();
		Runnable listener = () -> {
			changed.unblock();
		};
		p.listen(listener);
		Assert.assertFalse(changed.isDone());
		p.progress(200);
		Assert.assertEquals(1000, p.getAmount());
		Assert.assertEquals(700, p.getRemainingWork());
		Assert.assertEquals(300, p.getPosition());
		changed.blockThrow(0);
		changed.reset();
		p.setText("Toto");
		Assert.assertEquals("Toto", p.getText());
		changed.blockThrow(0);
		changed.reset();
		p.setSubText("Titi");
		Assert.assertEquals("Titi", p.getSubText());
		changed.blockThrow(0);
		changed.reset();
		p.unlisten(listener);
		p.unlisten(listener);
		Runnable listener1 = () -> {};
		Runnable listener2 = () -> {};
		p.listen(listener1);
		p.listen(listener2);
		p.unlisten(listener1);
		p.unlisten(listener2);
		p.progress(1);
		Assert.assertEquals(1000, p.getAmount());
		Assert.assertEquals(699, p.getRemainingWork());
		Assert.assertEquals(301, p.getPosition());
		changed.blockThrow(500);
		Assert.assertFalse(changed.isDone());
		p.setAmount(900);
		Assert.assertEquals(900, p.getAmount());
		Assert.assertEquals(599, p.getRemainingWork());
		Assert.assertEquals(301, p.getPosition());
		p.setAmount(900);
		Assert.assertEquals(900, p.getAmount());
		Assert.assertEquals(599, p.getRemainingWork());
		Assert.assertEquals(301, p.getPosition());
		p.setPosition(400);
		Assert.assertEquals(900, p.getAmount());
		Assert.assertEquals(500, p.getRemainingWork());
		Assert.assertEquals(400, p.getPosition());
		p.setPosition(400);
		Assert.assertEquals(900, p.getAmount());
		Assert.assertEquals(500, p.getRemainingWork());
		Assert.assertEquals(400, p.getPosition());
		p.progress(0);
		Assert.assertEquals(900, p.getAmount());
		Assert.assertEquals(500, p.getRemainingWork());
		Assert.assertEquals(400, p.getPosition());
		p.setPosition(-100);
		Assert.assertEquals(900, p.getAmount());
		Assert.assertEquals(900, p.getRemainingWork());
		Assert.assertEquals(0, p.getPosition());
		p.setPosition(10000);
		Assert.assertEquals(900, p.getAmount());
		Assert.assertEquals(0, p.getRemainingWork());
		Assert.assertEquals(900, p.getPosition());
		p.done();
		p.error(new Exception());
		p.cancel(new CancelException(new Exception()));
		p.getSynch();
		p.setAmount(1);
		Assert.assertEquals(1, p.getAmount());
		Assert.assertEquals(0, p.getRemainingWork());
		Assert.assertEquals(1, p.getPosition());
		
		p = new WorkProgressImpl(1000);
		p.unlisten(listener);
		Async<Exception> changed2 = new Async<>();
		listener = () -> {
			changed2.unblock();
		};
		p.listen(listener);
		p.interruptEvents();
		p.progress(100);
		Thread.sleep(1000);
		Assert.assertFalse(changed2.isDone());
		p.resumeEvents(false);
		Assert.assertFalse(changed2.isDone());
		p.progress(100);
		Thread.sleep(1000);
		Assert.assertTrue(changed2.isDone());
		Async<Exception> changed3 = new Async<>();
		listener = () -> {
			changed3.unblock();
		};
		p.listen(listener);
		p.interruptEvents();
		p.resumeEvents(true);
		Thread.sleep(1000);
		Assert.assertTrue(changed3.isDone());
		p.done();
		p.unlisten(listener);
	}
	
	@Test
	public void testLinks() throws Exception {
		WorkProgressImpl main = new WorkProgressImpl(1000);
		WorkProgressImpl sub1 = new WorkProgressImpl(1000);
		WorkProgressImpl sub2 = new WorkProgressImpl(1000);
		WorkProgressImpl sub3 = new WorkProgressImpl(1000);
		WorkProgress.link(sub1, main, 200);
		WorkProgress.link(sub2, main, 500);
		WorkProgress.link(sub3, main, 250);
		Task<?,?> task1 = Task.cpu("task1", Task.Priority.NORMAL, () -> null);
		Task<?,?> task2 = Task.cpu("task2", Task.Priority.NORMAL, () -> null);
		Task<?,?> task3 = Task.cpu("task3", Task.Priority.NORMAL, () -> null);
		WorkProgress.linkTo(sub1, task1);
		WorkProgress.linkTo(sub2, task2);
		WorkProgress.linkTo(sub3, task3);
		
		Assert.assertEquals(0, main.getPosition());

		Assert.assertEquals(0, sub2.getPosition());
		task2.start().getOutput().blockThrow(0);
		Assert.assertEquals(1000, sub2.getPosition());
		for (int i = 0; i < 100; ++i) {
			if (main.getPosition() == 500) break;
			try { Thread.sleep(10); } catch (InterruptedException e) {}
		}
		Assert.assertEquals(500, main.getPosition());
		
		Assert.assertEquals(0, sub3.getPosition());
		task3.start().getOutput().blockThrow(0);
		Assert.assertEquals(1000, sub3.getPosition());
		for (int i = 0; i < 100; ++i) {
			if (main.getPosition() == 750) break;
			try { Thread.sleep(10); } catch (InterruptedException e) {}
		}
		Assert.assertEquals(750, main.getPosition());
		
		Assert.assertEquals(0, sub1.getPosition());
		task1.start().getOutput().blockThrow(0);
		Assert.assertEquals(1000, sub1.getPosition());
		for (int i = 0; i < 100; ++i) {
			if (main.getPosition() == 950) break;
			try { Thread.sleep(10); } catch (InterruptedException e) {}
		}
		Assert.assertEquals(950, main.getPosition());
		
		main = new WorkProgressImpl(1000);
		sub1 = new WorkProgressImpl(1000);
		WorkProgress.link(sub1, main, 200);
		task1 = testTask(() -> { throw new Exception("Test error"); });
		WorkProgress.linkTo(sub1, task1);
		Assert.assertFalse(main.getSynch().hasError());
		Assert.assertFalse(sub1.getSynch().hasError());
		task1.start().getOutput().block(0);
		sub1.getSynch().block(5000);
		Assert.assertTrue(sub1.getSynch().hasError());
		main.getSynch().block(5000);
		Assert.assertTrue(main.getSynch().hasError());
		
		main = new WorkProgressImpl(1000);
		sub1 = new WorkProgressImpl(1000);
		WorkProgress.link(sub1, main, 200);
		task1 = testTask(() -> { throw new CancelException("Test cancel"); });
		WorkProgress.linkTo(sub1, task1);
		Assert.assertFalse(main.getSynch().hasError());
		Assert.assertFalse(sub1.getSynch().hasError());
		task1.start().getOutput().block(0);
		sub1.getSynch().block(5000);
		Assert.assertTrue(sub1.getSynch().isCancelled());
		main.getSynch().block(5000);
		Assert.assertTrue(main.getSynch().isCancelled());
	}
	
}
