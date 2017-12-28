package net.lecousin.framework.core.tests.application;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO;

public class TestApplication extends LCCoreAbstractTest {

	@Test
	public void testBasic() {
		Application app = LCCore.getApplication();
		app.getStartTime();
		app.getCommandLineArguments();
		app.getLocale();
		app.setLocale(Locale.FRENCH);
		app.getLanguageTag();
		app.setLocale(Locale.US);
		app.getLanguageTag();
		app.getLocalizedProperties();
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testResource() throws Exception {
		Application app = LCCore.getApplication();
		
		IO.Readable io = app.getLibrariesManager().getResource("xml-test-suite/mine/001.xml", Task.PRIORITY_NORMAL);
		Assert.assertFalse(null == io);
		io.close();
		
		io = app.getLibrariesManager().getResource("xml-test-suite/mine/doesntexist", Task.PRIORITY_NORMAL);
		Assert.assertTrue(null == io);
		
		io = app.getResource("xml-test-suite/mine/001.xml", Task.PRIORITY_NORMAL);
		Assert.assertFalse(null == io);
		io.close();
		
		io = app.getResource("xml-test-suite/mine/doesntexist", Task.PRIORITY_NORMAL);
		Assert.assertTrue(null == io);
	}
	
}
