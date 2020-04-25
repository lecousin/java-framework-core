package net.lecousin.framework.core.tests.concurrent.tasks.drives;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.concurrent.tasks.drives.CreateDirectory;
import net.lecousin.framework.concurrent.tasks.drives.DirectoryReader;
import net.lecousin.framework.concurrent.tasks.drives.RemoveDirectoryContent;
import net.lecousin.framework.concurrent.tasks.drives.RemoveDirectory;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Task.Priority;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.core.test.LogInterceptor;
import net.lecousin.framework.progress.FakeWorkProgress;

public class TestDirectoryTasks extends LCCoreAbstractTest {

	@Test
	public void testCreateAndRemove() throws Exception {
		Path root = Files.createTempDirectory("test");
		File dir = new File(root.toFile(), "toto");
		Assert.assertFalse(dir.exists());
		CreateDirectory.task(dir, true, true, Task.Priority.NORMAL).start().getOutput().blockThrow(0);
		Assert.assertTrue(dir.exists());
		try {
			LogInterceptor.intercept(log -> log.trace instanceof IOException);
			CreateDirectory.task(dir, true, true, Task.Priority.NORMAL).start().getOutput().blockThrow(0);
			throw new AssertionError("CreateDirectoryTask should throw an exception when the directory already exists");
		} catch (IOException e) {
			// ok
		} finally {
			LogInterceptor.intercept(null);
		}
		RemoveDirectory.task(dir, null, 0, null, Task.Priority.NORMAL, false).start().getOutput().blockThrow(0);
		Assert.assertFalse(dir.exists());

		CreateDirectory.task(dir, false, true, Task.Priority.NORMAL).start().getOutput().blockThrow(0);
		Assert.assertTrue(dir.exists());
		File subDir = new File(dir, "titi/tata");
		Assert.assertFalse(subDir.exists());
		CreateDirectory.task(subDir, true, true, Task.Priority.NORMAL).start().getOutput().blockThrow(0);
		Assert.assertTrue(subDir.exists());
		Assert.assertTrue(new File(dir, "titi").exists());
		RemoveDirectoryContent.task(dir, null, 0, Task.Priority.NORMAL, false).start().getOutput().blockThrow(0);
		Assert.assertTrue(dir.exists());
		Assert.assertFalse(subDir.exists());
		Assert.assertFalse(new File(dir, "titi").exists());
		dir.deleteOnExit();
		root.toFile().deleteOnExit();
	}
	
	@Test
	public void testDirectoryReader() throws Exception {
		Path root = Files.createTempDirectory("test");
		File dir = new File(root.toFile(), "toto");
		Assert.assertTrue(dir.mkdir());
		File file = new File(root.toFile(), "titi");
		Assert.assertTrue(file.createNewFile());
		
		DirectoryReader.Request request = new DirectoryReader.Request();
		request.readCreation().readIsSymbolicLink().readLastAccess().readLastModified().readSize();
		DirectoryReader reader = new DirectoryReader(root.toFile(), request, null);
		Assert.assertEquals(root.toFile(), reader.getDirectory());
		Task.file(root.toFile(), "test", Priority.NORMAL, reader, null).start().getOutput().blockThrow(0);
		
		DirectoryReader.ListSubDirectories.task(root.toFile(), Task.Priority.NORMAL)
		.start().getOutput().blockThrow(0);
		
		RemoveDirectoryContent.task(root.toFile(), null, 0, Task.Priority.NORMAL, false).start().getOutput().blockThrow(0);
		request = new DirectoryReader.Request();
		reader = new DirectoryReader(root.toFile(), request, null);
		Task.file(root.toFile(), "test", Priority.NORMAL, reader, null).start().getOutput().blockThrow(0);
		
		request = new DirectoryReader.Request();
		reader = new DirectoryReader(root.toFile(), request, new FakeWorkProgress());
		Task.file(root.toFile(), "test", Priority.NORMAL, reader, null).start().getOutput().blockThrow(0);
	}
	
}
