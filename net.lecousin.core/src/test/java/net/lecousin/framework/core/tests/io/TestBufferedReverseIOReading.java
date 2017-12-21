package net.lecousin.framework.core.tests.io;

import java.io.EOFException;
import java.io.File;
import java.util.Collection;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.core.test.io.TestIO;
import net.lecousin.framework.core.test.io.TestReadableByteStream;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.FileIO.ReadOnly;
import net.lecousin.framework.io.IO.ReadableByteStream;
import net.lecousin.framework.io.buffering.BufferedReverseIOReading;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestBufferedReverseIOReading extends TestReadableByteStream {

	@Parameters(name = "nbBuf = {2}")
	public static Collection<Object[]> parameters() {
		return TestIO.UsingGeneratedTestFiles.generateTestCases();
	}
	
	public TestBufferedReverseIOReading(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	@Override
	protected ReadableByteStream createReadableByteStreamFromFile(ReadOnly file, long fileSize) throws Exception {
		return new BufferedReverseIOReading(openFile(), 512);
	}
	
	@Override
	protected IO getIOForCommonTests() {
		return new BufferedReverseIOReading(openFile(), 512);
	}
	
	@SuppressWarnings({ "resource" })
	@Test
	public void testReverseIO() throws Exception {
		BufferedReverseIOReading rio = new BufferedReverseIOReading(openFile(), 512);
		// backward
		goBackward(rio);
		// forward
		for (int i = 0; i < nbBuf; ++i) {
			for (int j = 0; j < testBuf.length; ++j) {
				int c;
				try { c = rio.read(); }
				catch (Throwable t) {
					throw new Exception("Error at " + (i * testBuf.length + j), t);
				}
				if (c != (testBuf[j] & 0xFF))
					throw new Exception("Invalid character " + c + " (" + (char)c + ") at " + (i * testBuf.length + j));
			}
		}
		rio.close();
	}
	
	private void goBackward(BufferedReverseIOReading rio) {
		for (int i = nbBuf - 1; i >= 0; --i) {
			for (int j = testBuf.length - 1; j >= 0; --j) {
				int c;
				try { c = rio.readReverse(); }
				catch (Throwable t) {
					throw new AssertionError("Error at " + (i * testBuf.length + j), t);
				}
				if (c != (testBuf[j] & 0xFF))
					throw new AssertionError("Invalid character " + c + " (" + (char)c + ") at " + (i * testBuf.length + j));
			}
		}
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testReadFully() throws Exception {
		BufferedReverseIOReading rio = new BufferedReverseIOReading(openFile(), 512);
		goBackward(rio);
		byte[] b = new byte[testBuf.length];
		for (int i = 0; i < nbBuf; ++i) {
			int nb = rio.readFully(b);
			if (nb != testBuf.length)
				throw new AssertionError("Only " + nb + " byte(s) read at buffer " + i);
			if (!ArrayUtil.equals(testBuf, b))
				throw new AssertionError("Invalid read at buffer " + i);
			int c;
			try { c = rio.readReverse(); }
			catch (Throwable t) {
				throw new AssertionError("Error reading previous character after buffer " + i, t);
			}
			if (c != (testBuf[testBuf.length - 1] & 0xFF))
				throw new AssertionError("Invalid character read using readReverse after a readFully of buffer index " + i);
			c = rio.read();
			if (c != (testBuf[testBuf.length - 1] & 0xFF))
				throw new AssertionError("Invalid character read using read (forward) after a readFully of buffer index " + i);
		}
		rio.close();
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testSkip() throws Exception {
		BufferedReverseIOReading rio = new BufferedReverseIOReading(openFile(), 512);
		goBackward(rio);
		for (int i = 0; i < nbBuf; ++i) {
			int j = testBuf.length / 10;
			Assert.assertEquals(j, rio.skip(j));
			byte b;
			try { b = rio.readByte(); }
			catch (EOFException e) {
				throw new AssertionError("EOF reached at " + (i * testBuf.length + j) + ", file size is " + (nbBuf * testBuf.length));
			}
			if (b != testBuf[j])
				throw new AssertionError("Invalid byte " + b + " read at " + (i * testBuf.length + j)
					+ ", expected was " + testBuf[j]);
			Assert.assertEquals(testBuf.length - j - 2, rio.skip(testBuf.length - j - 2));
			j = testBuf.length - 1;
			try { b = rio.readByte(); }
			catch (EOFException e) {
				throw new AssertionError("EOF reached at " + (i * testBuf.length + j) + ", file size is " + (nbBuf * testBuf.length));
			}
			if (b != testBuf[j])
				throw new AssertionError("Invalid byte " + b + " read at " + (i * testBuf.length + j)
					+ ", expected was " + testBuf[j]);
		}
		rio.close();
	}
	
}
