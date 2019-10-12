package net.lecousin.framework.core.tests.application;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.libraries.classpath.DefaultApplicationClassLoader;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Test;

public class TestClassPath extends LCCoreAbstractTest {

	@Test(timeout=15000)
	public void testDefaultApplicationClassLoader() throws IOException {
		DefaultApplicationClassLoader cl = new DefaultApplicationClassLoader(LCCore.getApplication(), new File[] { new File("./pom.xml") });
		cl.getURLs();
		cl.close();
	}
	
	@Test(timeout=60000)
	public void testScanLibraries() {
		LCCore.getApplication().getLibrariesManager().scanLibraries("net.lecousin", true, pkg -> true, cl -> true, cl -> {});
	}
	
}
