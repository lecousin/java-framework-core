package net.lecousin.framework.core.tests.io.data;

import java.nio.ByteBuffer;

import net.lecousin.framework.core.test.io.data.TestBytesReadable;
import net.lecousin.framework.io.data.ByteArray;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class TestByteArrayReadable extends TestBytesReadable {

	public TestByteArrayReadable(byte[] data, int initialPos, int length, boolean useSubBuffer) {
		super(initialPos == 0 && length == data.length ? new ByteArray(data) : new ByteArray(data, initialPos, length), data, initialPos, length, useSubBuffer);
	}
	
	@Test
	public void testDuplicate() {
		Assume.assumeTrue(length > 4);
		buffer.setPosition(length - 3);
		ByteArray d = ((ByteArray)buffer).duplicate();
		Assert.assertEquals(buffer.position(), d.position());
		Assert.assertEquals(buffer.length(), d.length());
		Assert.assertEquals(buffer.remaining(), d.remaining());
		d.get();
		Assert.assertEquals(buffer.position() + 1, d.position());
		Assert.assertEquals(buffer.length(), d.length());
		Assert.assertEquals(buffer.remaining() - 1, d.remaining());
	}
	
	@Test
	public void testFromByteBuffer() {
		ByteBuffer b = ByteBuffer.wrap(data, initialPos, length);
		b.position(b.position() + 1);
		ByteArray rb = ByteArray.fromByteBuffer(b);
		Assert.assertEquals(length - 1, rb.remaining());
		Assert.assertEquals(initialPos + 1, b.position());
		rb.get();
		rb.setPosition(b);
		Assert.assertEquals(initialPos + 2, b.position());
		
		b = ByteBuffer.allocateDirect(length);
		b.position(b.position() + 1);
		rb = ByteArray.fromByteBuffer(b);
		Assert.assertEquals(length - 1, rb.remaining());
		Assert.assertEquals(1, b.position());
		rb.get();
		rb.setPosition(b);
		Assert.assertEquals(2, b.position());
	}
	
}
