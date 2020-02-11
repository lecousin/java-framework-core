package net.lecousin.framework.core.tests.io.data;

import java.nio.ByteBuffer;

import net.lecousin.framework.core.test.io.data.TestBytesWritable;
import net.lecousin.framework.io.data.ByteBufferAsBytes;

import org.junit.Assert;

public class TestByteBufferAsBytesWritable extends TestBytesWritable {

	public TestByteBufferAsBytesWritable(int initialPos, int length, boolean useSubBuffer) {
		super((ByteBufferAsBytes.Writable)ByteBufferAsBytes.create(ByteBuffer.wrap(new byte[initialPos + length + 10], initialPos, length).slice(), false), initialPos, length, useSubBuffer);
	}
	
	@Override
	protected void check(byte[] expected, int expectedOffset, int bufferOffset, int len) {
		for (int i = 0; i < len; ++i)
			Assert.assertEquals(expected[i + expectedOffset], ((ByteBufferAsBytes)buffer).toByteBuffer().array()[initialPos + bufferOffset + i]);
	}
	
}
