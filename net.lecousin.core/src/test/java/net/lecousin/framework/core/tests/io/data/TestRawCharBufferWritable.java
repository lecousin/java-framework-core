package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.io.data.TestCharsWritable;
import net.lecousin.framework.io.data.RawCharBuffer;

import org.junit.Assert;

public class TestRawCharBufferWritable extends TestCharsWritable {

	public TestRawCharBufferWritable(int initialPos, int length, boolean useSubBuffer) {
		super(new RawCharBuffer(new char[initialPos + length + 10], initialPos, length), initialPos, length, useSubBuffer);
	}
	
	@Override
	protected void check(char[] expected, int expectedOffset, int bufferOffset, int len) {
		for (int i = 0; i < len; ++i)
			Assert.assertEquals(expected[i + expectedOffset], ((RawCharBuffer)buffer).array[initialPos + bufferOffset + i]);
	}
	
}
