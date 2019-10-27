package net.lecousin.framework.core.tests.locale;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.locale.ILocalizableString;
import net.lecousin.framework.locale.annotations.LocalizableAnnotations;
import net.lecousin.framework.locale.annotations.LocalizableProperty;
import net.lecousin.framework.properties.Property;

import org.junit.Assert;
import org.junit.Test;

public class TestLocalizableAnnotations extends LCCoreAbstractTest {

	@LocalizableProperty(name="hello", namespace="test", key="test")
	public String myTest;
	
	@Property(name="hello2", value="bonjour2")
	public String myTest2;
	
	@Test
	public void testGet() throws Exception {
		ILocalizableString ls;
		
		ls = LocalizableAnnotations.get(this.getClass().getField("myTest"), "hello");
		Assert.assertEquals("this is a test", ls.localizeSync("en"));

		ls = LocalizableAnnotations.get(this.getClass().getField("myTest2"), "hello2");
		Assert.assertEquals("bonjour2", ls.localizeSync("en"));

		ls = LocalizableAnnotations.get(this.getClass().getField("myTest2"), "hello3");
		Assert.assertNull(ls);
	}
	
}
