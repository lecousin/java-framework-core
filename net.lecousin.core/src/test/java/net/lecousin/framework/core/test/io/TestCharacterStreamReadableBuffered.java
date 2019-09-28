package net.lecousin.framework.core.test.io;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.util.UnprotectedString;

public abstract class TestCharacterStreamReadableBuffered extends TestIO.UsingGeneratedTestFiles {

	protected TestCharacterStreamReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	protected abstract ICharacterStream.Readable.Buffered openStream(IO.Readable io) throws Exception;
	
	@Override
	protected IO getIOForCommonTests() {
		return openFile();
	}

	@Test(timeout=120000)
	public void testCharByChar() throws Exception {
		ICharacterStream.Readable.Buffered s = openStream(openFile());
		for (int i = 0; i < nbBuf; ++i) {
			for (int j = 0; j < testBuf.length; ++j) {
				char c;
				try { c = s.read(); }
				catch (IOException e) {
					throw new Exception("Read character error at " + (i*testBuf.length+j) + " in file " + testFile.getAbsolutePath(), e);
				}
				if (c != (char)(testBuf[j]&0xFF))
					throw new Exception("Invalid character "+c+" at "+(i*testBuf.length+j));
			}
		}
		try {
			s.read();
			throw new AssertionError("Can read after the end of stream");
		} catch (EOFException e) {}
		s.back('w');
		Assert.assertEquals('w', s.read());
		s.back('z');
		char[] buf = new char[20];
		Assert.assertTrue(s.readSync(buf, 0, 0) <= 0);
		Assert.assertEquals(1, s.readSync(buf, 0, 10));
		Assert.assertEquals('z', buf[0]);
		Assert.assertTrue(s.readSync(buf, 0, 0) <= 0);
		s.back('a');
		Assert.assertEquals(1, s.readSync(buf, 0, 1));
		Assert.assertEquals('a', buf[0]);
		s.close();
	}
	
	@Test(timeout=120000)
	public void testCharByCharAsync() throws Exception {
		ICharacterStream.Readable.Buffered s = openStream(openFile());
		Async<Exception> sp = new Async<>();
		continueReadAsync(s, 0, 0, sp);
		sp.blockThrow(0);
		s.back('z');
		char[] buf = new char[20];
		Assert.assertEquals(1, s.readAsync(buf, 0, 20).blockResult(0).intValue());
		Assert.assertEquals('z', buf[0]);
		s.close();
	}
	
	private void continueReadAsync(ICharacterStream.Readable.Buffered s, int iBuf, int iChar, Async<Exception> sp) throws Exception {
		while (iBuf < nbBuf) {
			if ((iBuf + iChar) % 13 == 3) {
				s.back('b');
				Assert.assertEquals('b', s.readAsync());
			}
			int c = s.readAsync();
			if (c == -1)
				throw new Exception("Unexpected end at " + (iBuf * testBuf.length + iChar));
			if (c == -2) {
				int i = iBuf;
				int j = iChar;
				s.canStartReading().thenStart(new Task.Cpu.FromRunnable("readAsync", Task.PRIORITY_NORMAL, () -> {
					try {
						continueReadAsync(s, i, j, sp);
					} catch (Exception e) {
						sp.error(e);
					}
				}), true);
				return;
			}
			if (c != (char)(testBuf[iChar]&0xFF))
				throw new Exception("Invalid character " + c + " at "+ (iBuf * testBuf.length + iChar));
			if (++iChar >= testBuf.length) {
				iBuf++;
				iChar = 0;
			}
		}
		sp.unblock();
	}

	@Test(timeout=120000)
	public void testNextBufferAsync() throws Exception {
		ICharacterStream.Readable.Buffered s = openStream(openFile());
		int iBuf = 0;
		int iChar = 0;
		while (iBuf < nbBuf) {
			UnprotectedString str = s.readNextBufferAsync().blockResult(0);
			Assert.assertNotNull(str);
			char[] chars = str.charArray();
			int len = str.length();
			for (int i = str.charArrayStart(), nb = 0; nb < len; ++i, ++nb) {
				Assert.assertEquals(testBuf[iChar] & 0xFF, chars[i]);
				if (++iChar == testBuf.length) {
					iChar = 0;
					iBuf++;
				}
			}
		}
		Assert.assertEquals(nbBuf, iBuf);
		Assert.assertEquals(0, iChar);
		Assert.assertNull(s.readNextBufferAsync().blockResult(0));
		s.back('z');
		UnprotectedString str = s.readNextBufferAsync().blockResult(0);
		Assert.assertNotNull(str);
		Assert.assertEquals(1, str.length());
		Assert.assertEquals('z', str.charAt(0));
		Assert.assertNull(s.readNextBufferAsync().blockResult(0));
		s.close();
	}	
}
