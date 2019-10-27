package net.lecousin.framework.core.tests.concurrent;

import org.junit.Test;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Console;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public class TestConsole extends LCCoreAbstractTest {

	@Test
	public void test() {
		Console c = LCCore.getApplication().getConsole();
		c.out(new Exception("test"));
		c.err(new Exception("test"));
	}
	
}
