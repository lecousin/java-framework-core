package net.lecousin.framework.core.tests.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.tasks.drives.CreateDirectory;
import net.lecousin.framework.concurrent.threads.DrivesThreadingManager;
import net.lecousin.framework.concurrent.threads.DrivesThreadingManager.DrivesProvider;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.threads.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.util.Triple;

import org.junit.Assert;
import org.junit.Test;

public class TestDrivesTaskManager extends LCCoreAbstractTest {

	@Test
	public void simpleTests() {
		Object res1 = Threading.getDrivesManager().getResource(new File("."));
		Object res2 = Threading.getDrivesManager().getResource(new File(".").getAbsolutePath());
		Assert.assertEquals(res1, res2);
		
		Threading.getDrivesManager().getResources();
	}
	
	@Test
	public void testDrivesProvider() throws Exception {
		DrivesThreadingManager tm = Threading.getDrivesManager();
		File tmpDir = TemporaryFiles.get().createDirectorySync("testDrivesTM");
		File dir = new File(tmpDir, "test");
		Object fakeDrive1 = new Object();
		Object fakeDrive2 = new Object();
		Async<Exception> spDrive2Appear = new Async<>();
		Async<Exception> spDrive2AppearDone = new Async<>();
		Async<Exception> spDrive1Disappear = new Async<>();
		Async<Exception> spDrive1DisappearDone = new Async<>();
		tm.setDrivesProvider(new DrivesProvider() {
			@Override
			public void provide(
				Consumer<Triple<Object, List<File>, Boolean>> onNewDrive,
				Consumer<Object> onDriveRemoved,
				Consumer<Pair<Object, File>> onNewPartition,
				Consumer<Pair<Object, File>> onPartitionRemoved
			) {
				onNewDrive.accept(new Triple<>(fakeDrive1, Collections.singletonList(tmpDir), Boolean.FALSE));
				onNewPartition.accept(new Pair<>(fakeDrive1, dir));
				onPartitionRemoved.accept(new Pair<>(fakeDrive1, dir));
				spDrive2Appear.onDone(() -> {
					onNewDrive.accept(new Triple<>(fakeDrive2, Collections.singletonList(tmpDir), Boolean.FALSE));
					spDrive2AppearDone.unblock();
				});
				spDrive1Disappear.onDone(() -> {
					onDriveRemoved.accept(new Pair<>(fakeDrive1, Collections.singletonList(tmpDir)));
					spDrive1DisappearDone.unblock();
				});
			}
		});
		
		Task<Void, IOException> task = CreateDirectory.task(dir, false, true, Task.Priority.NORMAL);
		task.executeIn(60000).start();
		Assert.assertEquals(fakeDrive1, task.getTaskManager().getResource());
		spDrive2Appear.unblock();
		spDrive2AppearDone.blockThrow(0);
		spDrive1Disappear.unblock();
		spDrive1DisappearDone.blockThrow(0);
		task.changeNextExecutionTime(System.currentTimeMillis());
		task.getOutput().block(0);
		Assert.assertEquals(fakeDrive2, task.getTaskManager().getResource());
		task.cancel(new CancelException("Test is ok"));
		task = CreateDirectory.task(dir, false, true, Task.Priority.NORMAL);
		Assert.assertEquals(fakeDrive2, task.getTaskManager().getResource());
	}
	
}
