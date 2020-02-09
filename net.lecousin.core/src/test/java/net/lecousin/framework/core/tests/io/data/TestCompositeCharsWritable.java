package net.lecousin.framework.core.tests.io.data;

import java.util.Arrays;

import net.lecousin.framework.core.test.io.data.TestCharsWritable;
import net.lecousin.framework.io.data.CompositeChars;
import net.lecousin.framework.io.data.RawCharBuffer;

import org.junit.Assert;
import org.junit.Test;

public class TestCompositeCharsWritable extends TestCharsWritable {

	public TestCompositeCharsWritable(int initialPos, int length, boolean useSubBuffer) {
		super(create(initialPos, length), initialPos, length, useSubBuffer);
	}
	
	private static CompositeChars.Writable create(int initialPos, int length) {
		char[] data = new char[initialPos + length];
		CompositeChars.Writable c = new CompositeChars.Writable();
		for (int i = 0; i < length; i += 5) {
			int len = Math.min(5, length - i);
			RawCharBuffer r = new RawCharBuffer(data, initialPos + i, len);
			c.add(r);
		}
		return c;
	}
	
	@Override
	protected void check(char[] expected, int expectedOffset, int bufferOffset, int len) {
		for (int i = 0; i < len; ++i)
			Assert.assertEquals(expected[i + expectedOffset], ((RawCharBuffer)((CompositeChars.Writable)buffer).getWrappedChars().get(0)).array[initialPos + bufferOffset + i]);
	}
	
	@Test
	public void testConstructorArray() {
		CompositeChars.Writable c = new CompositeChars.Writable(
			new RawCharBuffer(new char[10]),
			new RawCharBuffer(new char[5]),
			new RawCharBuffer(new char[3])
		);
		Assert.assertEquals(18, c.remaining());
	}
	
	@Test
	public void testConstructorList() {
		CompositeChars.Writable c = new CompositeChars.Writable(Arrays.asList(
			new RawCharBuffer(new char[10]),
			new RawCharBuffer(new char[5]),
			new RawCharBuffer(new char[3])
		));
		Assert.assertEquals(18, c.remaining());
	}

}
