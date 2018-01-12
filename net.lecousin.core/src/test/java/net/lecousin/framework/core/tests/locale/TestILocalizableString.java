package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.locale.ILocalizableString;

import org.junit.Assert;

public class TestILocalizableString {

	public static void test(ILocalizableString str, String expectedEnglish, String expectedFrench) throws Exception {
		Assert.assertEquals(expectedEnglish, str.localizeSync("en"));
		Assert.assertEquals(expectedEnglish, str.localizeSync(new String[] { "en" }));
		Assert.assertEquals(expectedEnglish, str.localizeSync("en-US"));
		Assert.assertEquals(expectedEnglish, str.localizeSync(new String[] { "en", "US" }));
		Assert.assertEquals(expectedFrench, str.localizeSync("fr"));
		Assert.assertEquals(expectedFrench, str.localizeSync(new String[] { "fr" }));
		Assert.assertEquals(expectedFrench, str.localizeSync("fr-FR"));
		Assert.assertEquals(expectedFrench, str.localizeSync(new String[] { "fr", "FR" }));
		Assert.assertEquals(expectedEnglish, str.localize("en").blockResult(0));
		Assert.assertEquals(expectedEnglish, str.localize(new String[] { "en" }).blockResult(0));
		Assert.assertEquals(expectedEnglish, str.localize("en-US").blockResult(0));
		Assert.assertEquals(expectedEnglish, str.localize(new String[] { "en", "US" }).blockResult(0));
		Assert.assertEquals(expectedFrench, str.localize("fr").blockResult(0));
		Assert.assertEquals(expectedFrench, str.localize(new String[] { "fr" }).blockResult(0));
		Assert.assertEquals(expectedFrench, str.localize("fr-FR").blockResult(0));
		Assert.assertEquals(expectedFrench, str.localize(new String[] { "fr", "FR" }).blockResult(0));
	}
	
}
