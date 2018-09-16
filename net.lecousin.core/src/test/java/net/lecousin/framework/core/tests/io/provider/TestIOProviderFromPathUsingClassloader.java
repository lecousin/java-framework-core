package net.lecousin.framework.core.tests.io.provider;

import org.junit.Test;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.provider.IOProviderFromPathUsingClassloader;

public class TestIOProviderFromPathUsingClassloader extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void test() throws Exception {
		IOProviderFromPathUsingClassloader provider = new IOProviderFromPathUsingClassloader(getClass().getClassLoader());
		provider.get("META-INF/net.lecousin/plugins").provideIOReadable(Task.PRIORITY_NORMAL).close();
	}
	
}
