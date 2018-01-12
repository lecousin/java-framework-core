package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.locale.ILocalizableString;

import org.junit.Assert;

public class TestILocalizableString {

	public static void test(ILocalizableString str, String expectedEnglish, String expectedFrench) throws Exception {
		Assert.assertEquals(expectedEnglish, str.localizeSync("en"));
		Assert.assertEquals(expectedFrench, str.localizeSync("fr"));
		Assert.assertEquals(expectedEnglish, str.localize("en").blockResult(0));
		Assert.assertEquals(expectedFrench, str.localize("fr").blockResult(0));
	}
	
}
