package net.lecousin.framework.core.tests.io.data;

import java.util.Arrays;

import net.lecousin.framework.core.test.io.data.TestCharsWritable;
import net.lecousin.framework.io.data.CompositeChars;
import net.lecousin.framework.io.data.CharArray;

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
			CharArray.Writable r = new CharArray.Writable(data, initialPos + i, len, false);
			c.add(r);
		}
		return c;
	}
	
	@Override
	protected void check(char[] expected, int expectedOffset, int bufferOffset, int len) {
		for (int i = 0; i < len; ++i)
			Assert.assertEquals(expected[i + expectedOffset], ((CharArray)((CompositeChars.Writable)buffer).getWrappedBuffers().get(0)).getArray()[initialPos + bufferOffset + i]);
	}
	
	@Test
	public void testConstructorArray() {
		CompositeChars.Writable c = new CompositeChars.Writable(
			new CharArray.Writable(new char[10], false),
			new CharArray.Writable(new char[5], false),
			new CharArray.Writable(new char[3], false)
		);
		Assert.assertEquals(18, c.remaining());
	}
	
	@Test
	public void testConstructorList() {
		CompositeChars.Writable c = new CompositeChars.Writable(Arrays.asList(
			new CharArray.Writable(new char[10], false),
			new CharArray.Writable(new char[5], false),
			new CharArray.Writable(new char[3], false)
		));
		Assert.assertEquals(18, c.remaining());
	}

}
