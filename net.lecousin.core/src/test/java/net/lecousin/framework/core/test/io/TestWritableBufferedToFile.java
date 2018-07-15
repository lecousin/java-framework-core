package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import net.lecousin.framework.io.IO;

public abstract class TestWritableBufferedToFile extends TestIO.UsingTestData {

	protected TestWritableBufferedToFile(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	protected abstract IO.Writable.Buffered createWritableBufferedFromFile(File file) throws IOException;

	@SuppressWarnings("unused")
	protected void flush(IO.Writable.Buffered io) throws Exception {
	}
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return createWritableBufferedFromFile(TestWritableToFile.createFile());
	}
	
	/** Can be overridden in case of a SubIO when checking file content. */
	public long getFileOffset() {
		return 0;
	}
	
	@Test(timeout=120000)
	public void testWriteBufferByBufferInBuffered() throws Exception {
		File file = TestWritableWrapper.createFile();
		IO.Writable.Buffered io = createWritableBufferedFromFile(file);
		for (int i = 0; i < nbBuf; ++i)
			io.write(testBuf, 0, testBuf.length);
		io.flush().blockThrow(0);
		flush(io);
		io.close();
		TestWritableToFile.checkFile(file, testBuf, nbBuf, getFileOffset());
	}

	@Test(timeout=120000)
	public void testWriteByteByByteInBuffered() throws Exception {
		File file = TestWritableWrapper.createFile();
		IO.Writable.Buffered io = createWritableBufferedFromFile(file);
		for (int i = 0; i < nbBuf; ++i)
			for (int j = 0; j < testBuf.length; ++j)
				io.write(testBuf[j]);
		io.flush().blockThrow(0);
		flush(io);
		io.close();
		TestWritableToFile.checkFile(file, testBuf, nbBuf, getFileOffset());
	}
	
}
