package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.locale.LocalizableString;
import net.lecousin.framework.locale.LocalizableStringBuffer;

import org.junit.Test;

public class TestLocalizableStringBuffer extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() throws Exception {
		TestILocalizableString.test(new LocalizableStringBuffer("start/", new LocalizableString("b", "name"), "/end"), "start/name/end", "start/nom/end");
	}
	
}
