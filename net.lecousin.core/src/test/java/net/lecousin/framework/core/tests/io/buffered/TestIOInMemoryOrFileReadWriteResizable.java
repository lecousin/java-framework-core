package net.lecousin.framework.core.tests.io.buffered;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.io.TestReadWriteResizable;
import net.lecousin.framework.io.buffering.IOInMemoryOrFile;

public class TestIOInMemoryOrFileReadWriteResizable extends TestReadWriteResizable {

	@SuppressWarnings("unchecked")
	@Override
	protected IOInMemoryOrFile openReadWriteResizable() {
		return new IOInMemoryOrFile(4096, Task.PRIORITY_NORMAL, "test resizable IOInMemoryOrFile");
	}
	
}
