package net.lecousin.framework.core.tests.concurrent.util;

import net.lecousin.framework.concurrent.Executable;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.concurrent.util.AsyncProducer;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Test;

public class TestProducerToConsumer extends LCCoreAbstractTest {

	@Test
	public void testIntegerProductionAndIntegerConsumption() throws Exception {
		new IntegerProducer(100).toConsumer(new IntegerConsumer(100), "test production of integers", Task.Priority.NORMAL).blockThrow(0);
	}

	@Test
	public void testShortProductionAndIntegerConsumption() throws Exception {
		new ShortProducer(100).toConsumer(s -> new AsyncSupplier<>(Integer.valueOf(s.intValue()), null), new IntegerConsumer(100), "test production of short and consumption of int", Task.Priority.NORMAL).blockThrow(0);
	}
	
	@Test
	public void testEmptyProducer() throws Exception {
		new AsyncProducer.Empty<Integer, Exception>().toConsumer(new IntegerConsumer(0), "test empty production of integers", Task.Priority.NORMAL).blockThrow(0);
	}
	
	@Test
	public void testSingleIntegerProducer() throws Exception {
		new AsyncProducer.SingleData<Integer, Exception>(Integer.valueOf(0)).toConsumer(new IntegerConsumer(1), "test production of 1 integer", Task.Priority.NORMAL).blockThrow(0);
	}
	
	private static class IntegerProducer implements AsyncProducer<Integer, Exception> {
		
		private int counter = 0;
		private int max;
		
		public IntegerProducer(int max) {
			this.max = max;
		}
		
		@Override
		public AsyncSupplier<Integer, Exception> produce() {
			return Task.cpu("produce an integer", Task.Priority.NORMAL, new Executable.FromSupplierThrows<>(
				() -> counter == max ? null : Integer.valueOf(counter++)
			)).start().getOutput();
		}
		
	}
	
	private static class ShortProducer implements AsyncProducer<Short, Exception> {
		
		private short counter = 0;
		private short max;
		
		public ShortProducer(int max) {
			this.max = (short)max;
		}
		
		@Override
		public AsyncSupplier<Short, Exception> produce() {
			return Task.cpu("produce a short", Task.Priority.NORMAL, new Executable.FromSupplierThrows<>(
				() -> counter == max ? null : Short.valueOf(counter++)
			)).start().getOutput();
		}
		
	}
	
	private static class IntegerConsumer implements AsyncConsumer<Integer, Exception> {
		
		private int counter = 0;
		private int max;
		
		public IntegerConsumer(int max) {
			this.max = max;
		}
		
		@Override
		public IAsync<Exception> consume(Integer data) {
			if (data == null)
				return new Async<>(new NullPointerException());
			if (data.intValue() != counter)
				return new Async<>(new Exception("Received " + data + " but " + counter + " was expected"));
			if (data.intValue() >= max)
				return new Async<>(new Exception("Received " + data + ", maximum expected is " + (max - 1)));
			counter++;
			return new Async<>(true);
		}
		
		@Override
		public IAsync<Exception> end() {
			if (counter == max)
				return new Async<>(true);
			return new Async<>(new Exception("End reached but counter is " + counter));
		}
		
		@Override
		public void error(Exception error) {
		}
		
	}
	
}
