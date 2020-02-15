package net.lecousin.framework.core.tests.application;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.libraries.classpath.DefaultApplicationClassLoader;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Assert;
import org.junit.Test;

public class TestClassPath extends LCCoreAbstractTest {

	@Test
	public void testDefaultApplicationClassLoader() throws IOException {
		DefaultApplicationClassLoader cl = new DefaultApplicationClassLoader(LCCore.getApplication(), new File[] { new File("./pom.xml") });
		cl.getURLs();
		cl.close();
	}
	
	@Test
	public void testScanLibraries() {
		LCCore.getApplication().getLibrariesManager().scanLibraries("net.lecousin", true, pkg -> true, cl -> true, cl -> {});
		LCCore.getApplication().getLibrariesManager().scanLibraries("org.junit", true, pkg -> true, cl -> true, cl -> {});
		LCCore.getApplication().getLibrariesManager().scanLibraries("org", false, null, cl -> false, cl -> {});
		LCCore.getApplication().getLibrariesManager().scanLibraries("org.junit", false, null, cl -> false, cl -> {});
		LCCore.getApplication().getLibrariesManager().scanLibraries("org.junit", true, null, cl -> false, cl -> {});
		LCCore.getApplication().getLibrariesManager().scanLibraries("org.junit", true, pkg -> false, cl -> false, cl -> {});
	}
	
	@Test
	public void testGetUnknownResource() {
		Assert.assertNull(LCCore.getApplication().getLibrariesManager().getResource("does/not/exist", Task.PRIORITY_NORMAL));
	}
	
}
