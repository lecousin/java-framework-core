package net.lecousin.framework.core.tests.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.tasks.drives.ReadFullFile;
import net.lecousin.framework.concurrent.tasks.drives.RenameFile;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.TemporaryFiles;

import org.junit.Assert;
import org.junit.Test;

public class TestFileTasks extends LCCoreAbstractTest {

	@Test
	public void testRenameFileTask() throws Exception {
		File src = TemporaryFiles.get().createFileSync("test", "rename");
		File dst = TemporaryFiles.get().createFileSync("test", "renamed");
		dst.delete();
		Assert.assertTrue(src.exists());
		Assert.assertFalse(dst.exists());
		RenameFile.rename(src, dst, Task.Priority.NORMAL).blockThrow(0);
		Assert.assertFalse(src.exists());
		Assert.assertTrue(dst.exists());

		try {
			RenameFile.rename(dst, new File("//////!?*"), Task.Priority.NORMAL).blockThrow(0);
			throw new AssertionError("must throw an error");
		} catch (IOException e) {
			// ok
		}
		Assert.assertTrue(dst.exists());
		
		File dst2 = TemporaryFiles.get().createFileSync("test", "renamed2");
		Assert.assertTrue(dst2.exists());
		try {
			RenameFile.rename(dst, dst2, Task.Priority.NORMAL).blockThrow(0);
			throw new AssertionError("must throw an error");
		} catch (IOException e) {
			// ok
		}
		Assert.assertTrue(dst.exists());
	}
	
	@Test
	public void testReadFullFile() throws Exception {
		File f = TemporaryFiles.get().createFileSync("test", "readfull");
		byte[] data = new byte[50000];
		for (int i = 0; i < data.length; ++i)
			data[i] = (byte)(i * 81);
		FileIO.WriteOnly out = new FileIO.WriteOnly(f, Priority.NORMAL);
		out.writeSync(ByteBuffer.wrap(data));
		out.close();
		
		byte[] read = ReadFullFile.create(f, Priority.NORMAL).start().getOutput().blockResult(0);
		Assert.assertEquals(data.length, read.length);
		Assert.assertArrayEquals(data, read);
	}
	
}
