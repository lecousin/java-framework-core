package net.lecousin.framework.core.tests.concurrent.threads;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.mutable.MutableInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestTask extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		Task<Object,?> task = Task.cpu("test", Task.Priority.NORMAL, t -> null).start();
		Assert.assertEquals(Threading.getCPUTaskManager(), task.getTaskManager());
		Assert.assertEquals(LCCore.getApplication(), task.getApplication());

		MutableBoolean ok = new MutableBoolean(false);
		Task.cpu("test", Task.Priority.NORMAL, new Executable.FromRunnable(() -> {
			ok.set(true);
		})).start().getOutput().block(0);
		Assert.assertTrue(ok.get());
		
		MutableInteger i = new MutableInteger(0);
		Task.cpu("test", Task.Priority.NORMAL, new Executable.FromRunnable(() -> {
			i.inc();
		}), r -> {
			i.inc();
		}).start().getOutput().block(0);
		Assert.assertEquals(2, i.get());
		
		i.set(0);
		task = Task.cpu("test", Task.Priority.NORMAL, t -> {
			i.inc();
			return null;
		}, r -> {
			i.inc();
		});
		Assert.assertFalse(task.isDone());
		task.getOutput().cancel(new CancelException("a cancel test"));
		task.start();
		Assert.assertTrue(task.isCancelled());
		Assert.assertTrue(task.isDone());
		Assert.assertEquals(0, i.get());
		
		Task<Integer, ?> unmanaged = Task.unmanaged("test", Task.Priority.NORMAL, t -> Integer.valueOf(111));
		unmanaged.startOn(true);
		Assert.assertEquals(111, unmanaged.getOutput().blockResult(0).intValue());
	}
	
	@Test
	public void testPriority() {
		Priority p = Priority.URGENT;
		do {
			Priority less = p.less();
			if (less.getValue() == p.getValue())
				break;
			p = less;
		} while (true);
		p = Priority.BACKGROUND;
		do {
			Priority more = p.more();
			if (more.getValue() == p.getValue())
				break;
			p = more;
		} while (true);
	}
	
}
