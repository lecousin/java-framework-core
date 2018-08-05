package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.io.IOFromOutputStream;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;
import net.lecousin.framework.progress.FakeWorkProgress;

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
			Assert.assertEquals(data[10 + i - 70], buf[i]);
		io.close();
		
		io = new ByteArrayIO(buf, "test");
		Assert.assertEquals(0, IOUtil.skipSyncByReading(io, -1));
		Assert.assertEquals(0, IOUtil.skipAsyncByReading(io, -1, null).blockResult(0).intValue());
		Assert.assertEquals(0, IOUtil.skipAsyncByReading(io, -1, (p) -> {}).blockResult(0).intValue());
		io.close();
	}

	@Test(timeout=120000)
	public void test2() throws Exception {
		String expected = "This is a string to test";
		byte[] data = expected.getBytes(StandardCharsets.UTF_16);
		ByteArrayIO io;
		
		io = new ByteArrayIO(data, "test");
		Assert.assertEquals(expected, IOUtil.readFullyAsString(io, StandardCharsets.UTF_16, Task.PRIORITY_NORMAL).blockResult(0).asString());
		io.close();
		
		io = new ByteArrayIO(data, "test");
		StringBuilder sb = new StringBuilder();
		IOUtil.readFullyAsStringSync(io, StandardCharsets.UTF_16, sb);
		Assert.assertEquals(expected, sb.toString());
		io.close();
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testCopy() throws Exception {
		String string = "This is a string to test";
		byte[] data = string.getBytes(StandardCharsets.UTF_16);

		File tmp1 = TemporaryFiles.get().createFileSync("test", "ioutilcopy");
		IOUtil.copy(new ByteArrayIO(data, "test"), new FileIO.WriteOnly(tmp1, Task.PRIORITY_NORMAL), -1, true, new FakeWorkProgress(), 100).blockThrow(0);
		
		File tmp2 = TemporaryFiles.get().createFileSync("test", "ioutilcopy");
		IOUtil.copy(tmp1, tmp2, Task.PRIORITY_NORMAL, -1, new FakeWorkProgress(), 100, null).blockThrow(0);
		Assert.assertEquals(string, IOUtil.readFullyAsStringSync(tmp2, StandardCharsets.UTF_16));
		tmp2.delete();
		
		tmp2 = TemporaryFiles.get().createFileSync("test", "ioutilcopy");
		InputStream in = new FileInputStream(tmp1);
		IOUtil.copy(new IOFromInputStream(in, "test", Threading.getCPUTaskManager(), Task.PRIORITY_NORMAL), new FileIO.WriteOnly(tmp2, Task.PRIORITY_NORMAL), -1, true, new FakeWorkProgress(), 100).blockThrow(0);
		Assert.assertEquals(string, IOUtil.readFullyAsStringSync(tmp2, StandardCharsets.UTF_16));
		tmp2.delete();
		
		tmp2 = TemporaryFiles.get().createFileSync("test", "ioutilcopy");
		in = new FileInputStream(tmp1);
		IOUtil.copy(new IOFromInputStream(in, "test", Threading.getDrivesTaskManager().getTaskManager(tmp2), Task.PRIORITY_NORMAL), new FileIO.WriteOnly(tmp2, Task.PRIORITY_NORMAL), -1, true, new FakeWorkProgress(), 100).blockThrow(0);
		Assert.assertEquals(string, IOUtil.readFullyAsStringSync(tmp2, StandardCharsets.UTF_16));
		tmp2.delete();

		tmp2 = TemporaryFiles.get().createFileSync("test", "ioutilcopy");
		OutputStream out = new FileOutputStream(tmp2);
		IOUtil.copy(new SimpleBufferedReadable(new FileIO.ReadOnly(tmp1, Task.PRIORITY_NORMAL), 10), new IOFromOutputStream(out, "test", Threading.getCPUTaskManager(), Task.PRIORITY_NORMAL), -1, true, new FakeWorkProgress(), 100).blockThrow(0);
		Assert.assertEquals(string, IOUtil.readFullyAsStringSync(tmp2, StandardCharsets.UTF_16));
		tmp2.delete();
		
		tmp2 = TemporaryFiles.get().createFileSync("test", "ioutilcopy");
		IOUtil.copy(new FileIO.ReadOnly(tmp1, Task.PRIORITY_NORMAL), new FileIO.WriteOnly(tmp2, Task.PRIORITY_NORMAL), tmp1.length(), true, new FakeWorkProgress(), 100).blockThrow(0);
		Assert.assertEquals(string, IOUtil.readFullyAsStringSync(tmp2, StandardCharsets.UTF_16));
		tmp2.delete();
		
		// big file
		tmp1 = TemporaryFiles.get().createFileSync("test", "ioutilcopy");
		out = new FileOutputStream(tmp1);
		for (int i = 0; i < 10; ++i)
			out.write(new byte[64 * 1024]);
		tmp2 = TemporaryFiles.get().createFileSync("test", "ioutilcopy");
		IOUtil.copy(new FileIO.ReadOnly(tmp1, Task.PRIORITY_NORMAL), new FileIO.WriteOnly(tmp2, Task.PRIORITY_NORMAL), tmp1.length(), true, new FakeWorkProgress(), 100).blockThrow(0);
		tmp1.delete();
		tmp2.delete();
	}
	
	@Test(timeout=120000)
	public void testToTempFile() throws Exception {
		byte[] b = new byte[65000];
		for (int i = 0; i < b.length; ++i)
			b[i] = (byte)(i % 167);
		File f = IOUtil.toTempFile(b).blockResult(0);
		b = IOUtil.readFully(f, Task.PRIORITY_NORMAL).blockResult(0);
		for (int i = 0; i < b.length; ++i)
			Assert.assertEquals((byte)(i % 167), b[i]);
		f.delete();
	}
}
