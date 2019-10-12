package net.lecousin.framework.core.tests.locale;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.locale.LocalizedProperties;

import org.junit.Assert;
import org.junit.Test;

public class TestLocalizedProperties extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void testErrors() {
		LocalizedProperties lp = LCCore.getApplication().getLocalizedProperties();
		ClassLoader cl = getClass().getClassLoader();
		
		IAsync<IOException> sp = lp.registerNamespace("test-error", "does/not/exist", cl);
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

		s = lp.localizeSync(new String[] { "end" }, "doesnotexist", "hello");
		Assert.assertTrue(s.startsWith("!! unknown namespace"));
		
		Set<String> langs = lp.getNamespaceLanguages("test-error2");
		Assert.assertEquals(3, langs.size());
		langs = lp.getNamespaceLanguages("test-error");
		Assert.assertEquals(0, langs.size());
		langs = lp.getNamespaceLanguages("test-error-test");
		Assert.assertEquals(0, langs.size());
		
		AsyncSupplier<Map<String, String>, IOException> content = lp.getNamespaceContent("test-error", new String[] { "en" });
		content.block(10000);
		Assert.assertTrue(content.hasError());
		content = lp.getNamespaceContent("test-error-test", new String[] { "en" });
		content.block(10000);
		Assert.assertTrue(content.hasError());
		content = lp.getNamespaceContent("test-error2", new String[] { "xx" });
		content.block(10000);
		Assert.assertTrue(content.hasError());
		content = lp.getNamespaceContent("test-error2", new String[] { "ph" });
		content.block(10000);
		Assert.assertFalse(content.hasError());
	}
	
	@Test(timeout=30000)
	public void test() {
		LocalizedProperties lp = LCCore.getApplication().getLocalizedProperties();
		// for coverage
		lp.getAvailableLanguageCodes();
		lp.getDeclaredNamespaces();
	}
	
	@Test(timeout=30000, expected = IOException.class)
	public void testRegisterExistingNamespace() throws Exception {
		IAsync<IOException> res = LCCore.getApplication().getLocalizedProperties().registerNamespace("b", "hello", getClass().getClassLoader());
		res.blockThrow(10000);
	}
	
	@Test(timeout=30000)
	public void testRegisterNotExistingFile() throws Exception {
		IAsync<IOException> res = LCCore.getApplication().getLocalizedProperties().registerNamespace("hello", "world", getClass().getClassLoader());
		try {
			res.blockThrow(10000);
			throw new AssertionError("Should throw an exception");
		} catch (IOException e) {
			// ok
		}
		Assert.assertEquals("!! error loading namespace hello !!", LCCore.getApplication().getLocalizedProperties().localizeSync(new String[] { "fr" }, "hello", "test", "val"));
	}
	
}
