package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.mutable.Mutable;
import net.lecousin.framework.mutable.MutableInteger;

public abstract class TestWritableToFile extends TestIO.UsingTestData {

	protected TestWritableToFile(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}

	protected abstract IO.Writable createWritableFromFile(File file) throws IOException;
	
	@SuppressWarnings("unused")
	protected void flush(IO.Writable io) throws Exception {
	}
	
	protected static File createFile() throws IOException {
		File file = File.createTempFile("test", "writable");
		file.deleteOnExit();
		return file;
	}
	
	public static void checkFile(File file, byte[] testBuf, int nbBuf) throws Exception {
		@SuppressWarnings("resource")
		FileIO.ReadOnly io = new FileIO.ReadOnly(file, Task.PRIORITY_NORMAL);
		byte[] b = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			io.readFullySync(ByteBuffer.wrap(b));
			if (!ArrayUtil.equals(testBuf, b))
				throw new Exception("Invalid data at " + (i * testBuf.length)
					+ ", read is:\r\n" + new String(b) + "\r\nexpected was: " + new String(testBuf) + "\r\ntemp file is: " + file.getAbsolutePath());
		}
		io.close();
	}
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return createWritableFromFile(createFile());
	}
	
	@Override
	protected void basicTests(IO io) throws Exception {
		super.basicTests(io);
		((IO.Writable)io).canStartWriting();
	}

	@Test(timeout=120000)
	public void testWriteBufferByBufferSync() throws Exception {
		File file = createFile();
		IO.Writable io = createWritableFromFile(file);
		if (io instanceof IO.PositionKnown)
			Assert.assertEquals(0, ((IO.PositionKnown)io).getPosition());
		for (int i = 0; i < nbBuf; ++i) {
			ByteBuffer b = ByteBuffer.wrap(testBuf);
			io.writeSync(b);
			Assert.assertEquals(0, b.remaining());
			if (io instanceof IO.PositionKnown)
				Assert.assertEquals(testBuf.length * (i + 1), ((IO.PositionKnown)io).getPosition());
		}
		flush(io);
		io.close();
		checkFile(file, testBuf, nbBuf);
	}
	
	@Test(timeout=120000)
	public void testWriteBufferByBufferAsync() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		File file = createFile();
		IO.Writable io = createWritableFromFile(file);
		MutableInteger i = new MutableInteger(0);
		Mutable<AsyncWork<Integer,IOException>> write = new Mutable<>(null);
		Mutable<ByteBuffer> buf = new Mutable<>(null);
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		Runnable listener = new Runnable() {
			@Override
			public void run() {
				do {
					if (write.get().hasError()) {
						sp.error(write.get().getError());
						return;
					}
					if (write.get().getResult().intValue() != testBuf.length) {
						sp.error(new Exception("Invalid write: returned " + write.get().getResult().intValue() + " on " + testBuf.length));
						return;
					}
					if (buf.get().remaining() > 0) {
						sp.error(new Exception("Write operation did not fully consumed buffer"));
						return;
					}
					if (io instanceof IO.PositionKnown)
						try { Assert.assertEquals(testBuf.length * (i.get() + 1), ((IO.PositionKnown)io).getPosition()); }
						catch (Throwable e) {
							sp.error(IO.error(e));
							return;
						}
					if (i.inc() == nbBuf) {
						sp.unblock();
						return;
					}
					buf.set(ByteBuffer.wrap(testBuf));
					write.set(io.writeAsync(buf.get()));
				} while (write.get().isUnblocked());
				write.get().listenInline(this);
			}
		};
		buf.set(ByteBuffer.wrap(testBuf));
		write.set(io.writeAsync(buf.get()));
		write.get().listenInline(listener);
		
		sp.blockThrow(0);
		flush(io);
		io.close();
		checkFile(file, testBuf, nbBuf);
	}	

}
