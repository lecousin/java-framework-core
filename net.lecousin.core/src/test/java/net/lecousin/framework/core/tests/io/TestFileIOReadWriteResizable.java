package net.lecousin.framework.core.tests.io;

import java.io.File;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestReadWriteResizable;
import net.lecousin.framework.io.FileIO;

public class TestFileIOReadWriteResizable extends TestReadWriteResizable {

	@SuppressWarnings("unchecked")
	@Override
	protected FileIO.ReadWrite openReadWriteResizable() throws Exception {
		File tmpFile = File.createTempFile("test", "fileio.rw");
		tmpFile.deleteOnExit();
		return new FileIO.ReadWrite(tmpFile, Task.PRIORITY_NORMAL);
	}
	
}
