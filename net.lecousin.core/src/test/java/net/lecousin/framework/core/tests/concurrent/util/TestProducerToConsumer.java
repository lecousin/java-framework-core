package net.lecousin.framework.core.tests.concurrent.util;

import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.concurrent.util.AsyncProducer;
import net.lecousin.framework.core.test.LCCoreAbstractTest;

import org.junit.Test;

public class TestProducerToConsumer extends LCCoreAbstractTest {

	@Test
	public void test() throws Exception {
		new IntegerProducer().toConsumer(new IntegerConsumer(), "test production of integers", Task.PRIORITY_NORMAL).blockThrow(0);
	}
	
	private static class IntegerProducer implements AsyncProducer<Integer, Exception> {
		
		private int counter = 0;
		
		@Override
		public AsyncSupplier<Integer, Exception> produce() {
			return new Task.Cpu.FromSupplierThrows<Integer, Exception>("produce an integer", Task.PRIORITY_NORMAL,
				() -> counter == 100 ? null : Integer.valueOf(counter++)
			).start().getOutput();
		}
		
	}
	
	private static class IntegerConsumer implements AsyncConsumer<Integer, Exception> {
		
		private int counter = 0;
		
		@Override
		public IAsync<Exception> consume(Integer data, Consumer<Integer> onDataRelease) {
			if (data == null)
				return new Async<>(new NullPointerException());
			if (data.intValue() != counter)
				return new Async<>(new Exception("Received " + data + " but " + counter + " was expected"));
			if (data.intValue() >= 100)
				return new Async<>(new Exception("Received " + data + ", maximum expected is 99"));
			counter++;
			return new Async<>(true);
		}
		
		@Override
		public IAsync<Exception> end() {
			if (counter == 100)
				return new Async<>(true);
			return new Async<>(new Exception("End reached but counter is " + counter));
		}
		
		@Override
		public void error(Exception error) {
		}
		
	}
	
}
