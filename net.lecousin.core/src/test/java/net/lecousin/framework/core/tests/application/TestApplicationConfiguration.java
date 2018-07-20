package net.lecousin.framework.core.tests.application;

import java.io.InputStream;

import net.lecousin.framework.application.ApplicationConfiguration;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestApplicationConfiguration extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() throws Exception {
		InputStream input = getClass().getClassLoader().getResourceAsStream("app/lc-project.xml");
		ApplicationConfiguration cfg = ApplicationConfiguration.load(input);
		input.close();
		Assert.assertEquals("This is a test", cfg.name);
		Assert.assertEquals("mypackage.App", cfg.clazz);
		Assert.assertEquals("mylogo.jpg", cfg.splash);
		Assert.assertEquals("World", cfg.properties.get("hello"));
		Assert.assertEquals("Le Monde", cfg.properties.get("bonjour"));
	}
	
}