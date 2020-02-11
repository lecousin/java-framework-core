package net.lecousin.framework.core.tests.io.data;

import java.nio.ByteBuffer;

import net.lecousin.framework.core.test.io.data.TestBytesReadable;
import net.lecousin.framework.io.data.ByteBufferAsBytes;

public class TestByteBufferAsBytesReadable extends TestBytesReadable {

	public TestByteBufferAsBytesReadable(byte[] data, int initialPos, int length, boolean useSubBuffer) {
		super(ByteBufferAsBytes.create(ByteBuffer.wrap(data, initialPos, length).slice(), false), data, initialPos, length, useSubBuffer);
	}
	
}
