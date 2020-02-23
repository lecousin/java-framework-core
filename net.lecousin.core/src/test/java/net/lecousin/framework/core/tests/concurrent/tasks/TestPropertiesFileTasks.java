package net.lecousin.framework.core.tests.concurrent.tasks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.concurrent.tasks.PropertiesFileLoader;
import net.lecousin.framework.concurrent.tasks.PropertiesFileSaver;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;

public class TestPropertiesFileTasks extends LCCoreAbstractTest {

	@Test
	public void testSaveAndLoad() throws Exception {
		Properties props = new Properties();
		testSaveAndLoad(props);
		props = new Properties();
		props.setProperty("p1", "v1");
		props.setProperty("p2", "v2");
		props.setProperty("p3", "v3");
		props.setProperty("p4", "v4");
		props.setProperty("p2", "V2");
		testSaveAndLoad(props);
	}
	
	protected void testSaveAndLoad(Properties props) throws Exception {
		File file = File.createTempFile("test", "properties");
		PropertiesFileSaver.savePropertiesFile(props, file, StandardCharsets.UTF_8, Task.Priority.NORMAL).blockThrow(0);
		Properties loaded = PropertiesFileLoader.loadPropertiesFile(file, StandardCharsets.UTF_8, Task.Priority.NORMAL, true, null).blockResult(0);
		check(props, loaded);
		loaded = PropertiesFileLoader.loadPropertiesFile((IO.Readable)new SimpleBufferedReadable(new FileIO.ReadOnly(file, Task.Priority.NORMAL), 1024), StandardCharsets.UTF_8, Task.Priority.NORMAL, IO.OperationType.SYNCHRONOUS, p -> {}).blockResult(0);
		check(props, loaded);
	}
	
	protected void check(Properties expected, Properties found) {
		Assert.assertEquals(expected.size(), found.size());
		for (Object key : expected.keySet())
			Assert.assertEquals("Property " + key, expected.getProperty((String)key), found.getProperty((String)key));
	}
	
}
