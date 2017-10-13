package net.lecousin.framework.core.tests.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableInteger;

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
		if (((IO.OutputToInput)io).canStartReading().isUnblocked())
			throw new AssertionError("OutputToInput.canStartReading returned an unblocked synchronization point, but nothing can be read yet because nothing has been written yet!");
	}

	@SuppressWarnings("resource")
	@Test
	public void testWriteSyncReadHalfSync() throws IOException {
		IO.OutputToInput o2i = createOutputToInput();
		int nbWrite = 0;
		int nbRead = 0;
		byte[] b = new byte[testBuf.length];
		while (nbWrite < nbBuf) {
			o2i.writeSync(ByteBuffer.wrap(testBuf));
			nbWrite++;
			readHalfSync(o2i, (nbRead % 2) == 0, b);
			nbRead++;
		}
		o2i.endOfData();
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
	
	@Test
	public void testWriteAsyncReadHalfAsync() throws IOException {
		IO.OutputToInput o2i = createOutputToInput();
		SynchronizationPoint<IOException> spWrite = new SynchronizationPoint<>();
		new Task.Cpu<Void, NoException>("Launch write async to OutputToInput", Task.PRIORITY_IMPORTANT) {
			@Override
			public Void run() {
				if (nbBuf == 0) {
					o2i.endOfData();
					spWrite.unblock();
					return null;
				}
				Mutable<AsyncWork<Integer, IOException>> write = new Mutable<>(o2i.writeAsync(ByteBuffer.wrap(testBuf)));
				MutableInteger nbWrite = new MutableInteger(1);
				write.get().listenInline(new Runnable() {
					@Override
					public void run() {
						if (write.get().hasError()) { spWrite.error(write.get().getError()); return; }
						if (nbWrite.get() == nbBuf) {
							o2i.endOfData();
							spWrite.unblock();
							return;
						}
						write.set(o2i.writeAsync(ByteBuffer.wrap(testBuf)));
						nbWrite.inc();
						write.get().listenInline(this);
					}
				});
				return null;
			}
		}.start();
		SynchronizationPoint<IOException> spRead = new SynchronizationPoint<>();
		new Task.Cpu<Void, NoException>("Launch read half async on OutputToInput", Task.PRIORITY_IMPORTANT) {
			@Override
			public Void run() {
				MutableInteger nbRead = new MutableInteger(0);
				Mutable<AsyncWork<Integer, IOException>> read = new Mutable<>(null);
				byte[] buffer = new byte[testBuf.length];
				Runnable onRead = new Runnable() {
					@Override
					public void run() {
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
						read.get().listenInline(this);
					}
				};
				onRead.run();
				return null;
			}
		}.start();
		spWrite.block(0);
		if (spWrite.hasError()) throw spWrite.getError();
		spRead.block(0);
		if (spRead.hasError()) throw spRead.getError();
		o2i.closeAsync();
	}
	
}
