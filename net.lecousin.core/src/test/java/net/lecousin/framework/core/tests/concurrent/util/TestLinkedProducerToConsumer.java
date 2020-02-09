package net.lecousin.framework.core.tests.concurrent.util;

import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.concurrent.util.AsyncProducer;
import net.lecousin.framework.concurrent.util.LinkedAsyncProducer;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Test;

public class TestLinkedProducerToConsumer extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		new LinkedAsyncProducer<>(
			new IntegerProducer(0, 15),
			new IntegerProducer(16, 48),
			new IntegerProducer(49, 100)
		).toConsumer(new IntegerConsumer(100), "test production of integers", Task.PRIORITY_NORMAL).blockThrow(0);
	}

	private static class IntegerProducer implements AsyncProducer<Integer, Exception> {
		
		private int pos = 0;
		private int max;
		
		public IntegerProducer(int start, int max) {
			this.pos = start;
			this.max = max;
		}
		
		@Override
		public AsyncSupplier<Integer, Exception> produce() {
			return new Task.Cpu.FromSupplierThrows<Integer, Exception>("produce an integer", Task.PRIORITY_NORMAL,
				() -> pos > max ? null : Integer.valueOf(pos++)
			).start().getOutput();
		}
		
	}
	
	private static class IntegerConsumer implements AsyncConsumer<Integer, Exception> {
		
		private int counter = 0;
		private int max;
		
		public IntegerConsumer(int max) {
			this.max = max;
		}
		
		@Override
		public IAsync<Exception> consume(Integer data, Consumer<Integer> onDataRelease) {
			if (data == null)
				return new Async<>(new NullPointerException());
			if (data.intValue() != counter)
				return new Async<>(new Exception("Received " + data + " but " + counter + " was expected"));
			if (data.intValue() > max)
				return new Async<>(new Exception("Received " + data + ", maximum expected is " + max));
			counter++;
			return new Async<>(true);
		}
		
		@Override
		public IAsync<Exception> end() {
			if (counter == max + 1)
				return new Async<>(true);
			return new Async<>(new Exception("End reached but counter is " + counter));
		}
		
		@Override
		public void error(Exception error) {
		}
		
	}
	
}
