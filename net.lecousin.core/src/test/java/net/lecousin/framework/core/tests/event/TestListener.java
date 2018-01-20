package net.lecousin.framework.core.tests.event;

import net.lecousin.framework.event.Listener;

import org.junit.Assert;
import org.junit.Test;

public class TestListener {

	@Test(timeout=30000)
	public void testWithData() {
		Listener.WithData<Integer, Long> listener = new Listener.WithData<Integer, Long>(Long.valueOf(10)) {
			@Override
			public void fire(Integer event) {
			}
		};
		Assert.assertEquals(10, listener.getData().longValue());
	}
	
}
