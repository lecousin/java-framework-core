package net.lecousin.framework.core.tests.application;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.libraries.artifacts.LoadedLibrary;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.util.AsyncCloseable;

import org.junit.Assert;
import org.junit.Test;

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
		app.closed(new AsyncCloseable<Exception>() {
			@Override
			public ISynchronizationPoint<Exception> closeAsync() {
				return new SynchronizationPoint<>(true);
			}
		});
		
		Integer i = Integer.valueOf(51);
		Assert.assertNull(app.getData("test"));
		Assert.assertNull(app.setData("test", i));
		Assert.assertEquals(i, app.getData("test"));
		Assert.assertEquals(i, app.removeData("test"));
		Assert.assertNull(app.getData("test"));
		
		Assert.assertNull(app.getInstance(Integer.class));
		Assert.assertNull(app.setInstance(Integer.class, i));
		Assert.assertEquals(i, app.getInstance(Integer.class));
		Assert.assertEquals(i, app.removeInstance(Integer.class));
		Assert.assertNull(app.getInstance(Integer.class));
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
	
	@Test
	public void testLibrariesLocations() {
		List<File> list = LCCore.getApplication().getLibrariesManager().getLibrariesLocations();
		Assert.assertNotEquals(0, list.size());
		boolean found = false;
		for (File f : list) {
			if (!f.isDirectory()) continue;
			File dir = new File(f, getClass().getPackage().getName().replace('.', '/'));
			if (!dir.exists()) continue;
			if (new File(dir, getClass().getSimpleName() + ".class").exists()) {
				found = true;
				break;
			}
		}
		Assert.assertTrue(found);
	}
	
	@Test
	public void testLibrary() {
		Application app = LCCore.getApplication();
		LoadedLibrary lib = new LoadedLibrary(new Artifact("mygroup", "myname", new Version("1")), null);
		Assert.assertEquals(app.getClassLoader(), lib.getClassLoader());
		Assert.assertEquals("mygroup", lib.getGroupId());
		Assert.assertEquals("myname", lib.getArtifactId());
		Assert.assertEquals("1", lib.getVersion().toString());
		Assert.assertEquals("mygroup:myname:1", lib.toString());
	}
	
	@Test
	public void testLCCore() {
		Closeable c = new Closeable() {
			@Override
			public void close() throws IOException {
			}
		};
		LCCore.get().closed(c);
		AsyncCloseable<Exception> ac = new AsyncCloseable<Exception>() {
			@Override
			public ISynchronizationPoint<Exception> closeAsync() {
				return new SynchronizationPoint<>(true);
			}
		};
		LCCore.get().closed(ac);
		LCCore.get().isStopping();
		LCCore.get().getSystemLibraries();
	}
	
}
