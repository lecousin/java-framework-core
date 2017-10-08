package net.lecousin.framework.core.tests.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

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

public abstract class TestWritableWrapper extends TestIO.UsingTestData {
	
	protected TestWritableWrapper(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	protected abstract IO.Writable openWritableWrapper(IO.Writable output) throws Exception;
	
	@SuppressWarnings("resource")
	@Override
	protected IO getIOForCommonTests() throws Exception {
		File file = createFile();
		FileIO.WriteOnly fileIO = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		IO.Writable wrapper = openWritableWrapper(fileIO);
		return wrapper;
	}

	static File createFile() throws Exception {
		File tmp = File.createTempFile("test", "writable");
		tmp.deleteOnExit();
		return tmp;
	}
	
	static void checkFile(File file, byte[] testBuf, int nbBuf) throws Exception {
		@SuppressWarnings("resource")
		FileIO.ReadOnly io = new FileIO.ReadOnly(file, Task.PRIORITY_NORMAL);
		byte[] b = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			io.readFullySync(ByteBuffer.wrap(b));
			if (!ArrayUtil.equals(testBuf, b))
				throw new Exception("Invalid data at " + (i * testBuf.length));
		}
		io.close();
	}
	
	@Test
	public void testWriteBufferByBufferSync() throws Exception {
		File file = createFile();
		@SuppressWarnings("resource")
		FileIO.WriteOnly fileIO = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		IO.Writable wrapper = openWritableWrapper(fileIO);
		for (int i = 0; i < nbBuf; ++i)
			wrapper.writeSync(ByteBuffer.wrap(testBuf));
		wrapper.close();
		checkFile(file, testBuf, nbBuf);
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testWriteBufferByBufferAsync() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		File file = createFile();
		FileIO.WriteOnly fileIO = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		IO.Writable wrapper = openWritableWrapper(fileIO);
		MutableInteger i = new MutableInteger(0);
		Mutable<AsyncWork<Integer,IOException>> write = new Mutable<>(null);
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		Runnable listener = new Runnable() {
			@Override
			public void run() {
				if (write.get().hasError()) {
					sp.error(write.get().getError());
					return;
				}
				if (write.get().getResult().intValue() != testBuf.length) {
					sp.error(new Exception("Invalid write: returned " + write.get().getResult().intValue() + " on " + testBuf.length));
					return;
				}
				if (i.inc() == nbBuf) {
					sp.unblock();
					return;
				}
				write.set(wrapper.writeAsync(ByteBuffer.wrap(testBuf)));
				write.get().listenInline(this);
			}
		};
		write.set(wrapper.writeAsync(ByteBuffer.wrap(testBuf)));
		write.get().listenInline(listener);
		
		sp.block(0);
		if (sp.hasError()) throw sp.getError();
		wrapper.close();
		checkFile(file, testBuf, nbBuf);
	}	
	
}
