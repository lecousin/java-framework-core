package net.lecousin.framework.core.tests.io;

import java.nio.ByteBuffer;

import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IO.Seekable.SeekType;

public abstract class TestReadWrite extends TestIO.UsingTestData {

	protected TestReadWrite(byte[] testBuf, int nbBuf) {
		super(testBuf, nbBuf);
	}
	
	protected abstract <T extends IO.Readable.Seekable & IO.Writable.Seekable> T openReadWrite() throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return openReadWrite();
	}

	@SuppressWarnings({ "resource" })
	@Test
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable> void testWriteThenReadFullySync() throws Exception {
		T io = openReadWrite();
		ByteBuffer buf = ByteBuffer.wrap(testBuf);
		for (int i = 0; i < nbBuf; ++i) {
			buf.position(0);
			int nb = io.writeSync(buf);
			if (nb != testBuf.length)
				throw new Exception("Write only "+nb+" bytes");
		}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		byte[] b = new byte[testBuf.length];
		buf = ByteBuffer.wrap(b);
		for (int i = 0; i < nbBuf; ++i) {
			buf.clear();
			int nb = io.readFullySync(buf);
			if (nb != testBuf.length)
				throw new Exception("Read only "+nb+" bytes at "+i);
			if (!ArrayUtil.equals(b, testBuf)) {
				System.out.println("Invalid read:");
				System.out.println(new String(b, 0, nb));
				throw new Exception("Invalid read at "+i);
			}
		}
		buf.clear();
		if (io.readSync(buf) > 0)
			throw new Exception("More bytes than expected can be read");
		io.close();
	}

	// TODO test with some seeking operations, sync and async, etc...
	// TODO flush
	
}
