package net.lecousin.framework.core.tests.io.util;

import java.io.File;
import java.io.FileOutputStream;

import net.lecousin.framework.adapter.AdapterRegistry;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.tasks.drives.GetFileInfoTask;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.util.FileInfo;

import org.junit.Assert;
import org.junit.Test;

public class TestFileInfo extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		File f = File.createTempFile("test", "fileinfo");
		f.deleteOnExit();
		FileOutputStream out = new FileOutputStream(f);
		out.write(new byte[] { 1, 2, 3 });
		out.close();
		GetFileInfoTask task = new GetFileInfoTask(f, Task.PRIORITY_NORMAL);
		FileInfo info = task.start().getOutput().blockResult(0);
		Assert.assertFalse(info.isDirectory);
		Assert.assertEquals(3, info.size);
		Assert.assertEquals(f, info.file);
		Assert.assertEquals(f, AdapterRegistry.get().adapt(info, File.class));
		f.delete();
	}
	
}
