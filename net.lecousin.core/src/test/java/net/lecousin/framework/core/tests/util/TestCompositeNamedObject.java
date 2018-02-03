package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.locale.FixedLocalizedString;
import net.lecousin.framework.util.CompositeNamedObject;

import org.junit.Assert;
import org.junit.Test;

public class TestCompositeNamedObject extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() {
		CompositeNamedObject c = new CompositeNamedObject();
		c.add(new FixedLocalizedString("Test"), Integer.valueOf(51));
		Assert.assertEquals(Integer.valueOf(51), c.get(0));
		Assert.assertEquals("Test", c.getName(0).appLocalizationSync());
	}
	
}
