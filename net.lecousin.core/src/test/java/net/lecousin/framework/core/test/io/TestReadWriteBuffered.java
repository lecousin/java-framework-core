package net.lecousin.framework.core.test.io;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import org.junit.Assert;
import org.junit.Assume;
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
		Assume.assumeTrue(nbBuf < 5000);
		return openReadWriteBuffered();
	}
	
	@SuppressWarnings({ "resource" })
	@Test(timeout=120000)
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.Readable.Buffered & IO.Writable.Buffered>
	void testWriteThenReadBuffered() throws Exception {
		T io = openReadWriteBuffered();
		for (int i = 0; i < nbBuf; ++i) {
			io.write(testBuf);
		}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		byte[] b = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			int nb = io.readFully(b);
			if (nb != testBuf.length)
				throw new Exception("Read only " + nb + " bytes at " + i);
			if (!ArrayUtil.equals(b, testBuf))
				throw new Exception("Invalid read at " + i
					+ ", read is:\r\n" + new String(b) + "\r\nexpected was:\r\n" + new String(testBuf));
		}
		if (io.read() != -1)
			throw new Exception("More bytes than expected can be read");
		io.close();
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public <T extends IO.Readable.Seekable & IO.Writable.Seekable & IO.Readable.Buffered & IO.Writable.Buffered>
	void testWriteThenReadBufferedWithSkipSync() throws Exception {
		Assume.assumeTrue(nbBuf > 0);
		T io = openReadWriteBuffered();
		Assert.assertEquals(0, io.getPosition());
		io.canStartWriting();
		// make the file have its final size to be able to use SEEK_END
		io.writeSync(nbBuf * testBuf.length - 1, ByteBuffer.wrap(testBuf, 0, 1));
		Assert.assertEquals(0, io.getPosition());
		io.canStartWriting();
		int pos = 0;
		// write randomly using skip or skipSync
		LinkedList<Integer> todo = new LinkedList<>();
		for (int i = 0; i < nbBuf; ++i) todo.add(Integer.valueOf(i));
		while (!todo.isEmpty()) {
			int bufIndex = todo.remove(rand.nextInt(todo.size())).intValue();
			if ((bufIndex % 2) == 0)
				io.skip(bufIndex * testBuf.length - pos);
			else
				io.skipSync(bufIndex * testBuf.length - pos);
			Assert.assertEquals(bufIndex * testBuf.length, io.getPosition());
			io.write(testBuf);
			pos = (bufIndex + 1) * testBuf.length;
			Assert.assertEquals(pos, io.getPosition());
		}
		// read randomly using skip or skipSync
		todo = new LinkedList<>();
		for (int i = 0; i < nbBuf; ++i) todo.add(Integer.valueOf(i));
		byte[] b = new byte[testBuf.length];
		while (!todo.isEmpty()) {
			int bufIndex = todo.remove(rand.nextInt(todo.size())).intValue();
			if ((bufIndex % 2) == 0)
				io.skip(bufIndex * testBuf.length - pos);
			else
				io.skipSync(bufIndex * testBuf.length - pos);
			Assert.assertEquals(bufIndex * testBuf.length, io.getPosition());
			int nb = io.readFully(b);
			if (nb != testBuf.length)
				throw new AssertionError("Only " + nb + " byte(s) read at buffer " + bufIndex);
			if (!ArrayUtil.equals(testBuf, b))
				throw new AssertionError("Invalid read at buffer " + bufIndex
					+ ", read is:\r\n" + new String(b) + "\r\nexpected was:\r\n" + new String(testBuf));
			pos = (bufIndex + 1) * testBuf.length;
			Assert.assertEquals(pos, io.getPosition());
		}
		io.close();
	}
	
}
