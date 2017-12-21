package net.lecousin.framework.core.tests.io.buffered;

import net.lecousin.framework.core.test.io.TestReadWriteResizable;
import net.lecousin.framework.io.buffering.ByteArrayIO;

public class TestByteArrayIOReadWriteResizable extends TestReadWriteResizable {

	@SuppressWarnings("unchecked")
	@Override
	protected ByteArrayIO openReadWriteResizable() {
		return new ByteArrayIO("test");
	}
	
}
