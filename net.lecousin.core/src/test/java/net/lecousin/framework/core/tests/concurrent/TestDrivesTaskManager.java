package net.lecousin.framework.core.tests.concurrent;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.concurrent.DrivesTaskManager;
import net.lecousin.framework.concurrent.DrivesTaskManager.DrivesProvider;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.tasks.drives.CreateDirectoryTask;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.util.Pair;

public class TestDrivesTaskManager extends LCCoreAbstractTest {

	@Test(timeout=60000)
	public void simpleTests() {
		Object res1 = Threading.getDrivesTaskManager().getResource(new File("."));
		Object res2 = Threading.getDrivesTaskManager().getResource(new File(".").getAbsolutePath());
		Assert.assertEquals(res1, res2);
		
		Threading.getDrivesTaskManager().getResources();
	}
	
	@Test(timeout=120000)
	public void testDrivesProvider() throws Exception {
		DrivesTaskManager tm = Threading.getDrivesTaskManager();
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
				Consumer<Pair<Object, List<File>>> onNewDrive,
				Consumer<Pair<Object, List<File>>> onDriveRemoved,
				Consumer<Pair<Object, File>> onNewPartition,
				Consumer<Pair<Object, File>> onPartitionRemoved
			) {
				onNewDrive.accept(new Pair<>(fakeDrive1, Collections.singletonList(tmpDir)));
				onNewPartition.accept(new Pair<>(fakeDrive1, dir));
				onPartitionRemoved.accept(new Pair<>(fakeDrive1, dir));
				spDrive2Appear.onDone(() -> {
					onNewDrive.accept(new Pair<>(fakeDrive2, Collections.singletonList(tmpDir)));
					spDrive2AppearDone.unblock();
				});
				spDrive1Disappear.onDone(() -> {
					onDriveRemoved.accept(new Pair<>(fakeDrive1, Collections.singletonList(tmpDir)));
					spDrive1DisappearDone.unblock();
				});
			}
		});
		
		CreateDirectoryTask task = new CreateDirectoryTask(dir, false, true, Task.PRIORITY_NORMAL);
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
		task = new CreateDirectoryTask(dir, false, true, Task.PRIORITY_NORMAL);
		Assert.assertEquals(fakeDrive2, task.getTaskManager().getResource());
	}
	
}
