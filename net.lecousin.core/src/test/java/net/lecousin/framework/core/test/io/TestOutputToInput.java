package net.lecousin.framework.core.test.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableInteger;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestOutputToInput extends TestIO.UsingTestData {

	public TestOutputToInput(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	protected abstract IO.OutputToInput createOutputToInput() throws IOException;
	
	@Override
	protected IO getIOForCommonTests() throws IOException {
		return createOutputToInput();
	}
	
	@Override
	protected void basicTests(IO io) throws Exception {
		super.basicTests(io);
		((IO.OutputToInput)io).canStartWriting();
		((IO.OutputToInput)io).canStartReading();
		((IO.OutputToInput)io).getAvailableDataSize();
	}
	
	@Test(timeout=15000)
	public void testError() throws Exception {
		IO.OutputToInput o2i = createOutputToInput();
		IOException err = new IOException();
		o2i.signalErrorBeforeEndOfData(err);
		try {
			o2i.readSync(ByteBuffer.allocate(10));
			throw new AssertionError("should throw an exception");
		} catch (Exception e) {
			//ok
		}
		try {
			o2i.readAsync(ByteBuffer.allocate(10)).blockResult(5000);
			throw new AssertionError("should throw an exception");
		} catch (Exception e) {
			//ok
		}
		try {
			o2i.skipSync(1024);
			throw new AssertionError("should throw an exception");
		} catch (Exception e) {
			//ok
		}
		try {
			o2i.skipAsync(1024).blockResult(5000);
			throw new AssertionError("should throw an exception");
		} catch (Exception e) {
			//ok
		}
	}

	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testWriteSyncReadHalfSync() throws Exception {
		IO.OutputToInput o2i = createOutputToInput();
		int nbWrite = 0;
		int nbRead = 0;
		byte[] b = new byte[testBuf.length];
		while (nbWrite < nbBuf) {
			ByteBuffer bu = ByteBuffer.wrap(testBuf);
			o2i.writeSync(bu);
			Assert.assertEquals(0, bu.remaining());
			nbWrite++;
			readHalfSync(o2i, (nbRead % 2) == 0, b);
			nbRead++;
		}
		Assert.assertFalse(o2i.isFullDataAvailable());
		o2i.endOfData();
		Assert.assertTrue(o2i.isFullDataAvailable());
		while (nbRead < nbBuf * 2) {
			readHalfSync(o2i, (nbRead % 2) == 0, b);
			nbRead++;
		}
		if (o2i.readSync(ByteBuffer.wrap(new byte[1])) == 1)
			throw new IOException("Data can be read after the end");
		o2i.close();
	}
	
	private void readHalfSync(IO.OutputToInput o2i, boolean firstHalf, byte[] buffer) throws IOException {
		int len = testBuf.length / 2;
		if (!firstHalf && (testBuf.length % 2) == 1) len++;
		int nb = o2i.readFullySync(ByteBuffer.wrap(buffer, 0, len));
		if (nb != len)
			throw new IOException("Only " + nb + " byte(s) read instead of " + len);
		boolean ok;
		if (firstHalf)
			ok = ArrayUtil.equals(testBuf, 0, buffer, 0, len);
		else
			ok = ArrayUtil.equals(testBuf, testBuf.length / 2, buffer, 0, len);
		if (!ok)
			throw new IOException("Read bytes do not match expected ones");
	}
	
	@Test(timeout=120000)
	public void testWriteAsyncReadHalfAsync() throws Exception {
		IO.OutputToInput o2i = createOutputToInput();
		Async<IOException> spWrite = new Async<>();
		new Task.Cpu<Void, NoException>("Launch write async to OutputToInput", Task.PRIORITY_IMPORTANT) {
			@Override
			public Void run() {
				if (nbBuf == 0) {
					o2i.endOfData();
					spWrite.unblock();
					return null;
				}
				Mutable<ByteBuffer> buf = new Mutable<>(ByteBuffer.wrap(testBuf));
				Mutable<AsyncSupplier<Integer, IOException>> write = new Mutable<>(o2i.writeAsync(buf.get()));
				MutableInteger nbWrite = new MutableInteger(1);
				write.get().onDone(new Runnable() {
					@Override
					public void run() {
						do {
							if (write.get().hasError()) { spWrite.error(write.get().getError()); return; }
							if (buf.get().remaining() > 0) { spWrite.error(new IOException("writeAsync did not consume the buffer")); return; }
							if (nbWrite.get() == nbBuf) {
								o2i.endOfData();
								spWrite.unblock();
								return;
							}
							buf.set(ByteBuffer.wrap(testBuf));
							write.set(o2i.writeAsync(buf.get()));
							nbWrite.inc();
						} while (write.get().isDone());
						write.get().onDone(this);
					}
				});
				return null;
			}
		}.start();
		Async<IOException> spRead = new Async<>();
		new Task.Cpu<Void, NoException>("Launch read half async on OutputToInput", Task.PRIORITY_IMPORTANT) {
			@Override
			public Void run() {
				MutableInteger nbRead = new MutableInteger(0);
				Mutable<AsyncSupplier<Integer, IOException>> read = new Mutable<>(null);
				byte[] buffer = new byte[testBuf.length];
				Runnable onRead = new Runnable() {
					@Override
					public void run() {
						do {
							if (read.get() != null) {
								if (read.get().hasError()) { spRead.error(read.get().getError()); return; }
								if (nbRead.get() > nbBuf * 2) {
									if (read.get().getResult().intValue() > 0) {
										spRead.error(new IOException(read.get().getResult().intValue() + " byte(s) read after the end"));
										return;
									}
									spRead.unblock();
									return;
								}
								int len = testBuf.length / 2;
								if ((nbRead.get() % 2) == 0 && (testBuf.length % 2) == 1) len++;
								if (read.get().getResult().intValue() != len) {
									spRead.error(new IOException("Only " + read.get().getResult() + " byte(s) read instead of " + len + " on the read number " + nbRead.get()));
									return;
								}
								boolean ok;
								if ((nbRead.get() % 2) != 0)
									ok = ArrayUtil.equals(testBuf, 0, buffer, 0, len);
								else
									ok = ArrayUtil.equals(testBuf, testBuf.length / 2, buffer, 0, len);
								if (!ok) {
									spRead.error(new IOException("Read bytes do not match expected ones on read number " + nbRead.get()));
									return;
								}
							}
							int len = testBuf.length / 2;
							if ((nbRead.get() % 2) != 0 && (testBuf.length % 2) != 0) len++;
							read.set(o2i.readFullyAsync(ByteBuffer.wrap(buffer, 0, len)));
							nbRead.inc();
						} while (read.get().isDone());
						read.get().onDone(this);
					}
				};
				onRead.run();
				return null;
			}
		}.start();
		spWrite.blockThrow(0);
		spRead.blockThrow(0);
		o2i.close();
	}
	
	@Test(timeout=120000)
	public void testSkipSync() throws Exception {
		IO.OutputToInput o2i = createOutputToInput();
		writeBg(o2i, nbBuf, testBuf);
		byte[] buf = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			if ((i % 2) == 0)
				Assert.assertEquals("Skip buffer " + i, testBuf.length, o2i.skipSync(testBuf.length));
			else {
				int nb = o2i.readFullySync(ByteBuffer.wrap(buf));
				Assert.assertEquals("Read buffer " + i, testBuf.length, nb);
				Assert.assertTrue("Buffer " + i, ArrayUtil.equals(testBuf, 0, buf, 0, testBuf.length));
			}
		}
		Assert.assertEquals(-1, o2i.readSync(ByteBuffer.wrap(buf)));
		o2i.close();
	}
	
	@Test(timeout=120000)
	public void testSkipAsync() throws Exception {
		IO.OutputToInput o2i = createOutputToInput();
		writeBg(o2i, nbBuf, testBuf);
		byte[] buf = new byte[testBuf.length];
		MutableInteger i = new MutableInteger(0);
		Async<IOException> sp = new Async<>();
		AsyncSupplier<Long, IOException> op = o2i.skipAsync(testBuf.length);
		op.onDone(new Runnable() {
			@Override
			public void run() {
				IAsync<IOException> op;
				do {
					if (nbBuf == 0) {
						sp.unblock();
						return;
					}
					if ((i.get() % 2) == 1) {
						if (!ArrayUtil.equals(testBuf, 0, buf, 0, testBuf.length)) {
							sp.error(new IOException("Invalid read"));
							return;
						}
					}
					i.inc();
					if (i.get() == nbBuf) {
						sp.unblock();
						return;
					}
					if ((i.get() % 2) == 0) {
						op = o2i.skipAsync(testBuf.length);
					} else {
						op = o2i.readFullyAsync(ByteBuffer.wrap(buf));
					}
				} while (op.isSuccessful());
				op.onDone(this, sp);
			}
		}, sp);
		sp.blockThrow(0);
		o2i.close();
	}
	
	public static Async<IOException> writeBg(IO.OutputToInput o2i, int nbBuf, byte[] testBuf) {
		Async<IOException> spWrite = new Async<>();
		new Task.Cpu<Void, NoException>("Launch write async to OutputToInput", Task.PRIORITY_IMPORTANT) {
			@Override
			public Void run() {
				if (nbBuf == 0) {
					o2i.endOfData();
					spWrite.unblock();
					return null;
				}
				Mutable<AsyncSupplier<Integer, IOException>> write = new Mutable<>(o2i.writeAsync(ByteBuffer.wrap(testBuf)));
				MutableInteger nbWrite = new MutableInteger(1);
				write.get().onDone(new Runnable() {
					@Override
					public void run() {
						do {
							if (write.get().hasError()) { spWrite.error(write.get().getError()); return; }
							if (write.get().isCancelled()) { spWrite.cancel(write.get().getCancelEvent()); return; }
							if (nbWrite.get() == nbBuf) {
								o2i.endOfData();
								spWrite.unblock();
								return;
							}
							write.set(o2i.writeAsync(ByteBuffer.wrap(testBuf)));
							nbWrite.inc();
						} while (write.get().isDone());
						write.get().onDone(this);
					}
				});
				return null;
			}
		}.start();
		return spWrite;
	}
	
}
