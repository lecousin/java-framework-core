package net.lecousin.framework.core.tests.io.provider;

import java.io.File;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.libraries.classloader.AppClassLoader;
import net.lecousin.framework.application.libraries.classloader.DirectoryClassLoader;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.io.provider.IOProviderFromPathUsingClassloader;

import org.junit.Assert;
import org.junit.Test;

public class TestIOProviderFromPathUsingClassloader extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		IOProviderFromPathUsingClassloader provider = new IOProviderFromPathUsingClassloader(getClass().getClassLoader());
		IOProvider.Readable iop = provider.get("META-INF/net.lecousin/plugins");
		iop.getDescription();
		iop.provideIOReadable(Task.Priority.NORMAL).close();
		
		iop = provider.get("/META-INF/net.lecousin/plugins");
		iop.provideIOReadable(Task.Priority.NORMAL).close();

		iop = provider.get("/does/not/exist");
		Assert.assertNull(iop);
		
		DirectoryClassLoader cl = new DirectoryClassLoader(new AppClassLoader(LCCore.getApplication()), new File("."));
		provider = new IOProviderFromPathUsingClassloader(cl);
		iop = provider.get("does.not.exist");
		Assert.assertNull(iop);
		iop = provider.get("pom.xml");
		Assert.assertNotNull(iop);
	}
	
}
