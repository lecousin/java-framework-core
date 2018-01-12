package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.locale.FixedLocalizedString;

import org.junit.Test;

public class TestFixedLocalizedString extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() throws Exception {
		TestILocalizableString.test(new FixedLocalizedString("it's fixed"), "it's fixed", "it's fixed");
	}
	
}
