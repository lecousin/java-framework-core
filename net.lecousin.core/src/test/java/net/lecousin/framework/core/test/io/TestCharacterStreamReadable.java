package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestCharacterStreamReadable extends TestIO.UsingGeneratedTestFiles {

	protected TestCharacterStreamReadable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	protected abstract ICharacterStream.Readable openStream(IO.Readable io) throws Exception;
	
	@Override
	protected IO getIOForCommonTests() {
		return openFile();
	}
	
	@Override
	protected void basicTests(IO io) throws Exception {
		super.basicTests(io);
		ICharacterStream.Readable stream = openStream(openFile());
		stream.getEncoding();
		byte p = io.getPriority();
		io.setPriority(Task.PRIORITY_LOW);
		Assert.assertEquals(Task.PRIORITY_LOW, io.getPriority());
		io.setPriority(p);
		Assert.assertEquals(p, io.getPriority());
		stream.close();
	}
	
	@Test(timeout=30000)
	public void basicStreamTests() throws Exception {
		ICharacterStream.Readable s = openStream(openFile());
		s.setPriority(Task.PRIORITY_IMPORTANT);
		s.close();
	}
	
	@SuppressWarnings({ "resource" })
	@Test(timeout=120000)
	public void testBufferByBufferFully() throws Exception {
		ICharacterStream.Readable s = openStream(openFile());
		char[] buf = new char[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			int nb = s.readFullySync(buf, 0, buf.length);
			if (nb != testBuf.length)
				throw new AssertionError("" + nb + " characters read at buffer " + i + ", expected is " + testBuf.length);
			for (int j = 0; j < testBuf.length; ++j)
				if (buf[j] != (testBuf[j] & 0xFF))
					throw new AssertionError("Invalid character " + buf[j] + " at " + (i * testBuf.length + j));
			if (i < nbBuf - 1)
				Assert.assertFalse(s.endReached());
		}
		Assert.assertTrue(s.readFullySync(buf, 0, buf.length) <= 0);
		Assert.assertTrue(s.endReached());
		s.close();
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testBufferByBuffer() throws Exception {
		ICharacterStream.Readable s = openStream(openFile());
		char[] buf = new char[testBuf.length * 3 - testBuf.length / 10];
		int pos = 0;
		while (pos < nbBuf * testBuf.length) {
			int nb = s.readSync(buf, 0, buf.length);
			if (nb <= 0)
				throw new AssertionError("End of stream reached after " + pos + " characters, expected was "
					+ (nbBuf * testBuf.length));
			for (int i = 0; i < nb; ++i)
				Assert.assertEquals("Invalid character at " + (pos + i), (char)testBuf[(pos + i) % testBuf.length], buf[i]);
			pos += nb;
			if (pos < nbBuf * testBuf.length)
				Assert.assertFalse(s.endReached());
		}
		Assert.assertTrue(s.readFullySync(buf, 0, buf.length) <= 0);
		Assert.assertTrue(s.readSync(buf, 0, buf.length) <= 0);
		Assert.assertTrue(s.endReached());
		s.close();
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testBufferByBufferAsync() throws Exception {
		ICharacterStream.Readable s = openStream(openFile());
		char[] buf = new char[testBuf.length * 3 - testBuf.length / 10];
		int pos = 0;
		while (pos < nbBuf * testBuf.length) {
			AsyncWork<Integer, IOException> read = s.readAsync(buf, 0, buf.length);
			int nb = read.blockResult(0).intValue();
			if (nb <= 0)
				throw new AssertionError("End of stream reached after " + pos + " characters, expected was "
					+ (nbBuf * testBuf.length));
			for (int i = 0; i < nb; ++i)
				Assert.assertEquals("Invalid character at " + (pos + i), (char)testBuf[(pos + i) % testBuf.length], buf[i]);
			pos += nb;
			if (pos < nbBuf * testBuf.length)
				Assert.assertFalse(s.endReached());
		}
		Assert.assertTrue(s.readFullySync(buf, 0, buf.length) <= 0);
		Assert.assertTrue(s.readAsync(buf, 0, buf.length).blockResult(0).intValue() <= 0);
		Assert.assertTrue(s.endReached());
		s.close();
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testBufferByBufferFullyAsync() throws Exception {
		ICharacterStream.Readable s = openStream(openFile());
		char[] buf = new char[testBuf.length * 3 - testBuf.length / 10];
		int pos = 0;
		while (pos < nbBuf * testBuf.length) {
			AsyncWork<Integer, IOException> read = s.readFullyAsync(buf, 0, buf.length);
			int nb = read.blockResult(0).intValue();
			if (nb <= 0)
				throw new AssertionError("End of stream reached after " + pos + " characters, expected was "
					+ (nbBuf * testBuf.length));
			for (int i = 0; i < nb; ++i)
				Assert.assertEquals("Invalid character at " + (pos + i), (char)testBuf[(pos + i) % testBuf.length], buf[i]);
			pos += nb;
			if (pos < nbBuf * testBuf.length) {
				Assert.assertFalse(s.endReached());
				if (nb != buf.length)
					throw new Exception("readFullyAsync returned " + nb + " on " + buf.length);
			}
		}
		Assert.assertTrue(s.readFullySync(buf, 0, buf.length) <= 0);
		Assert.assertTrue(s.readAsync(buf, 0, buf.length).blockResult(0).intValue() <= 0);
		Assert.assertTrue(s.endReached());
		s.close();
	}

}
