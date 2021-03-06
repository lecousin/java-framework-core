package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.locale.FixedLocalizedString;

import org.junit.Assert;
import org.junit.Test;

public class TestFixedLocalizedString extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		TestILocalizableString.test(new FixedLocalizedString("it's fixed"), "it's fixed", "it's fixed");
		Assert.assertTrue(new FixedLocalizedString("test").equals(new FixedLocalizedString("test")));
		Assert.assertEquals(new FixedLocalizedString("test").hashCode(), new FixedLocalizedString("test").hashCode());
		Assert.assertFalse(new FixedLocalizedString("test").equals(new Object()));
	}
	
}
