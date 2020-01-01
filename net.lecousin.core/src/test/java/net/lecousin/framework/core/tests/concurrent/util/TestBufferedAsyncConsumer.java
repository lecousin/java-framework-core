package net.lecousin.framework.core.tests.concurrent.util;

import java.util.Random;
import java.util.function.Consumer;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.util.AsyncConsumer;
import net.lecousin.framework.concurrent.util.BufferedAsyncConsumer;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.mutable.MutableInteger;

import org.junit.Test;

public class TestBufferedAsyncConsumer extends LCCoreAbstractTest {

	@Test
	public void testFast() throws Exception {
		AsyncConsumer<Integer, Exception> finalConsumer = new AsyncConsumer<Integer, Exception>() {
			private int pos = 0;
			@Override
			public IAsync<Exception> consume(Integer data, Consumer<Integer> onDataRelease) {
				if (data.intValue() != pos)
					return new Async<>(new Exception("Received " + data.intValue() + ", expected was " + pos));
				pos++;
				return new Async<>(true);
			}
			
			@Override
			public IAsync<Exception> end() {
				return new Async<>(true);
			}
			
			@Override
			public void error(Exception error) {
			}
		};
		BufferedAsyncConsumer<Integer, Exception> buffered = new BufferedAsyncConsumer<>(20, finalConsumer);
		MutableInteger nbSent = new MutableInteger(0);
		Async<Exception> done = new Async<>();
		send(nbSent, buffered, done, 2500);
		done.blockThrow(0);
	}

	@Test
	public void testSlow() throws Exception {
		AsyncConsumer<Integer, Exception> finalConsumer = new AsyncConsumer<Integer, Exception>() {
			private int pos = 0;
			private Random rand = new Random();
			@Override
			public IAsync<Exception> consume(Integer data, Consumer<Integer> onDataRelease) {
				if (data.intValue() != pos)
					return new Async<>(new Exception("Received " + data.intValue() + ", expected was " + pos));
				pos++;
				Async<Exception> result = new Async<>();
				new Task.Cpu.FromRunnable("test", Task.PRIORITY_NORMAL, () -> result.unblock())
				.executeIn(rand.nextInt(100)).start();
				return result;
			}
			
			@Override
			public IAsync<Exception> end() {
				return new Async<>(true);
			}
			
			@Override
			public void error(Exception error) {
			}
		};
		BufferedAsyncConsumer<Integer, Exception> buffered = new BufferedAsyncConsumer<>(20, finalConsumer);
		MutableInteger nbSent = new MutableInteger(0);
		Async<Exception> done = new Async<>();
		send(nbSent, buffered, done, 100);
		done.blockThrow(0);
	}

	@Test
	public void testErrorConsuming() throws Exception {
		AsyncConsumer<Integer, Exception> finalConsumer = new AsyncConsumer<Integer, Exception>() {
			private int pos = 0;
			@Override
			public IAsync<Exception> consume(Integer data, Consumer<Integer> onDataRelease) {
				if (pos == 127)
					return new Async<>(new IllegalArgumentException());
				if (pos > 127)
					return new Async<>(new Exception("Must not receive data after error"));
				if (data.intValue() != pos)
					return new Async<>(new Exception("Received " + data.intValue() + ", expected was " + pos));
				pos++;
				return new Async<>(true);
			}
			
			@Override
			public IAsync<Exception> end() {
				return new Async<>(true);
			}
			
			@Override
			public void error(Exception error) {
			}
		};
		BufferedAsyncConsumer<Integer, Exception> buffered = new BufferedAsyncConsumer<>(20, finalConsumer);
		MutableInteger nbSent = new MutableInteger(0);
		Async<Exception> done = new Async<>();
		send(nbSent, buffered, done, 2500);
		try {
			done.blockThrow(0);
		} catch (IllegalArgumentException e) {
			// ok
		}
	}

	@Test
	public void testErrorAtTheEnd() throws Exception {
		AsyncConsumer<Integer, Exception> finalConsumer = new AsyncConsumer<Integer, Exception>() {
			private int pos = 0;
			@Override
			public IAsync<Exception> consume(Integer data, Consumer<Integer> onDataRelease) {
				if (data.intValue() != pos)
					return new Async<>(new Exception("Received " + data.intValue() + ", expected was " + pos));
				pos++;
				return new Async<>(true);
			}
			
			@Override
			public IAsync<Exception> end() {
				return new Async<>(new IllegalArgumentException());
			}
			
			@Override
			public void error(Exception error) {
			}
		};
		BufferedAsyncConsumer<Integer, Exception> buffered = new BufferedAsyncConsumer<>(20, finalConsumer);
		MutableInteger nbSent = new MutableInteger(0);
		Async<Exception> done = new Async<>();
		send(nbSent, buffered, done, 500);
		try {
			done.blockThrow(0);
		} catch (IllegalArgumentException e) {
			// ok
		}
	}
	
	private void send(MutableInteger nbSent, AsyncConsumer<Integer, Exception> consumer, Async<Exception> done, int max) {
		do {
			if (nbSent.get() == max) {
				consumer.end().onDone(done);
				return;
			}
			IAsync<Exception> send = consumer.consume(Integer.valueOf(nbSent.get()), nbSent.get() % 2 == 0 ? null : b -> {});
			nbSent.inc();
			if (!send.isDone()) {
				send.thenStart("test", Task.PRIORITY_NORMAL, () -> send(nbSent, consumer, done, max), done);
				return;
			} else if (send.hasError()) {
				done.error(send.getError());
				return;
			}
		} while (true);
	}
	
}
