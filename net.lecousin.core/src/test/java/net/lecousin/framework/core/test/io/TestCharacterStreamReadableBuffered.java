package net.lecousin.framework.core.test.io;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.data.Chars;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.text.CharArrayStringBuffer;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public abstract class TestCharacterStreamReadableBuffered extends TestIO.UsingGeneratedTestFiles {

	protected TestCharacterStreamReadableBuffered(File testFile, byte[] testBuf, int nbBuf) {
		super(testFile, testBuf, nbBuf);
	}
	
	protected abstract ICharacterStream.Readable.Buffered openStream(IO.Readable io) throws Exception;
	
	@Override
	protected IO getIOForCommonTests() {
		Assume.assumeTrue(nbBuf < 5000);
		return openFile();
	}


	@Test
	public void testIOError() throws Exception {
		ICharacterStream.Readable.Buffered s = openStream(new TestIOError.IOError1());
		try {
			s.read();
			throw new AssertionError();
		} catch (IOException e) {
			// ok
		}
		try {
			if (s.readAsync() == -2) {
				s.canStartReading().block(10000);
				s.readAsync();
			}
			throw new AssertionError();
		} catch (IOException e) {
			// ok
		}
		try {
			s.readNextBufferAsync().blockResult(10000);
			throw new AssertionError();
		} catch (IOException e) {
			// ok
		}
		try {
			s.readNextBuffer();
			throw new AssertionError();
		} catch (IOException e) {
			// ok
		}
		try {
			s.readUntil('m', new CharArrayStringBuffer());
			throw new AssertionError();
		} catch (IOException e) {
			// ok
		}
		try {
			s.readUntilAsync('m', new CharArrayStringBuffer()).blockResult(10000);
			throw new AssertionError();
		} catch (IOException e) {
			// ok
		}
		s.close();
	}
	
	@Test
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
	
	@Test
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

	@Test
	public void testNextBufferAsync() throws Exception {
		ICharacterStream.Readable.Buffered s = openStream(openFile());
		int iBuf = 0;
		int iChar = 0;
		while (iBuf < nbBuf) {
			Chars.Readable str = s.readNextBufferAsync().blockResult(0);
			Assert.assertNotNull(str);
			while (str.hasRemaining()) {
				Assert.assertEquals(testBuf[iChar] & 0xFF, str.get());
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
		Chars.Readable str = s.readNextBufferAsync().blockResult(0);
		Assert.assertNotNull(str);
		Assert.assertEquals(1, str.remaining());
		Assert.assertEquals('z', str.get());
		Assert.assertNull(s.readNextBufferAsync().blockResult(0));
		s.close();
	}
	
	@Test
	public void testNextBuffer() throws Exception {
		ICharacterStream.Readable.Buffered s = openStream(openFile());
		int iBuf = 0;
		int iChar = 0;
		while (iBuf < nbBuf) {
			Chars.Readable str = s.readNextBuffer();
			Assert.assertNotNull(str);
			while (str.hasRemaining()) {
				Assert.assertEquals(testBuf[iChar] & 0xFF, str.get());
				if (++iChar == testBuf.length) {
					iChar = 0;
					iBuf++;
				}
			}
		}
		Assert.assertEquals(nbBuf, iBuf);
		Assert.assertEquals(0, iChar);
		Assert.assertNull(s.readNextBuffer());
		s.back('z');
		Chars.Readable str = s.readNextBuffer();
		Assert.assertNotNull(str);
		Assert.assertEquals(1, str.remaining());
		Assert.assertEquals('z', str.get());
		Assert.assertNull(s.readNextBuffer());
		s.close();
	}
	
	@Test
	public void testReadUntil() throws Exception {
		ICharacterStream.Readable.Buffered s = openStream(openFile());
		int iBuf = 0;
		int iChar = 0;
		while (iBuf < nbBuf) {
			CharArrayStringBuffer str = new CharArrayStringBuffer();
			char endChar = (char)testBuf[(iBuf + 17 + iChar) % testBuf.length];
			boolean found = s.readUntil(endChar, str);
			int i = 0;
			boolean foundExpected = false;
			do {
				if (testBuf[iChar] == endChar) {
					Assert.assertEquals(i, str.length());
					if (++iChar == testBuf.length) {
						iChar = 0;
						iBuf++;
					}
					foundExpected = true;
					break;
				}
				Assert.assertTrue(str.length() > i);
				Assert.assertEquals(testBuf[iChar], str.charAt(i++));
				if (++iChar == testBuf.length) {
					iChar = 0;
					iBuf++;
				}
			} while (iBuf < nbBuf);
			Assert.assertTrue(foundExpected == found);
		}
		CharArrayStringBuffer str = new CharArrayStringBuffer();
		boolean found = s.readUntil('m', str);
		Assert.assertFalse(found);
		Assert.assertEquals(0, str.length());
		s.close();
		if (nbBuf <= 100) {
			s = openStream(openFile());
			str = new CharArrayStringBuffer();
			found = s.readUntil('$', str);
			s.close();
			Assert.assertFalse(found);
			Assert.assertEquals(nbBuf * testBuf.length, str.length());
		}
	}
	
	@Test
	public void testReadUntilAsync() throws Exception {
		ICharacterStream.Readable.Buffered s = openStream(openFile());
		int iBuf = 0;
		int iChar = 0;
		while (iBuf < nbBuf) {
			CharArrayStringBuffer str = new CharArrayStringBuffer();
			char endChar = (char)testBuf[(iBuf + (testBuf.length * 2 / 3 - 1) + iChar) % testBuf.length];
			boolean found = s.readUntilAsync(endChar, str).blockResult(10000).booleanValue();
			int i = 0;
			boolean foundExpected = false;
			do {
				if (testBuf[iChar] == endChar) {
					Assert.assertEquals(i, str.length());
					if (++iChar == testBuf.length) {
						iChar = 0;
						iBuf++;
					}
					foundExpected = true;
					break;
				}
				Assert.assertTrue(str.length() > i);
				Assert.assertEquals(testBuf[iChar], str.charAt(i++));
				if (++iChar == testBuf.length) {
					iChar = 0;
					iBuf++;
				}
			} while (iBuf < nbBuf);
			Assert.assertTrue(foundExpected == found);
		}
		CharArrayStringBuffer str = new CharArrayStringBuffer();
		boolean found = s.readUntilAsync('m', str).blockResult(10000).booleanValue();
		Assert.assertFalse(found);
		Assert.assertEquals(0, str.length());
		s.close();
		if (nbBuf <= 100) {
			s = openStream(openFile());
			str = new CharArrayStringBuffer();
			found = s.readUntilAsync('$', str).blockResult(10000).booleanValue();
			s.close();
			Assert.assertFalse(found);
			Assert.assertEquals(nbBuf * testBuf.length, str.length());
		}
	}
}
