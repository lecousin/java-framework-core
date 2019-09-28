package net.lecousin.framework.core.tests.event;

import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.event.ListenableProperty;
import net.lecousin.framework.mutable.MutableInteger;

public class TestListenableProperty {

	@Test(timeout=30000)
	public void test() {
		ListenableProperty<Integer> prop = new ListenableProperty<Integer>(Integer.valueOf(1));
		Assert.assertEquals(1, prop.get().intValue());
		prop.set(Integer.valueOf(3));
		Assert.assertEquals(3, prop.get().intValue());
		Assert.assertFalse(prop.hasListeners());
		MutableInteger val1 = new MutableInteger(0);
		MutableInteger val2 = new MutableInteger(0);
		Consumer<Integer> listener = event -> val1.set(event.intValue());
		prop.addListener(listener);
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				val2.set(prop.get().intValue());
			}
		};
		prop.addListener(runnable);
		Assert.assertTrue(prop.hasListeners());
		prop.set(Integer.valueOf(5));
		Assert.assertEquals(5, prop.get().intValue());
		Assert.assertEquals(3, val1.get());
		Assert.assertEquals(5, val2.get());
		prop.removeListener(listener);
		prop.removeListener(runnable);
		Assert.assertFalse(prop.hasListeners());
		
		prop.set(Integer.valueOf(7));
		Assert.assertEquals(7, prop.get().intValue());
		Assert.assertEquals(3, val1.get());
		Assert.assertEquals(5, val2.get());
		
		prop.listen(listener);
		prop.listen(runnable);
		Assert.assertTrue(prop.hasListeners());
		prop.set(Integer.valueOf(7));
		Assert.assertEquals(7, prop.get().intValue());
		Assert.assertEquals(3, val1.get());
		Assert.assertEquals(5, val2.get());
		prop.set(Integer.valueOf(9));
		Assert.assertEquals(9, prop.get().intValue());
		Assert.assertEquals(7, val1.get());
		Assert.assertEquals(9, val2.get());

		prop.unlisten(listener);
		prop.unlisten(runnable);
		Assert.assertFalse(prop.hasListeners());
		prop.set(Integer.valueOf(11));
		Assert.assertEquals(11, prop.get().intValue());
		Assert.assertEquals(7, val1.get());
		Assert.assertEquals(9, val2.get());
	}
	
}
