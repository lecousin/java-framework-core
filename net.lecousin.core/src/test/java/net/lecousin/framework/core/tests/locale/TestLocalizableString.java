package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.locale.LocalizableString;

import org.junit.Assert;
import org.junit.Test;

public class TestLocalizableString extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() throws Exception {
		TestILocalizableString.test(new LocalizableString("b", "invalid _", new LocalizableString("b", "file"), "test"), "invalid file: test", "fichier invalide: test");
		Assert.assertTrue(new LocalizableString("b", "file").equals(new LocalizableString("b", "file")));
		Assert.assertEquals(new LocalizableString("b", "file").hashCode(), new LocalizableString("b", "file").hashCode());
		Assert.assertFalse(new LocalizableString("b", "file").equals(new Object()));
		Assert.assertFalse(new LocalizableString("b", "file").equals(new LocalizableString("b2", "file")));
		Assert.assertFalse(new LocalizableString("b", "file").equals(new LocalizableString("b", "file2")));
	}
	
}
