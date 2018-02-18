package net.lecousin.framework.log.bridges.slf4j;

import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Test;
import org.slf4j.LoggerFactory;

public class TestSLF4J extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		LoggerFactory.getLogger("test").info("This is a test");
	}
	
}
