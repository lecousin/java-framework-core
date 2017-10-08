package net.lecousin.framework.core.tests.io;

import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;

public abstract class TestReadWriteBuffered extends TestIO.UsingTestData {

	protected TestReadWriteBuffered(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	protected abstract 
	<T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.Readable.Buffered & IO.Writable.Buffered> 
	T openReadWriteBuffered() throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return openReadWriteBuffered();
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.Readable.Buffered & IO.Writable.Buffered> void testWriteThenReadBuffered() throws Exception {
		T io = openReadWriteBuffered();
		for (int i = 0; i < nbBuf; ++i) {
			io.write(testBuf);
		}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		byte[] b = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			int nb = io.readFully(b);
			if (nb != testBuf.length)
				throw new Exception("Read only "+nb+" bytes at "+i);
			if (!ArrayUtil.equals(b, testBuf)) {
				System.out.println("Invalid read:");
				System.out.println(new String(b, 0, nb));
				throw new Exception("Invalid read at "+i);
			}
		}
		if (io.read() != -1)
			throw new Exception("More bytes than expected can be read");
		io.close();
	}
	
}
