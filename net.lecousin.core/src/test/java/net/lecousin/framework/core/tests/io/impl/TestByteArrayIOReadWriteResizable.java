package net.lecousin.framework.core.tests.io.impl;

import net.lecousin.framework.core.tests.io.TestReadWriteResizable;
import net.lecousin.framework.io.buffering.ByteArrayIO;

public class TestByteArrayIOReadWriteResizable extends TestReadWriteResizable {

	@SuppressWarnings("unchecked")
	@Override
	protected ByteArrayIO openReadWriteResizable() {
		return new ByteArrayIO("test");
	}
	
}
