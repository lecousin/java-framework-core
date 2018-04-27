package net.lecousin.framework.core.tests.concurrent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.tasks.LoadPropertiesFileTask;
import net.lecousin.framework.concurrent.tasks.SavePropertiesFileTask;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

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
		SavePropertiesFileTask.savePropertiesFile(props, file, StandardCharsets.UTF_8, Task.PRIORITY_NORMAL).blockThrow(0);
		Properties loaded = LoadPropertiesFileTask.loadPropertiesFile(file, StandardCharsets.UTF_8, Task.PRIORITY_NORMAL, true, null).blockResult(0);
		check(props, loaded);
	}
	
	protected void check(Properties expected, Properties found) {
		Assert.assertEquals(expected.size(), found.size());
		for (Object key : expected.keySet())
			Assert.assertEquals("Property " + key, expected.getProperty((String)key), found.getProperty((String)key));
	}
	
}
