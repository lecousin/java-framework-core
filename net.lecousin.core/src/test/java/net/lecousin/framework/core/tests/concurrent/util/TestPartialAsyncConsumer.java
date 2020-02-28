package net.lecousin.framework.core.tests.concurrent.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.util.PartialAsyncConsumer;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.data.ByteArray;

import org.junit.Assert;
import org.junit.Test;

public class TestPartialAsyncConsumer extends LCCoreAbstractTest {

	private static class Consumer1 implements PartialAsyncConsumer<ByteBuffer, Exception> {
		private boolean done = false;
		
		@Override
		public AsyncSupplier<Boolean, Exception> consume(ByteBuffer data) {
			done = true;
			if (data.get() == 1)
				return new AsyncSupplier<>(Boolean.TRUE, null);
			return new AsyncSupplier<>(null, new Exception());
		}

		@Override
		public boolean isExpectingData() {
			return !done;
		}
	}
	
	private static class Consumer2 implements PartialAsyncConsumer<ByteArray, Exception> {

		private boolean done = false;
		
		@Override
		public AsyncSupplier<Boolean, Exception> consume(ByteArray data) {
			done = true;
			if (data.get() == 2)
				return new AsyncSupplier<>(Boolean.TRUE, null);
			return new AsyncSupplier<>(null, new Exception());
		}

		@Override
		public boolean isExpectingData() {
			return !done;
		}
		
	}
	
	private static class Consumer3 implements PartialAsyncConsumer<ByteBuffer, IOException> {

		private int pos = 0;
		
		@Override
		public AsyncSupplier<Boolean, IOException> consume(ByteBuffer data) {
			if (pos == 0) {
				pos++;
				if (data.get() == 3)
					return new AsyncSupplier<>(Boolean.FALSE, null);
				return new AsyncSupplier<>(null, new IOException());
			}
			pos++;
			if (data.get() == 4)
				return new AsyncSupplier<>(Boolean.TRUE, null);
			return new AsyncSupplier<>(null, new IOException());
		}

		@Override
		public boolean isExpectingData() {
			return pos < 2;
		}
		
	}
	
	@Test
	public void testQueue() throws Exception {
		Consumer1 c1 = new Consumer1();
		Consumer2 c2 = new Consumer2();
		PartialAsyncConsumer.ConsumerQueue<ByteBuffer, Exception> queue = new PartialAsyncConsumer.ConsumerQueue<>(
			c1,
			c2.convert(ByteArray::fromByteBuffer, ByteArray::setPosition, e -> e)
		);
		ByteBuffer b = ByteBuffer.wrap(new byte[] { 1, 2, 3 });
		queue.consume(b).blockThrow(0);
		Assert.assertEquals(2, b.position());
		
		c1 = new Consumer1();
		c2 = new Consumer2();
		Consumer3 c3 = new Consumer3();
		
		Queue<PartialAsyncConsumer<ByteBuffer, Exception>> consumers = new LinkedList<>();
		consumers.add(c1);
		consumers.add(c2.convert(ByteArray::fromByteBuffer, ByteArray::setPosition, e -> e));
		consumers.add(c3.convertError(e -> e));
		queue = new PartialAsyncConsumer.ConsumerQueue<>(consumers);
		b = ByteBuffer.wrap(new byte[] { 1, 2, 3 });
		queue.consume(b).blockThrow(0);
		Assert.assertEquals(3, b.position());
		b = ByteBuffer.wrap(new byte[] { 4, 5, 6 });
		queue.consume(b).blockThrow(0);
		Assert.assertEquals(1, b.position());
	}
	
}
