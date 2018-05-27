package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.locale.LocalizedProperties;

import org.junit.Assert;
import org.junit.Test;

public class TestLocalizedProperties extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void testErrors() {
		LocalizedProperties lp = LCCore.getApplication().getLocalizedProperties();
		ClassLoader cl = LCCore.getApplication().getClassLoader();
		
		ISynchronizationPoint<Exception> sp = lp.registerNamespace("test-error", "does/not/exist", cl);
		try {
			sp.blockThrow(0);
			throw new AssertionError("Loading a namespace that does not exist must fail");
		} catch (Exception e) {
			// normal
		}
		
		lp.registerNamespace("test-error2", "locale/error", cl);
		String s = lp.localizeSync(new String[] { "fr" }, "test-error2", "hello");
		Assert.assertTrue(s.startsWith("!! no compatible language"));
		s = lp.localizeSync(new String[] { "en" }, "test-error2", "hello");
		Assert.assertTrue(s.startsWith("!! no compatible language"));
		s = lp.localizeSync(new String[] { "en-us" }, "test-error2", "hello");
		Assert.assertTrue(s.startsWith("!! no compatible language"));
		s = lp.localizeSync(new String[] { "in" }, "test-error2", "hello");
		Assert.assertTrue(s.startsWith("!! missing key"));
		s = lp.localizeSync(new String[] { "ph" }, "test-error2", "hello");
		Assert.assertTrue(s.startsWith("!! missing key"));
	}
	
	@Test(timeout=30000)
	public void test() {
		LocalizedProperties lp = LCCore.getApplication().getLocalizedProperties();
		// for coverage
		lp.getAvailableLanguageCodes();
	}
	
}
