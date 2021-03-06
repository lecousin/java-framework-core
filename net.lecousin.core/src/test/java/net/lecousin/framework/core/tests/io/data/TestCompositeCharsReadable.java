package net.lecousin.framework.core.tests.io.data;

import java.util.Arrays;

import net.lecousin.framework.core.test.io.data.TestCharsReadable;
import net.lecousin.framework.io.data.CompositeChars;
import net.lecousin.framework.io.data.CharArray;

import org.junit.Assert;
import org.junit.Test;

public class TestCompositeCharsReadable extends TestCharsReadable {

	public TestCompositeCharsReadable(char[] data, int initialPos, int length, boolean useSubBuffer) {
		super(create(data, initialPos, length), data, initialPos, length, useSubBuffer);
	}
	
	private static CompositeChars.Readable create(char[] data, int initialPos, int length) {
		CompositeChars.Readable c = new CompositeChars.Readable();
		for (int i = 0; i < length; i += 5) {
			int len = Math.min(5, length - i);
			CharArray r = new CharArray(data, initialPos + i, len);
			c.add(r);
		}
		return c;
	}
	
	@Test
	public void testConstructorArray() {
		CompositeChars.Readable c = new CompositeChars.Readable(
			new CharArray(new char[10]),
			new CharArray(new char[5]),
			new CharArray(new char[3])
		);
		Assert.assertEquals(18, c.remaining());
	}
	
	@Test
	public void testConstructorList() {
		CompositeChars.Readable c = new CompositeChars.Readable(Arrays.asList(
			new CharArray(new char[10]),
			new CharArray(new char[5]),
			new CharArray(new char[3])
		));
		Assert.assertEquals(18, c.remaining());
	}
	
}
