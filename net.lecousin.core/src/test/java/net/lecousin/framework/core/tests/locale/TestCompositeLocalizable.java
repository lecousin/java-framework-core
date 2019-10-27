package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.locale.CompositeLocalizable;
import net.lecousin.framework.locale.LocalizableString;

import org.junit.Test;

public class TestCompositeLocalizable extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		TestILocalizableString.test(new CompositeLocalizable(" * ", new LocalizableString("b", "name"), new LocalizableString("b", "file")), "name * file", "nom * fichier");
	}
	
}
