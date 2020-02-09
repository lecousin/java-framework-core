package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.io.data.TestBytesWritable;
import net.lecousin.framework.io.data.RawByteBuffer;

import org.junit.Assert;

public class TestRawByteBufferWritable extends TestBytesWritable {

	public TestRawByteBufferWritable(int initialPos, int length, boolean useSubBuffer) {
		super(new RawByteBuffer(new byte[initialPos + length + 10], initialPos, length), initialPos, length, useSubBuffer);
	}
	
	@Override
	protected void check(byte[] expected, int expectedOffset, int bufferOffset, int len) {
		for (int i = 0; i < len; ++i)
			Assert.assertEquals(expected[i + expectedOffset], ((RawByteBuffer)buffer).array[initialPos + bufferOffset + i]);
	}
	
}
