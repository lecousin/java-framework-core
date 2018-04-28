package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestLibraryLocaleFile extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		Assert.assertEquals("This is a test", LCCore.getApplication().getLocalizedProperties().localizeSync(new String[] { "en" }, "test", "Test"));
		Assert.assertEquals("Ceci est un test", LCCore.getApplication().getLocalizedProperties().localizeSync(new String[] { "fr" }, "test", "Test"));
	}
	
}
