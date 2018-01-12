package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.locale.LocalizableString;

import org.junit.Test;

public class TestLocalizableString extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() throws Exception {
		TestILocalizableString.test(new LocalizableString("b", "invalid _", new LocalizableString("b", "file"), "test"), "invalid file: test", "fichier invalide: test");
	}
	
}
