package net.lecousin.framework.core.tests.concurrent;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.concurrent.FileSystemWatcher;
import net.lecousin.framework.concurrent.FileSystemWatcher.PathEventListener;
import net.lecousin.framework.concurrent.async.WaitingDataQueueSynchronizationPoint;
import net.lecousin.framework.concurrent.tasks.drives.RemoveFile;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.Triple;

public class TestFileSystemWatcher extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		Path dir = Files.createTempDirectory("test");
		WaitingDataQueueSynchronizationPoint<Triple<Path, String, Integer>, Exception> queue = new WaitingDataQueueSynchronizationPoint<>();
		PathEventListener listener = new PathEventListener() {
			@Override
			public void fileRemoved(Path parent, String childName) {
				queue.newDataReady(new Triple<>(parent, childName, Integer.valueOf(1)));
			}
			@Override
			public void fileModified(Path parent, String childName) {
				queue.newDataReady(new Triple<>(parent, childName, Integer.valueOf(2)));
			}
			@Override
			public void fileCreated(Path parent, String childName) {
				queue.newDataReady(new Triple<>(parent, childName, Integer.valueOf(3)));
			}
		};
		Assert.assertFalse(queue.isDone());
		Runnable stop = FileSystemWatcher.watch(dir, listener);
		Assert.assertFalse(queue.isDone());

		Path file = Files.createTempFile(dir, "test", "file");
		Triple<Path, String, Integer> event = queue.waitForData(5000);
		Assert.assertNotNull(event);
		Assert.assertEquals(3, event.getValue3().intValue());
		Assert.assertEquals(dir, event.getValue1());
		Assert.assertTrue(event.getValue2().startsWith("test") && event.getValue2().endsWith("file"));
		Assert.assertFalse(queue.isDone());
		
		FileOutputStream out = new FileOutputStream(file.toFile());
		out.write(new byte[] { 1, 2, 3 });
		out.close();
		event = queue.waitForData(5000);
		Assert.assertNotNull(event);
		Assert.assertEquals(2, event.getValue3().intValue());
		Assert.assertEquals(dir, event.getValue1());
		Assert.assertTrue(event.getValue2().startsWith("test") && event.getValue2().endsWith("file"));
		do {
			event = queue.waitForData(1000);
		} while (event != null);
		Assert.assertFalse(queue.isDone());
		
		RemoveFile.task(file.toFile(), Task.Priority.NORMAL).start().getOutput().blockThrow(0);
		do {
			event = queue.waitForData(5000);
			Assert.assertNotNull(event);
			if (event.getValue3().intValue() == 2) continue;
			break;
		} while (true);
		Assert.assertEquals(1, event.getValue3().intValue());
		Assert.assertEquals(dir, event.getValue1());
		Assert.assertTrue(event.getValue2().startsWith("test") && event.getValue2().endsWith("file"));
		Assert.assertFalse(queue.isDone());
		
		stop.run();
	}
	
}
