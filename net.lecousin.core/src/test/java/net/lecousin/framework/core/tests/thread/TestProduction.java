package net.lecousin.framework.core.tests.thread;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Random;

import org.junit.Test;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.concurrent.util.production.simple.Consumer;
import net.lecousin.framework.concurrent.util.production.simple.Production;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IOWritePool;
import net.lecousin.framework.io.out2in.OutputToInputBuffers;
import net.lecousin.framework.io.util.IOReaderAsProducer;
import net.lecousin.framework.util.Pair;

public class TestProduction extends LCCoreAbstractTest {

	@SuppressWarnings("resource")
	@Test
	public void testProduction() {
		System.out.println("Start");
		MyChecker checker = new MyChecker();
		OutputToInputBuffers io = new OutputToInputBuffers(false, Task.PRIORITY_NORMAL);
		MyWriter writer = new MyWriter(io, checker);
		writer.start();
		IOReaderAsProducer producer = new IOReaderAsProducer(io, 50);
		MyConsumer consumer = new MyConsumer(checker);
		Production<ByteBuffer> production = new Production<>(producer, 3, consumer);
		production.start();
		JoinPoint<Exception> jp = new JoinPoint<>();
		jp.addToJoin(2);
		jp.start();
		writer.getOutput().listenInline(new Runnable() {
			@Override
			public void run() {
				System.out.println("Writer done");
				if (!writer.isSuccessful())
					jp.error(writer.getError());
				else
					jp.joined();
			}
		});
		production.getSyncOnFinished().listenInline(new Runnable() {
			@Override
			public void run() {
				System.out.println("Production done");
				if (!production.getSyncOnFinished().isSuccessful())
					jp.error(production.getSyncOnFinished().getError());
				else
					jp.joined();
			}
		});
		jp.block(0);
		System.out.println("All done");
		if (jp.hasError()) {
			jp.getError().printStackTrace(System.err);
		} else {
			System.out.println(" ************************ SUCCESS **********************");
		}
	}
	
	private static class MyChecker {
		private LinkedList<Pair<Integer,Byte>> expected = new LinkedList<>();
		private boolean end = false;
		public synchronized void write(int nb, int value) throws Exception {
			if (end)
				throw new Exception("End already called, we cannot write data anymore");
			expected.add(new Pair<>(Integer.valueOf(nb),Byte.valueOf((byte)value)));
		}
		public synchronized boolean read(ByteBuffer data) throws Exception {
			if (data.remaining() == 0) {
				System.out.println("0 byte received.");
				return false;
			}
			boolean res = false;
			do {
				if (expected.isEmpty())
					throw new Exception("Data received, but nothing expected");
				Pair<Integer,Byte> exp = expected.getFirst();
				if ((exp.getValue2().byteValue()&0xFF) >= 100)
					res = true;
				do {
					byte b = data.get();
					if (b != exp.getValue2().byteValue())
						throw new Exception("Byte "+b+" received, but "+exp.getValue2()+" expected "+exp.getValue1()+" times");
					int rem = exp.getValue1().intValue()-1;
					if (rem == 0) {
						expected.removeFirst();
						break;
					}
					exp.setValue1(Integer.valueOf(rem));
				} while (data.remaining() > 0);
			} while (data.remaining() > 0);
			return res;
		}
		public synchronized void end() throws Exception {
			if (!expected.isEmpty())
				throw new Exception("End called but still data is expected");
			if (end)
				throw new Exception("End already called");
			end = true;
		}
	}
	
	private class MyWriter extends Task.Cpu<Void,Exception> {
		public MyWriter(OutputToInputBuffers io, MyChecker checker) {
			super("Writer", Task.PRIORITY_NORMAL);
			this.checker = checker;
			this.io = io;
			pool = new IOWritePool(io);
		}
		private MyChecker checker;
		private OutputToInputBuffers io;
		private IOWritePool pool;
		@Override
		public Void run() throws Exception, CancelException {
			Random rand = new Random();
			for (int i = 0; i < 200; ++i) {
				int nbWrite = rand.nextBoolean() ? 20 + rand.nextInt(20) : 500+rand.nextInt(1500);
				checker.write(nbWrite, i);
				byte[] buf = new byte[nbWrite];
				for (int j = 0; j < nbWrite; ++j) buf[j] = (byte)i;
				ByteBuffer buffer = ByteBuffer.wrap(buf);
				System.out.println("Write "+nbWrite+" bytes: "+(byte)i);
				pool.write(buffer);
				if (i < 100)
					Thread.sleep(rand.nextInt(300));
			}
			pool.onDone().listenInline(new Runnable() {
				@Override
				public void run() {
					io.endOfData();
				}
			});
			return null;
		}
	}
	
	private static class MyConsumer implements Consumer<ByteBuffer> {
		public MyConsumer(MyChecker checker) {
			this.checker = checker;
			rand = new Random();
		}
		private MyChecker checker;
		private Random rand;
		@Override
		public AsyncWork<?, ? extends Exception> consume(ByteBuffer product) {
			System.out.println(product.remaining()+" bytes received");
			try {
				if (checker.read(product))
					Thread.sleep(rand.nextInt(25));
				return new AsyncWork<>(null,null);
			} catch (Exception e) {
				return new AsyncWork<>(null,e);
			}
		}
		@Override
		public AsyncWork<?, ? extends Exception> endOfProduction() {
			try {
				checker.end();
				return new AsyncWork<>(null,null);
			} catch (Exception e) {
				return new AsyncWork<>(null,e);
			}
		}
		@Override
		public void error(Exception error) {
			error.printStackTrace(System.err);
		}
		@Override
		public void cancel(CancelException event) {
			event.printStackTrace(System.err);
		}
	}

}
