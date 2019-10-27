package net.lecousin.framework.core.test.io;

import java.io.IOException;

import org.junit.Test;

import net.lecousin.framework.io.IO;

public abstract class TestWritableBuffered extends TestWritable {

	protected TestWritableBuffered(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	protected abstract IO.Writable.Buffered createWritableBuffered() throws IOException;
	
	@Override
	protected IO.Writable createWritable() throws IOException {
		return createWritableBuffered();
	}

	@Test
	public void testWriteBufferByBufferInBuffered() throws Exception {
		IO.Writable.Buffered io = createWritableBuffered();
		for (int i = 0; i < nbBuf; ++i)
			io.write(testBuf, 0, testBuf.length);
		io.flush().blockThrow(0);
		flush(io);
		io.close();
		check();
	}

	@Test
	public void testWriteByteByByteInBuffered() throws Exception {
		IO.Writable.Buffered io = createWritableBuffered();
		for (int i = 0; i < nbBuf; ++i)
			for (int j = 0; j < testBuf.length; ++j)
				io.write(testBuf[j]);
		io.flush().blockThrow(0);
		flush(io);
		io.close();
		check();
	}
	
}
