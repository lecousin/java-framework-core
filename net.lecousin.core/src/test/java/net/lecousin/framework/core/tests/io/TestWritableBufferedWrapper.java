package net.lecousin.framework.core.tests.io;

import java.io.File;

import org.junit.Test;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;

public abstract class TestWritableBufferedWrapper extends TestIO.UsingTestData {

	protected TestWritableBufferedWrapper(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	protected abstract IO.Writable.Buffered openWritableBufferedWrapper(IO.Writable output) throws Exception;
	
	@SuppressWarnings("resource")
	@Override
	protected IO getIOForCommonTests() throws Exception {
		File file = TestWritableWrapper.createFile();
		FileIO.WriteOnly fileIO = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		IO.Writable.Buffered wrapper = openWritableBufferedWrapper(fileIO);
		return wrapper;
	}

	@Test
	public void testWriteBufferByBufferInBuffered() throws Exception {
		File file = TestWritableWrapper.createFile();
		@SuppressWarnings("resource")
		FileIO.WriteOnly fileIO = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		IO.Writable.Buffered wrapper = openWritableBufferedWrapper(fileIO);
		for (int i = 0; i < nbBuf; ++i)
			wrapper.write(testBuf, 0, testBuf.length);
		wrapper.flush().block(0);
		wrapper.close();
		TestWritableWrapper.checkFile(file, testBuf, nbBuf);
	}

	@Test
	public void testWriteByteByByteInBuffered() throws Exception {
		File file = TestWritableWrapper.createFile();
		@SuppressWarnings("resource")
		FileIO.WriteOnly fileIO = new FileIO.WriteOnly(file, Task.PRIORITY_NORMAL);
		IO.Writable.Buffered wrapper = openWritableBufferedWrapper(fileIO);
		for (int i = 0; i < nbBuf; ++i)
			for (int j = 0; j < testBuf.length; ++j)
				wrapper.write(testBuf[j]);
		wrapper.flush().block(0);
		wrapper.close();
		TestWritableWrapper.checkFile(file, testBuf, nbBuf);
	}
	
}
