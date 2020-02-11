package net.lecousin.framework.core.tests.io.data;

import java.util.Arrays;

import net.lecousin.framework.core.test.io.data.TestBytesReadable;
import net.lecousin.framework.io.data.CompositeBytes;
import net.lecousin.framework.io.data.ByteArray;

import org.junit.Assert;
import org.junit.Test;

public class TestCompositeBytesReadable extends TestBytesReadable {

	public TestCompositeBytesReadable(byte[] data, int initialPos, int length, boolean useSubBuffer) {
		super(create(data, initialPos, length), data, initialPos, length, useSubBuffer);
	}
	
	private static CompositeBytes.Readable create(byte[] data, int initialPos, int length) {
		CompositeBytes.Readable c = new CompositeBytes.Readable();
		for (int i = 0; i < length; i += 5) {
			int len = Math.min(5, length - i);
			ByteArray r = new ByteArray(data, initialPos + i, len);
			c.add(r);
		}
		return c;
	}
	
	@Test
	public void testConstructorArray() {
		CompositeBytes.Readable c = new CompositeBytes.Readable(
			new ByteArray(new byte[10]),
			new ByteArray(new byte[5]),
			new ByteArray(new byte[3])
		);
		Assert.assertEquals(18, c.remaining());
	}
	
	@Test
	public void testConstructorList() {
		CompositeBytes.Readable c = new CompositeBytes.Readable(Arrays.asList(
			new ByteArray(new byte[10]),
			new ByteArray(new byte[5]),
			new ByteArray(new byte[3])
		));
		Assert.assertEquals(18, c.remaining());
	}
	
}
