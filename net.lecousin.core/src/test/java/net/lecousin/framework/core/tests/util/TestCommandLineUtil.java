package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.CommandLineUtil;

import org.junit.Assert;
import org.junit.Test;

public class TestCommandLineUtil extends LCCoreAbstractTest {

	@Test(timeout=15000)
	public void testGetOptionValue() {
		String[] args = new String[] { "hello", "-test", "world", "-aa=bb", "-hello=world", "cc=dd" };
		Assert.assertNull(CommandLineUtil.getOptionValue(args, "toto"));
		Assert.assertNull(CommandLineUtil.getOptionValue(args, "test"));
		Assert.assertNull(CommandLineUtil.getOptionValue(args, "world"));
		Assert.assertNull(CommandLineUtil.getOptionValue(args, "cc"));
		Assert.assertEquals("bb", CommandLineUtil.getOptionValue(args, "aa"));
		Assert.assertEquals("world", CommandLineUtil.getOptionValue(args, "hello"));
	}
	
}
