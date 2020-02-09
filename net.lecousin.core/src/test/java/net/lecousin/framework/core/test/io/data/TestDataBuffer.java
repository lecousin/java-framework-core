package net.lecousin.framework.core.test.io.data;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.data.DataBuffer;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestDataBuffer<Impl extends DataBuffer, Data> extends LCCoreAbstractTest {

	protected TestDataBuffer(Impl buffer, int initialPos, int length, Data data) {
		this.buffer = buffer;
		this.initialPos = initialPos;
		this.length = length;
		this.data = data;
	}
	
	protected Impl buffer;
	protected int initialPos;
	protected int length;
	protected Data data;
	
	@SuppressWarnings("boxing")
	@Test
	public void basicTests() {
		Assert.assertEquals(length, buffer.length());
		Assert.assertEquals(0, buffer.position());
		Assert.assertEquals(length, buffer.remaining());
		Assert.assertEquals(length > 0, buffer.hasRemaining());
	}
	
	@Test
	public void testMove() {
		if (length > 0) {
			buffer.setPosition(1);
			Assert.assertEquals(1, buffer.position());
		}
		if (length > 1) {
			buffer.moveForward(1);
			Assert.assertEquals(2, buffer.position());
		}
		buffer.goToEnd();
		Assert.assertEquals(length, buffer.position());
	}
	
}
