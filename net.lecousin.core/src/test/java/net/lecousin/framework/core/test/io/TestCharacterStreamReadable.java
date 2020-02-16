package net.lecousin.framework.core.test.io;

import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public abstract class TestCharacterStreamReadable extends TestIO.UsingGeneratedTestFiles {

	protected TestCharacterStreamReadable(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	protected abstract ICharacterStream.Readable openStream(IO.Readable io) throws Exception;
	
	@Override
	protected IO getIOForCommonTests() {
		Assume.assumeTrue(nbBuf < 5000);
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
	
	@Test
	public void basicStreamTests() throws Exception {
		ICharacterStream.Readable s = openStream(openFile());
		s.setPriority(Task.PRIORITY_IMPORTANT);
		s.close();
	}

	@Test
	public void testIOError() throws Exception {
		ICharacterStream.Readable s = openStream(new TestIOError.ReadableAlwaysError());
		try {
			s.readSync(new char[10], 0, 10);
			throw new AssertionError();
		} catch (IOException e) {
			// ok
		}
		try {
			s.readAsync(new char[10], 0, 10).blockResult(10000);
			throw new AssertionError();
		} catch (IOException e) {
			// ok
		}
		try {
			s.readFullySync(new char[10], 0, 10);
			throw new AssertionError();
		} catch (IOException e) {
			// ok
		}
		try {
			s.readFullyAsync(new char[10], 0, 10).blockResult(10000);
			throw new AssertionError();
		} catch (IOException e) {
			// ok
		}
		s.close();
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testBufferByBufferFully() throws Exception {
		ICharacterStream.Readable s = openStream(openFile());
		char[] buf = new char[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			int nb = s.readFullySync(buf, 0, buf.length);
			if (nb != testBuf.length)
				throw new AssertionError("" + nb + " characters read at buffer " + i + ", expected is " + testBuf.length);
			for (int j = 0; j < testBuf.length; ++j)
				Assert.assertEquals("Character at " + (i * testBuf.length + j), testBuf[j] & 0xFF, buf[j]);
			if (i < nbBuf - 1)
				Assert.assertFalse(s.endReached());
		}
		Assert.assertTrue(s.readFullySync(buf, 0, buf.length) <= 0);
		Assert.assertTrue(s.endReached());
		s.close();
	}
	
	@SuppressWarnings("resource")
	@Test
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
	@Test
	public void testBufferByBufferAsync() throws Exception {
		ICharacterStream.Readable s = openStream(openFile());
		char[] buf = new char[testBuf.length * 3 - testBuf.length / 10];
		int pos = 0;
		while (pos < nbBuf * testBuf.length) {
			AsyncSupplier<Integer, IOException> read = s.readAsync(buf, 0, buf.length);
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
	@Test
	public void testBufferByBufferFullyAsync() throws Exception {
		ICharacterStream.Readable s = openStream(openFile());
		char[] buf = new char[testBuf.length * 3 - testBuf.length / 10];
		int pos = 0;
		while (pos < nbBuf * testBuf.length) {
			AsyncSupplier<Integer, IOException> read = s.readFullyAsync(buf, 0, buf.length);
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
