package net.lecousin.framework.core.tests.io.data;

import java.util.Arrays;

import net.lecousin.framework.core.test.io.data.TestBytesWritable;
import net.lecousin.framework.io.data.CompositeBytes;
import net.lecousin.framework.io.data.RawByteBuffer;

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
			RawByteBuffer r = new RawByteBuffer(data, initialPos + i, len);
			c.add(r);
		}
		return c;
	}
	
	@Override
	protected void check(byte[] expected, int expectedOffset, int bufferOffset, int len) {
		for (int i = 0; i < len; ++i)
			Assert.assertEquals(expected[i + expectedOffset], ((RawByteBuffer)((CompositeBytes.Writable)buffer).getWrappedChars().get(0)).array[initialPos + bufferOffset + i]);
	}
	
	@Test
	public void testConstructorArray() {
		CompositeBytes.Writable c = new CompositeBytes.Writable(
			new RawByteBuffer(new byte[10]),
			new RawByteBuffer(new byte[5]),
			new RawByteBuffer(new byte[3])
		);
		Assert.assertEquals(18, c.remaining());
	}
	
	@Test
	public void testConstructorList() {
		CompositeBytes.Writable c = new CompositeBytes.Writable(Arrays.asList(
			new RawByteBuffer(new byte[10]),
			new RawByteBuffer(new byte[5]),
			new RawByteBuffer(new byte[3])
		));
		Assert.assertEquals(18, c.remaining());
	}

}
