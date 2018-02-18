package net.lecousin.framework.core.tests.io.provider;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.provider.IOProviderFromNameUsingClassloader;

import org.junit.Test;

public class TestIOProviderFromNameUsingClassloader extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void test() throws Exception {
		IOProviderFromNameUsingClassloader provider = new IOProviderFromNameUsingClassloader(LCCore.getApplication().getClassLoader());
		provider.provideReadableIO("META-INF/net.lecousin/plugins", Task.PRIORITY_NORMAL).close();
	}
	
}
