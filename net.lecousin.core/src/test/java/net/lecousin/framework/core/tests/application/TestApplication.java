package net.lecousin.framework.core.tests.application;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO;

public class TestApplication extends LCCoreAbstractTest {

	@SuppressWarnings("resource")
	@Test
	public void testResource() throws Exception {
		IO.Readable io = LCCore.getApplication().getLibrariesManager().getResource("xml-test-suite/mine/001.xml", Task.PRIORITY_NORMAL);
		Assert.assertFalse(null == io);
		io.close();
		io = LCCore.getApplication().getLibrariesManager().getResource("xml-test-suite/mine/doesntexist", Task.PRIORITY_NORMAL);
		Assert.assertTrue(null == io);
	}
	
}
