package net.lecousin.framework.core.tests.io.provider;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.provider.IOProviderFromPathUsingClassloader;

import org.junit.Test;

public class TestIOProviderFromPathUsingClassloader extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void test() throws Exception {
		IOProviderFromPathUsingClassloader provider = new IOProviderFromPathUsingClassloader(LCCore.getApplication().getClassLoader());
		provider.get("META-INF/net.lecousin/plugins").provideIOReadable(Task.PRIORITY_NORMAL).close();
	}
	
}
