package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;
import java.nio.ByteBuffer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestReadWriteResizable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.BufferedIO;

public class TestBufferedIOReadWriteResizableManyBuffers extends TestReadWriteResizable {

	@SuppressWarnings({ "unchecked", "resource" })
	@Override
	protected BufferedIO.ReadWrite openReadWriteResizable() throws Exception {
		File tmpFile = File.createTempFile("test", "bufferedio.rw");
		tmpFile.deleteOnExit();
		FileIO.ReadWrite fio = new FileIO.ReadWrite(tmpFile, Task.PRIORITY_NORMAL);
		fio.setSizeSync(10000000);
		BufferedIO.ReadWrite io = new BufferedIO.ReadWrite(fio, 10000000, 32, 128, false);
		byte[] b = new byte[500];
		for (int i = 0; i < 9000000; i += 123456)
			io.readSync(i, ByteBuffer.wrap(b));
		io.setSizeSync(0);
		return io;
	}
	
}
