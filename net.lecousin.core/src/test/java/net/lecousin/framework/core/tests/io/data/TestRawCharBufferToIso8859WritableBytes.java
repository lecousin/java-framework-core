package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.io.data.TestBytesWritable;
import net.lecousin.framework.io.data.RawCharBuffer;

import org.junit.Assert;

public class TestRawCharBufferToIso8859WritableBytes extends TestBytesWritable {

	public TestRawCharBufferToIso8859WritableBytes(int initialPos, int length, boolean useSubBuffer) {
		super(new RawCharBuffer(new char[initialPos + length + 10], initialPos, length).asWritableIso8859Bytes(), initialPos, length, useSubBuffer);
	}
	
	@Override
	protected void check(byte[] expected, int expectedOffset, int bufferOffset, int len) {
		for (int i = 0; i < len; ++i)
			Assert.assertEquals(expected[i + expectedOffset], ((RawCharBuffer.Iso8859Buffer)buffer).getOriginalBuffer().array[initialPos + bufferOffset + i]);
	}
	
}
