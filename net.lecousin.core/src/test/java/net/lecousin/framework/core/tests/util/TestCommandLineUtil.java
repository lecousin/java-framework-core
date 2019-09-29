package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.CommandLine;

import org.junit.Assert;
import org.junit.Test;

public class TestCommandLineUtil extends LCCoreAbstractTest {

	@Test(timeout=15000)
	public void testGetOptionValue() {
		String[] args = new String[] { "hello", "-test", "world", "-aa=bb", "-hello=world", "cc=dd" };
		Assert.assertNull(CommandLine.getOptionValue(args, "toto"));
		Assert.assertNull(CommandLine.getOptionValue(args, "test"));
		Assert.assertNull(CommandLine.getOptionValue(args, "world"));
		Assert.assertNull(CommandLine.getOptionValue(args, "cc"));
		Assert.assertEquals("bb", CommandLine.getOptionValue(args, "aa"));
		Assert.assertEquals("world", CommandLine.getOptionValue(args, "hello"));
	}
	
}
