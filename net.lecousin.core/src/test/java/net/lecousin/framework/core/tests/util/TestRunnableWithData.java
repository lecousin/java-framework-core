package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.RunnableWithData;

import org.junit.Assert;
import org.junit.Test;

public class TestRunnableWithData extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		RunnableWithData<String> r = new RunnableWithData<String>("test") {
			@Override
			public void run() {
			}
		};
		Assert.assertEquals("test", r.getData());
	}
	
}
