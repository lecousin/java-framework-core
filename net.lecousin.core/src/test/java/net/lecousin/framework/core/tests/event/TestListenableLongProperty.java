package net.lecousin.framework.core.tests.event;

import java.util.function.Consumer;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.event.ListenableLongProperty;
import net.lecousin.framework.mutable.MutableBoolean;
import net.lecousin.framework.mutable.MutableLong;

import org.junit.Assert;
import org.junit.Test;

public class TestListenableLongProperty extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void test() {
		ListenableLongProperty p = new ListenableLongProperty(5);
		MutableLong value = new MutableLong(100);
		MutableBoolean called = new MutableBoolean(false);
		Consumer<Long> cons = val -> value.set(val.longValue());
		Runnable listener = () -> called.set(true);
		
		p.set(10);
		p.inc();
		p.sub(4);
		p.inc();
		p.add(12);
		Assert.assertEquals(20L, p.get());
		Assert.assertEquals(100L, value.get());
		Assert.assertFalse(called.get());
		
		p.addListener(cons);
		Assert.assertTrue(p.hasListeners());
		p.addListener(listener);
		Assert.assertTrue(p.hasListeners());
		p.set(50);
		Assert.assertTrue(called.get());
		Assert.assertEquals(20L, value.get());
		called.set(false);
		p.inc(); // 50 -> 51
		Assert.assertTrue(called.get());
		Assert.assertEquals(50L, value.get());
		called.set(false);
		p.add(6); // 51 -> 57
		Assert.assertTrue(called.get());
		Assert.assertEquals(51L, value.get());
		called.set(false);
		p.dec(); // 57 -> 56
		Assert.assertTrue(called.get());
		Assert.assertEquals(57L, value.get());
		called.set(false);
		p.sub(3); // 56 -> 53
		Assert.assertTrue(called.get());
		Assert.assertEquals(56L, value.get());
		called.set(false);
		p.set(53); // same
		Assert.assertFalse(called.get());
		Assert.assertEquals(56L, value.get());
		Assert.assertTrue(p.hasListeners());

		p.removeListener(cons);
		Assert.assertTrue(p.hasListeners());
		p.removeListener(listener);
		Assert.assertFalse(p.hasListeners());
		p.set(10);
		Assert.assertFalse(called.get());
		Assert.assertEquals(56L, value.get());
	}
	
}
