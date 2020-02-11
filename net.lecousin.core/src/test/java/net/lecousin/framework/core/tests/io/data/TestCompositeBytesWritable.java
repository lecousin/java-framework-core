package net.lecousin.framework.core.tests.io.data;

import java.util.Arrays;

import net.lecousin.framework.core.test.io.data.TestBytesWritable;
import net.lecousin.framework.io.data.CompositeBytes;
import net.lecousin.framework.io.data.ByteArray;

import org.junit.Assert;
import org.junit.Test;

public class TestCompositeBytesWritable extends TestBytesWritable {

	public TestCompositeBytesWritable(int initialPos, int length, boolean useSubBuffer) {
		super(create(initialPos, length), initialPos, length, useSubBuffer);
	}
	
	private static CompositeBytes.Writable create(int initialPos, int length) {
		byte[] data = new byte[initialPos + length];
		CompositeBytes.Writable c = new CompositeBytes.Writable();
		for (int i = 0; i < length; i += 5) {
			int len = Math.min(5, length - i);
			ByteArray.Writable r = new ByteArray.Writable(data, initialPos + i, len, false);
			c.add(r);
		}
		return c;
	}
	
	@Override
	protected void check(byte[] expected, int expectedOffset, int bufferOffset, int len) {
		for (int i = 0; i < len; ++i)
			Assert.assertEquals(expected[i + expectedOffset], ((ByteArray)((CompositeBytes.Writable)buffer).getWrappedBuffers().get(0)).getArray()[initialPos + bufferOffset + i]);
	}
	
	@Test
	public void testConstructorArray() {
		CompositeBytes.Writable c = new CompositeBytes.Writable(
			new ByteArray.Writable(new byte[10], true),
			new ByteArray.Writable(new byte[5], false),
			new ByteArray.Writable(new byte[3], false)
		);
		Assert.assertEquals(18, c.remaining());
	}
	
	@Test
	public void testConstructorList() {
		CompositeBytes.Writable c = new CompositeBytes.Writable(Arrays.asList(
			new ByteArray.Writable(new byte[10], false),
			new ByteArray.Writable(new byte[5], false),
			new ByteArray.Writable(new byte[3], false)
		));
		Assert.assertEquals(18, c.remaining());
	}

}
