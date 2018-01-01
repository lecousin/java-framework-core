package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.SystemEnvironment;

import org.junit.Test;

public class TestSystemEnvironment extends LCCoreAbstractTest {

	@Test
	public void test() {
		// cannot really test the result as it depends on the platform this test is run...
		SystemEnvironment.getOSFamily();
	}
	
}
