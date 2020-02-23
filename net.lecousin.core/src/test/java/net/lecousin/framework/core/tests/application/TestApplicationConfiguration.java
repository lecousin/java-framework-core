package net.lecousin.framework.core.tests.application;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.lecousin.framework.application.ApplicationConfiguration;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.IOUtil;

import org.junit.Assert;
import org.junit.Test;

public class TestApplicationConfiguration extends LCCoreAbstractTest {

	@Test
	public void testFromStream() throws Exception {
		InputStream input = getClass().getClassLoader().getResourceAsStream("app/lc-project.xml");
		ApplicationConfiguration cfg = ApplicationConfiguration.load(input);
		input.close();
		Assert.assertEquals("This is a test", cfg.getName());
		Assert.assertEquals("mypackage.App", cfg.getClazz());
		Assert.assertEquals("mylogo.jpg", cfg.getSplash());
		Assert.assertEquals("World", cfg.getProperties().get("hello"));
		Assert.assertEquals("Le Monde", cfg.getProperties().get("bonjour"));
	}

	@Test
	public void testFromFile() throws Exception {
		File tmp = File.createTempFile("test", "lc-project");
		tmp.deleteOnExit();
		InputStream input = getClass().getClassLoader().getResourceAsStream("app/lc-project.xml");
		IOUtil.copy(
			new IOFromInputStream(input, "lc-project.xml", Threading.getUnmanagedTaskManager(), Task.Priority.NORMAL),
			new FileIO.WriteOnly(tmp, Task.Priority.NORMAL),
			-1, true, null, 0).blockThrow(15000);
		try {
			ApplicationConfiguration cfg = ApplicationConfiguration.load(tmp);
			Assert.assertEquals("This is a test", cfg.getName());
			Assert.assertEquals("mypackage.App", cfg.getClazz());
			Assert.assertEquals("mylogo.jpg", cfg.getSplash());
			Assert.assertEquals("World", cfg.getProperties().get("hello"));
			Assert.assertEquals("Le Monde", cfg.getProperties().get("bonjour"));
		} finally {
			tmp.delete();
		}
		
		try {
			ApplicationConfiguration.load(new File("/doesnotexist"));
			throw new AssertionError("should throw an error if the file does not exist");
		} catch (Exception e) {
			// ok
		}
	}
	
	@Test
	public void testErrors() throws Exception {
		testError("invalid");
		testError("empty");
		testError("missing-project");
		testError("missing-application");
		testError("unknown-element");
	}
	
	private void testError(String name) throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("app/lc-project.error." + name + ".xml")) {
			try {
				ApplicationConfiguration.load(in);
				throw new AssertionError("should throw an error for file lc-project.error." + name + ".xml");
			} catch (Exception e) {
				// ok
			}
		}
	}
	
}
