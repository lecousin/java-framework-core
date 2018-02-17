package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.ByteArrayIO;

import org.junit.Assert;
import org.junit.Test;

public class TestIOUtil extends LCCoreAbstractTest {

	@Test(timeout=120000)
	public void test1() throws Exception {
		byte[] data = new byte[150];
		for (int i = 0; i < 150; ++i) data[i] = (byte)(i + 21 % 140);
		ByteArrayIO io;
		io = new ByteArrayIO(data, "test");
		File file = IOUtil.toTempFile(io).blockResult(0);
		Assert.assertArrayEquals(data, IOUtil.readFully(file, Task.PRIORITY_NORMAL).blockResult(0));
		io.close();

		Assert.assertArrayEquals(data, IOUtil.readFully(file, Task.PRIORITY_NORMAL).blockResult(0));
		
		byte[] buf = new byte[150];
		
		io = new ByteArrayIO(data, "test");
		Assert.assertEquals(150, IOUtil.readSyncUsingAsync(io, ByteBuffer.wrap(buf)));
		Assert.assertArrayEquals(data, buf);
		io.close();
		
		io = new ByteArrayIO(data, "test");
		Assert.assertEquals(50, IOUtil.readSyncUsingAsync(io, 100, ByteBuffer.wrap(buf)));
		io.close();
		
		io = new ByteArrayIO(data, "test");
		Assert.assertEquals(150, IOUtil.readFullySyncUsingAsync(io, ByteBuffer.wrap(buf)));
		Assert.assertArrayEquals(data, buf);
		io.close();
		
		io = new ByteArrayIO(data, "test");
		Assert.assertEquals(50, IOUtil.readFullySyncUsingAsync(io, 100, ByteBuffer.wrap(buf)));
		io.close();

		io = new ByteArrayIO(data, "test");
		Assert.assertEquals(10, IOUtil.skipSyncUsingAsync(io, 10));
		io.close();

		io = new ByteArrayIO(buf, "test");
		Assert.assertEquals(150, IOUtil.writeSyncUsingAsync(io, ByteBuffer.wrap(data)));
		Assert.assertArrayEquals(data, buf);
		io.close();

		io = new ByteArrayIO(new byte[500], "test");
		Assert.assertEquals(150, IOUtil.writeSyncUsingAsync(io, 10, ByteBuffer.wrap(data)));
		io.close();

		io = new ByteArrayIO(data, "test");
		Assert.assertEquals(140, IOUtil.seekSyncUsingAsync(io, SeekType.FROM_END, 10));
		io.close();
		
		System.arraycopy(data, 0, buf, 0, data.length);
		io = new ByteArrayIO(buf, "test");
		IOUtil.copy(io, 10, 70, 50).blockThrow(0);
		for (int i = 70; i < 70 + 50; ++i)
			Assert.assertEquals(data[10 + i], buf[i]);
		io.close();
	}

	@Test(timeout=120000)
	public void test2() throws Exception {
		String expected = "This is a string to test";
		byte[] data = expected.getBytes(StandardCharsets.UTF_16);
		ByteArrayIO io;
		
		io = new ByteArrayIO(data, "test");
		Assert.assertEquals(expected, IOUtil.readFullyAsString(io, StandardCharsets.UTF_16, Task.PRIORITY_NORMAL).getOutput().blockResult(0).asString());
		io.close();
		
		io = new ByteArrayIO(data, "test");
		StringBuilder sb = new StringBuilder();
		IOUtil.readFullyAsStringSync(io, StandardCharsets.UTF_16, sb);
		Assert.assertEquals(expected, sb.toString());
		io.close();
	}
	
}
