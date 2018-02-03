package net.lecousin.framework.core.tests.progress;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.progress.WorkProgressImpl;

import org.junit.Assert;
import org.junit.Test;

public class TestWorkProgressImpl extends LCCoreAbstractTest {

	@Test(timeout=30000)
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
		SynchronizationPoint<Exception> changed = new SynchronizationPoint<>();
		Runnable listener = () -> {
			changed.unblock();
		};
		p.listen(listener);
		Assert.assertFalse(changed.isUnblocked());
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
		Assert.assertFalse(changed.isUnblocked());
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
		p.done();
		p.error(new Exception());
		p.cancel(new CancelException(new Exception()));
		p.getSynch();
		p.setAmount(1);
		Assert.assertEquals(1, p.getAmount());
		Assert.assertEquals(0, p.getRemainingWork());
		Assert.assertEquals(1, p.getPosition());
	}
	
}
