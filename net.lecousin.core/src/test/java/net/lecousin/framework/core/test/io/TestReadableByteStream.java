package net.lecousin.framework.core.test.io;

import java.io.EOFException;
import java.io.File;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestReadableByteStream extends TestIO.UsingGeneratedTestFiles {
	
	protected TestReadableByteStream(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}

	protected abstract IO.ReadableByteStream createReadableByteStreamFromFile(FileIO.ReadOnly file, long fileSize) throws Exception;
	
	@Override
	protected IO getIOForCommonTests() throws Exception {
		return createReadableByteStreamFromFile(openFile(), getFileSize());
	}
	
	@Override
	protected void basicTests(IO io) throws Exception {
		super.basicTests(io);
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testReadableBufferedBufferByBufferFully() throws Exception {
		IO.ReadableByteStream ioBuf = createReadableByteStreamFromFile(openFile(), getFileSize());
		byte[] b = new byte[testBuf.length];
		int count = 0;
		do {
			int nb = ioBuf.readFully(b);
			if (nb <= 0) break;
			if (!ArrayUtil.equals(b, testBuf))
				throw new Exception("Invalid read after "+count);
			if (nb != testBuf.length)
				throw new Exception("Only "+nb+" bytes read after "+count);
			count++;
		} while (true);
		if (count != nbBuf)
			throw new Exception(""+count+" buffers read, expected is " + nbBuf);
		ioBuf.close();
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testReadableBufferedByteByByte() throws Exception {
		IO.ReadableByteStream io = createReadableByteStreamFromFile(openFile(), getFileSize());
		for (int i = 0; i < nbBuf; ++i) {
			if (nbBuf > 1000 && (i % 100) == 99) {
				// make the test faster
				int skipBuf = 50;
				if (i + skipBuf > nbBuf) skipBuf = nbBuf - i;
				io.skip(skipBuf * testBuf.length);
				i += skipBuf - 1;
				continue;
			}
			for (int j = 0; j < testBuf.length; ++j) {
				if ((i + j) % 2 == 0) {
					int b = io.read();
					if (b != (testBuf[j] & 0xFF))
						throw new Exception("Byte " + b + " read instead of " + (testBuf[j] & 0xFF) + " at offset " + (testBuf.length*i+j));
				} else {
					byte b = io.readByte();
					if (b != testBuf[j])
						throw new Exception("Byte " + b + " read instead of " + testBuf[j] + " at offset " + (testBuf.length*i+j));
				}
			}
		}
		if (io.read() != -1)
			throw new Exception("Remaining byte(s) at the end of the file");
		io.close();
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testReadableBufferedBufferByBuffer() throws Exception {
		long size = getFileSize();
		IO.ReadableByteStream ioBuf = createReadableByteStreamFromFile(openFile(), size);
		byte[] b = new byte[8192];
		long pos = 0;
		do {
			int nb = ioBuf.read(b, 0, 8192);
			if (nb <= 0) {
				if (pos != size)
					throw new Exception("Unexpected end of file at offset " + pos);
				break;
			}
			if (pos + nb > size)
				throw new Exception("" + nb + " byte(s) read at offset " + pos + ", but file size is " + size);
			
			int i = 0;
			while (i < nb) {
				int start = (int)((pos+i) % testBuf.length);
				int len = nb - i;
				if (len > testBuf.length - start) len = testBuf.length - start;
				for (int j = 0; j < len; ++j)
					if (b[i+j] != testBuf[start+j])
						throw new Exception("Invalid byte at offset " + (pos + i + start + j));
				i += len;
			}
			pos += nb;
		} while (true);
		ioBuf.close();
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testSkip() throws Exception {
		long size = getFileSize();
		IO.ReadableByteStream ioBuf = createReadableByteStreamFromFile(openFile(), size);
		for (int i = 0; i < nbBuf; ++i) {
			int j = testBuf.length / 10;
			Assert.assertEquals(j, ioBuf.skip(j));
			byte b;
			try { b = ioBuf.readByte(); }
			catch (EOFException e) {
				throw new AssertionError("EOF reached at " + (i * testBuf.length + j) + ", file size is " + (nbBuf * testBuf.length));
			}
			if (b != testBuf[j])
				throw new AssertionError("Invalid byte " + b + " read at " + (i * testBuf.length + j)
					+ ", expected was " + testBuf[j]);
			Assert.assertEquals(testBuf.length - j - 2, ioBuf.skip(testBuf.length - j - 2));
			j = testBuf.length - 1;
			try { b = ioBuf.readByte(); }
			catch (EOFException e) {
				throw new AssertionError("EOF reached at " + (i * testBuf.length + j) + ", file size is " + (nbBuf * testBuf.length));
			}
			if (b != testBuf[j])
				throw new AssertionError("Invalid byte " + b + " read at " + (i * testBuf.length + j)
					+ ", expected was " + testBuf[j]);
		}
		ioBuf.close();
	}
	
}
