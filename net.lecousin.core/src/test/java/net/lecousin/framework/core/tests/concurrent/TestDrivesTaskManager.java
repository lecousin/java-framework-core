package net.lecousin.framework.core.tests.concurrent;

import java.io.File;
import java.util.Collections;
import java.util.List;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.DrivesTaskManager;
import net.lecousin.framework.concurrent.DrivesTaskManager.DrivesProvider;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.concurrent.tasks.drives.CreateDirectoryTask;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.event.Listener;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.util.Pair;

import org.junit.Assert;
import org.junit.Test;

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
		SynchronizationPoint<Exception> spDrive2Appear = new SynchronizationPoint<>();
		SynchronizationPoint<Exception> spDrive2AppearDone = new SynchronizationPoint<>();
		SynchronizationPoint<Exception> spDrive1Disappear = new SynchronizationPoint<>();
		SynchronizationPoint<Exception> spDrive1DisappearDone = new SynchronizationPoint<>();
		tm.setDrivesProvider(new DrivesProvider() {
			@Override
			public void provide(
				Listener<Pair<Object, List<File>>> onNewDrive,
				Listener<Pair<Object, List<File>>> onDriveRemoved,
				Listener<Pair<Object, File>> onNewPartition,
				Listener<Pair<Object, File>> onPartitionRemoved
			) {
				onNewDrive.fire(new Pair<>(fakeDrive1, Collections.singletonList(tmpDir)));
				spDrive2Appear.listenInline(() -> {
					onNewDrive.fire(new Pair<>(fakeDrive2, Collections.singletonList(tmpDir)));
					spDrive2AppearDone.unblock();
				});
				spDrive1Disappear.listenInline(() -> {
					onDriveRemoved.fire(new Pair<>(fakeDrive1, Collections.singletonList(tmpDir)));
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
