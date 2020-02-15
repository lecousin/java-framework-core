package net.lecousin.framework.core.tests.concurrent.tasks.drives;

import java.io.File;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.concurrent.tasks.drives.DriveOperationsSequence;
import net.lecousin.framework.concurrent.tasks.drives.DriveOperationsSequence.ReadOperation;
import net.lecousin.framework.concurrent.tasks.drives.DriveOperationsSequence.WriteIntegerOperation;
import net.lecousin.framework.concurrent.tasks.drives.DriveOperationsSequence.WriteLongOperation;
import net.lecousin.framework.concurrent.tasks.drives.DriveOperationsSequence.WriteOperationSubBuffer;
import net.lecousin.framework.concurrent.tasks.drives.FileAccess;
import net.lecousin.framework.concurrent.tasks.drives.FullReadFileTask;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.util.DataUtil;

import org.junit.Assert;
import org.junit.Test;

public class TestDriveOperationsSequence extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		File file = File.createTempFile("test", "sequence");
		file.deleteOnExit();
		DriveOperationsSequence seq = new DriveOperationsSequence(Threading.getDrivesTaskManager().getTaskManager(file), "test", Task.PRIORITY_NORMAL, true);
		FileAccess io = new FileAccess(file, "rw", Task.PRIORITY_NORMAL);
		io.getStartingTask().start();
		io.open();
		io.getDirectAccess();
		seq.add(new WriteLongOperation(io, 0, 1234L, ByteBuffer.allocate(8)));
		seq.add(new WriteIntegerOperation(io, 10, 9876, ByteBuffer.allocate(8)));
		byte[] buf = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		seq.add(new WriteOperationSubBuffer(io, 8, ByteBuffer.wrap(buf), 4, 2));
		byte[] res = new byte[14];
		seq.add(new ReadOperation() {
			@Override
			public FileAccess getFile() {
				return io;
			}
			@Override
			public long getPosition() {
				return 0;
			}
			@Override
			public ByteBuffer getBuffer() {
				return ByteBuffer.wrap(res);
			}
		});
		seq.endOfOperations();
		seq.getOutput().blockThrow(0);
		Assert.assertEquals(1234L, DataUtil.Read64.BE.read(res, 0));
		Assert.assertEquals(9876, DataUtil.Read32.BE.read(res, 10));
		Assert.assertEquals((byte)5, res[8]);
		Assert.assertEquals((byte)6, res[9]);
		io.close();
		byte[] b = new FullReadFileTask(file, Task.PRIORITY_NORMAL).start().getOutput().blockResult(0);
		Assert.assertArrayEquals(res, b);
	}
	
}
