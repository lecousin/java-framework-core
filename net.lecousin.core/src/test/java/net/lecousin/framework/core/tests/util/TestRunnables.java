package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.Runnables;

import org.junit.Assert;
import org.junit.Test;

public class TestRunnables extends LCCoreAbstractTest {

	@Test
	public void test() {
		Runnables.WithData<String> r = new Runnables.WithData<String>("test") {
			@Override
			public void run() {
			}
		};
		Assert.assertEquals("test", r.getData());
	}
	
}
