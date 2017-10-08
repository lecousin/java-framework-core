package net.lecousin.framework.core.tests.io.impl;

import net.lecousin.framework.core.tests.io.TestReadWriteResizable;
import net.lecousin.framework.io.buffering.MemoryIO;

public class TestMemoryIOReadWriteResizable extends TestReadWriteResizable {

	@SuppressWarnings("unchecked")
	@Override
	protected MemoryIO openReadWriteResizable() {
		return new MemoryIO(4096, "test");
	}
	
}
