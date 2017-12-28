package net.lecousin.framework.core.tests.io.util;

import java.nio.ByteBuffer;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.util.Buffers;

import org.junit.Assert;
import org.junit.Test;

public class TestBuffers extends LCCoreAbstractTest {

	@Test
	public void test() {
		Buffers b = new Buffers(10, 3);
		ByteBuffer b1 = b.getBuffer();
		Assert.assertEquals(10, b1.remaining());
		b1.position(3);
		b.freeBuffer(b1);
		ByteBuffer bb = b.getBuffer();
		Assert.assertEquals(b1, bb);
		Assert.assertEquals(10, bb.remaining());
		b1.position(3);
		ByteBuffer b2 = b.getBuffer();
		Assert.assertEquals(10, b2.remaining());
		b2.position(4);
		ByteBuffer b3 = b.getBuffer();
		Assert.assertEquals(10, b3.remaining());
		b3.position(5);
		b.freeBuffer(b2);
		b.freeBuffer(b3);
		bb = b.getBuffer();
		Assert.assertEquals(b2, bb);
		Assert.assertEquals(10, bb.remaining());
		bb = b.getBuffer();
		Assert.assertEquals(b3, bb);
		Assert.assertEquals(10, bb.remaining());
	}
	
}
