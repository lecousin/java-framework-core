package net.lecousin.framework.core.tests.concurrent.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
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
			public IAsync<Exception> consume(Integer data) {
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
			public IAsync<Exception> consume(Integer data) {
				if (data.intValue() != pos)
					return new Async<>(new Exception("Received " + data.intValue() + ", expected was " + pos));
				pos++;
				Async<Exception> result = new Async<>();
				testTask(() -> result.unblock())
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
			public IAsync<Exception> consume(Integer data) {
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
			public IAsync<Exception> consume(Integer data) {
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
	
	@Test
	public void testPush() throws Exception {
		AsyncConsumer<Integer, Exception> finalConsumer = new AsyncConsumer<Integer, Exception>() {
			private int pos = 0;
			@Override
			public IAsync<Exception> consume(Integer data) {
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
		List<Integer> toPush = new LinkedList<>();
		for (int i = 0; i < 1000; ++i)
			toPush.add(Integer.valueOf(i));
		buffered.push(toPush).blockThrow(0);
		buffered.consumeEnd(Integer.valueOf(1000)).blockThrow(0);
	}
	
	private void send(MutableInteger nbSent, AsyncConsumer<Integer, Exception> consumer, Async<Exception> done, int max) {
		do {
			if (nbSent.get() == max) {
				consumer.end().onDone(done);
				return;
			}
			IAsync<Exception> send = consumer.consume(Integer.valueOf(nbSent.get()));
			nbSent.inc();
			if (!send.isDone()) {
				send.thenStart("test", Task.Priority.NORMAL, () -> send(nbSent, consumer, done, max), done);
				return;
			} else if (send.hasError()) {
				done.error(send.getError());
				return;
			}
		} while (true);
	}
	
}
