package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.io.data.TestCharsWritable;
import net.lecousin.framework.io.data.CharArray;

import org.junit.Assert;

public class TestCharArrayWritable extends TestCharsWritable {

	public TestCharArrayWritable(int initialPos, int length, boolean useSubBuffer) {
		super(new CharArray.Writable(new char[initialPos + length + 10], initialPos, length, true), initialPos, length, useSubBuffer);
	}
	
	@Override
	protected void check(char[] expected, int expectedOffset, int bufferOffset, int len) {
		for (int i = 0; i < len; ++i)
			Assert.assertEquals(expected[i + expectedOffset], ((CharArray)buffer).getArray()[initialPos + bufferOffset + i]);
	}
	
}
