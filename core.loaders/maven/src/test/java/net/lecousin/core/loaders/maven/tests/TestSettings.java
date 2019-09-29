package net.lecousin.core.loaders.maven.tests;

import java.io.InputStream;

import net.lecousin.core.loaders.maven.MavenSettings;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestSettings extends LCCoreAbstractTest {

	@Test
	public void testSettings() throws Exception {
		InputStream in = getClass().getClassLoader().getResourceAsStream("test-maven/settings.xml");
		MavenSettings settings = MavenSettings.load(in);
		in.close();
		Assert.assertEquals("/test/maven/local/repo", settings.getLocalRepository());
	}
	
}
