package net.lecousin.framework.core.tests.util;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.PathPattern;

public class TestPathPattern extends LCCoreAbstractTest {

	@Test
	public void testPatterns() {
		Assert.assertFalse(new PathPattern("").matches("path"));
		Assert.assertTrue(new PathPattern("path").matches("path"));
		Assert.assertFalse(new PathPattern("toto/t?t?").matches("path"));
		Assert.assertFalse(new PathPattern("toto/t?t?").matches("toto"));
		Assert.assertFalse(new PathPattern("toto/t?t?").matches("toto/hello"));
		Assert.assertTrue(new PathPattern("toto/t?t?").matches("toto/titi"));
		Assert.assertTrue(new PathPattern("toto/t?t?").matches("toto/tttt"));
		Assert.assertFalse(new PathPattern("toto/t?t?").matches("toto/tttt2"));
		Assert.assertTrue(new PathPattern("toto/*/t?t?").matches("toto/tttt/tttt"));
		Assert.assertTrue(new PathPattern("toto/*/t?t?").matches("toto/hello/tttt"));
		Assert.assertFalse(new PathPattern("toto/*/t?t?").matches("toto/hello/hello"));
		Assert.assertFalse(new PathPattern("toto/*/t?t?").matches("toto/tttt/hello"));
		Assert.assertFalse(new PathPattern("toto/*/t?t?").matches("toto/hello/hello/tata"));
		Assert.assertTrue(new PathPattern("toto/**/t?t?").matches("toto/hello/hello/tata"));
		Assert.assertTrue(new PathPattern("toto/**/t?t?").matches("toto/tata"));
		Assert.assertTrue(new PathPattern("toto/**/t?t?").matches("toto/tata/tutu"));
		Assert.assertFalse(new PathPattern("toto/**/t?t?").matches("toto/tata/hello"));
	}
	
}
