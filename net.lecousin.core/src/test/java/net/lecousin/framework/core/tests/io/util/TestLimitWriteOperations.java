package net.lecousin.framework.core.tests.io.util;

import java.io.File;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.util.LimitWriteOperations;

import org.junit.Assert;
import org.junit.Test;

public class TestLimitWriteOperations extends LCCoreAbstractTest {

	@Test(timeout=120000)
	public void test() throws Exception {
		File tmp = File.createTempFile("test", "lwo");
		tmp.deleteOnExit();
		FileIO.WriteOnly io = new FileIO.WriteOnly(tmp, Task.PRIORITY_NORMAL);
		LimitWriteOperations writeOps = new LimitWriteOperations(io, 3);
		for (int i = 0; i < 500; ++i) {
			byte[] data = new byte[100];
			for (int j = 0; j < data.length; ++j)
				data[j] = (byte)j;
			data[i % 100] = (byte)((i % 69) + 5);
			writeOps.write(ByteBuffer.wrap(data));
		}
		writeOps.flush().blockThrow(0);
		io.close();
		FileIO.ReadOnly in = new FileIO.ReadOnly(tmp, Task.PRIORITY_NORMAL);
		byte[] buf = new byte[100];
		for (int i = 0; i < 500; ++i) {
			Assert.assertEquals("buffer " + i, 100, in.readFullySync(ByteBuffer.wrap(buf)));
			byte[] data = new byte[100];
			for (int j = 0; j < data.length; ++j)
				data[j] = (byte)j;
			data[i % 100] = (byte)((i % 69) + 5);
			Assert.assertArrayEquals("buffer " + i, data, buf);
		}
		Assert.assertTrue(in.readFullySync(ByteBuffer.wrap(buf)) <= 0);
		in.close();
	}
	
}
