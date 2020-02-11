package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.io.data.TestBytesWritable;
import net.lecousin.framework.io.data.BytesFromIso8859CharArray;
import net.lecousin.framework.io.data.CharArray;

import org.junit.Assert;

public class TestBytesFromIso8859CharArrayWritable extends TestBytesWritable {

	public TestBytesFromIso8859CharArrayWritable(int initialPos, int length, boolean useSubBuffer) {
		super(new BytesFromIso8859CharArray.Writable(new CharArray.Writable(new char[initialPos + length + 10], initialPos, length, true), true), initialPos, length, useSubBuffer);
	}
	
	@Override
	protected void check(byte[] expected, int expectedOffset, int bufferOffset, int len) {
		for (int i = 0; i < len; ++i)
			Assert.assertEquals(expected[i + expectedOffset], ((BytesFromIso8859CharArray)buffer).getOriginalBuffer().getArray()[initialPos + bufferOffset + i]);
	}
	
}
