package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.io.data.TestBytesWritable;
import net.lecousin.framework.io.data.ByteArray;

import org.junit.Assert;

public class TestByteArrayWritable extends TestBytesWritable {

	public TestByteArrayWritable(int initialPos, int length, boolean useSubBuffer) {
		super(new ByteArray.Writable(new byte[initialPos + length + 10], initialPos, length, true), initialPos, length, useSubBuffer);
	}
	
	@Override
	protected void check(byte[] expected, int expectedOffset, int bufferOffset, int len) {
		for (int i = 0; i < len; ++i)
			Assert.assertEquals(expected[i + expectedOffset], ((ByteArray)buffer).getArray()[initialPos + bufferOffset + i]);
	}
	
}
