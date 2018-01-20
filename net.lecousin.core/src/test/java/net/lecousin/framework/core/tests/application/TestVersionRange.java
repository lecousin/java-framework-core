package net.lecousin.framework.core.tests.application;

import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.VersionRange;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestVersionRange extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		Assert.assertTrue(new VersionRange(new Version("1.2.3")).includes(new Version("1.2.3")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3")).includes(new Version("1.2.2")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3")).includes(new Version("1.2.4")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3")).includes(new Version("1.3.3")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3")).includes(new Version("2.2.3")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3")).includes(new Version("0.2.3")));
		Assert.assertEquals("1.2.3", new VersionRange(new Version("1.2.3")).toString());

		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.2.3")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.2.4")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.2.7")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3.0")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3.1")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3.5")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3.6")));
		Assert.assertTrue(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.3.7")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.2.2")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.1.5")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("1.4.2")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("2.0")));
		Assert.assertFalse(new VersionRange(new Version("1.2.3"), new Version("1.3.6"), true).includes(new Version("2.3.2")));
	}
	
}
