package net.lecousin.framework.core.tests.io.util;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import net.lecousin.framework.adapter.AdapterRegistry;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.tasks.drives.GetFileInfoTask;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.util.FileInfo;

import org.junit.Assert;
import org.junit.Test;

public class TestFileInfo extends LCCoreAbstractTest {

	@Test(timeout=120000)
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
		Assert.assertEquals(f, AdapterRegistry.get().adapt(info, Path.class).toFile());
		IO.Readable in = AdapterRegistry.get().adapt(info, IO.Readable.class);
		byte[] buf = new byte[5];
		Assert.assertEquals(3, in.readFullySync(ByteBuffer.wrap(buf)));
		Assert.assertEquals(1, buf[0]);
		Assert.assertEquals(2, buf[1]);
		Assert.assertEquals(3, buf[2]);
		in.close();
		f.delete();
	}
	
}
