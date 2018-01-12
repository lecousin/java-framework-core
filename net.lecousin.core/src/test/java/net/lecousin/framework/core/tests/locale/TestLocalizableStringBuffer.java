package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.locale.LocalizableString;
import net.lecousin.framework.locale.LocalizableStringBuffer;

import org.junit.Test;

public class TestLocalizableStringBuffer extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() throws Exception {
		TestILocalizableString.test(new LocalizableStringBuffer("start/", new LocalizableString("b", "name"), "/end"), "start/name/end", "start/nom/end");
		LocalizableStringBuffer b = new LocalizableStringBuffer("#");
		b.add(new LocalizableString("b", "name"));
		b.add(Integer.valueOf(51));
		TestILocalizableString.test(b, "#name51", "#nom51");
	}
	
}
