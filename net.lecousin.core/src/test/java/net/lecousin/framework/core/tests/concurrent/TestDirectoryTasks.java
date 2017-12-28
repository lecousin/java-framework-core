package net.lecousin.framework.core.tests.concurrent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.tasks.drives.CreateDirectoryTask;
import net.lecousin.framework.concurrent.tasks.drives.RemoveDirectoryContentTask;
import net.lecousin.framework.concurrent.tasks.drives.RemoveDirectoryTask;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

public class TestDirectoryTasks extends LCCoreAbstractTest {

	@Test
	public void testCreateAndRemove() throws Exception {
		Path root = Files.createTempDirectory("test");
		File dir = new File(root.toFile(), "toto");
		Assert.assertFalse(dir.exists());
		new CreateDirectoryTask(dir, true, true, Task.PRIORITY_NORMAL).start().getOutput().blockThrow(0);
		Assert.assertTrue(dir.exists());
		try {
			new CreateDirectoryTask(dir, true, true, Task.PRIORITY_NORMAL).start().getOutput().blockThrow(0);
			throw new AssertionError("CreateDirectoryTask should throw an exception when the directory already exists");
		} catch (IOException e) {
			// ok
		}
		new RemoveDirectoryTask(dir, null, 0, null, Task.PRIORITY_NORMAL, false).start().getOutput().blockThrow(0);
		Assert.assertFalse(dir.exists());

		new CreateDirectoryTask(dir, true, true, Task.PRIORITY_NORMAL).start().getOutput().blockThrow(0);
		Assert.assertTrue(dir.exists());
		File subDir = new File(dir, "titi/tata");
		Assert.assertFalse(subDir.exists());
		new CreateDirectoryTask(subDir, true, true, Task.PRIORITY_NORMAL).start().getOutput().blockThrow(0);
		Assert.assertTrue(subDir.exists());
		Assert.assertTrue(new File(dir, "titi").exists());
		new RemoveDirectoryContentTask(dir, null, 0, Task.PRIORITY_NORMAL, false).start().getOutput().blockThrow(0);
		Assert.assertTrue(dir.exists());
		Assert.assertFalse(subDir.exists());
		Assert.assertFalse(new File(dir, "titi").exists());
		dir.deleteOnExit();
		root.toFile().deleteOnExit();
	}
	
}
