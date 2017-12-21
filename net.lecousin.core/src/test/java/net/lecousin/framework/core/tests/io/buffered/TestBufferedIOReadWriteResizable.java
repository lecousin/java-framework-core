package net.lecousin.framework.core.tests.io.buffered;

import java.io.File;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestReadWriteResizable;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.buffering.BufferedIO;

public class TestBufferedIOReadWriteResizable extends TestReadWriteResizable {

	@SuppressWarnings({ "unchecked", "resource" })
	@Override
	protected BufferedIO.ReadWrite openReadWriteResizable() throws Exception {
		File tmpFile = File.createTempFile("test", "bufferedio.rw");
		tmpFile.deleteOnExit();
		return new BufferedIO.ReadWrite(new FileIO.ReadWrite(tmpFile, Task.PRIORITY_NORMAL), 4096, 0);
	}
	
}
