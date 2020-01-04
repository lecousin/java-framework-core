package net.lecousin.framework.core.test.io.bit;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.bit.BitIO;
import net.lecousin.framework.io.buffering.ByteArrayIO;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public abstract class TestBitIOReadable extends LCCoreAbstractTest {

	@Parameters
	public static Collection<Object[]> parameters() {
		ArrayList<Object[]> list = new ArrayList<>(1);
		list.add(new Object[] {
			new byte[] { (byte)0x56, (byte)0xF1, (byte)0x00, (byte)0x04, (byte)0xD7 },
			new boolean[] {
				false, true, true, false,
				true, false, true, false,
				true, false, false, false,
				true, true, true, true,
				false, false, false, false,
				false, false, false, false,
				false, false, true, false,
				false, false, false, false,
				true, true, true, false,
				true, false, true, true
			}
		});
		return list;
	}
	
	public TestBitIOReadable(byte[] bytes, boolean[] bits) {
		this.bytes = bytes;
		this.bits = bits;
	}
	
	protected byte[] bytes;
	protected boolean[] bits;
	
	protected abstract BitIO.Readable createLittleEndian(IO.Readable.Buffered io);
	
	protected abstract BitIO.Readable createBigEndian(IO.Readable.Buffered io);
	
	@Test
	public void testBitByBitLittleEndian() throws IOException {
		ByteArrayIO io = new ByteArrayIO(bytes, "test");
		BitIO.Readable bio = createLittleEndian(io);
		for (int i = 0; i < bits.length; ++i)
			Assert.assertTrue("bit " + i, bits[i] == bio.readBoolean());
		try {
			bio.readBoolean();
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
	}
	
	@Test
	public void testBitByBitBigEndian() throws IOException {
		ByteArrayIO io = new ByteArrayIO(bytes, "test");
		BitIO.Readable bio = createBigEndian(io);
		for (int i = 0; i < bytes.length; ++i) {
			for (int j = 0; j < 8; ++j)
				Assert.assertTrue("bit " + (i * 8 + j) + " expected to be " + bits[i * 8 + 7 - j], bits[i * 8 + 7 - j] == bio.readBoolean());
		}
		try {
			bio.readBoolean();
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
	}
	
	@Test
	public void testBitByBitAsyncLittleEndian() throws Exception {
		ByteArrayIO io = new ByteArrayIO(bytes, "test");
		BitIO.Readable bio = createLittleEndian(io);
		for (int i = 0; i < bits.length; ++i)
			Assert.assertTrue("bit " + i, bits[i] == bio.readBooleanAsync().blockResult(0).booleanValue());
		try {
			bio.readBooleanAsync().blockResult(0);
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
	}
	
	@Test
	public void testBitByBitAsyncBigEndian() throws Exception {
		ByteArrayIO io = new ByteArrayIO(bytes, "test");
		BitIO.Readable bio = createBigEndian(io);
		for (int i = 0; i < bytes.length; ++i) {
			for (int j = 0; j < 8; ++j)
				Assert.assertTrue("bit " + (i * 8 + j) + " expected to be " + bits[i * 8 + 7 - j], bits[i * 8 + 7 - j] == bio.readBooleanAsync().blockResult(0).booleanValue());
		}
		try {
			bio.readBooleanAsync().blockResult(0);
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
	}
	
	@Test
	public void testLittleEndianBy4Bits() throws IOException {
		testLittleEndianBy(4);
	}
	
	@Test
	public void testLittleEndianBy3Bits() throws IOException {
		testLittleEndianBy(3);
	}
	
	@Test
	public void testLittleEndianBy5Bits() throws IOException {
		testLittleEndianBy(5);
	}
	
	@Test
	public void testLittleEndianBy7Bits() throws IOException {
		testLittleEndianBy(7);
	}
	
	@Test
	public void testLittleEndianBy19Bits() throws IOException {
		testLittleEndianBy(19);
	}
	
	private void testLittleEndianBy(int nbBits) throws IOException {
		ByteArrayIO io = new ByteArrayIO(bytes, "test");
		BitIO.Readable bio = createLittleEndian(io);
		int bitsPos = 0;
		while (bitsPos + nbBits <= bits.length) {
			long expected = 0;
			for (int i = 0; i < nbBits; ++i) {
				expected <<= 1;
				expected |= bits[bitsPos + nbBits - 1 - i] ? 1 : 0;
			}
			long value = bio.readBits(nbBits);
			Assert.assertEquals("bits from " + bitsPos, expected, value);
			bitsPos += nbBits;
		}
		try {
			bio.readBits(nbBits);
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
		try {
			bio.readBits(nbBits);
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
	}
	
	@Test
	public void testLittleEndianAsyncBy4Bits() throws Exception {
		testLittleEndianAsyncBy(4);
	}
	
	@Test
	public void testLittleEndianAsyncBy3Bits() throws Exception {
		testLittleEndianAsyncBy(3);
	}
	
	@Test
	public void testLittleEndianAsyncBy5Bits() throws Exception {
		testLittleEndianAsyncBy(5);
	}
	
	@Test
	public void testLittleEndianAsyncBy7Bits() throws Exception {
		testLittleEndianAsyncBy(7);
	}
	
	@Test
	public void testLittleEndianAsyncBy19Bits() throws Exception {
		testLittleEndianAsyncBy(19);
	}
	
	private void testLittleEndianAsyncBy(int nbBits) throws Exception {
		ByteArrayIO io = new ByteArrayIO(bytes, "test");
		BitIO.Readable bio = createLittleEndian(io);
		int bitsPos = 0;
		while (bitsPos + nbBits <= bits.length) {
			long expected = 0;
			for (int i = 0; i < nbBits; ++i) {
				expected <<= 1;
				expected |= bits[bitsPos + nbBits - 1 - i] ? 1 : 0;
			}
			long value = bio.readBitsAsync(nbBits).blockResult(0).longValue();
			Assert.assertEquals("bits from " + bitsPos, expected, value);
			bitsPos += nbBits;
		}
		try {
			bio.readBitsAsync(nbBits).blockThrow(0);
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
		try {
			bio.readBitsAsync(nbBits).blockThrow(0);
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
	}
	
	@Test
	public void testBigEndianBy3Bits() throws IOException {
		testBigEndianBy(3);
	}
	
	@Test
	public void testBigEndianBy4Bits() throws IOException {
		testBigEndianBy(4);
	}
	
	@Test
	public void testBigEndianBy5Bits() throws IOException {
		testBigEndianBy(5);
	}
	
	@Test
	public void testBigEndianBy7Bits() throws IOException {
		testBigEndianBy(7);
	}
	
	@Test
	public void testBigEndianBy19Bits() throws IOException {
		testBigEndianBy(19);
	}
	
	private void testBigEndianBy(int nbBits) throws IOException {
		ByteArrayIO io = new ByteArrayIO(bytes, "test");
		BitIO.Readable bio = createBigEndian(io);
		int bitsPos = 0;
		while (bitsPos + nbBits <= bits.length) {
			long expected = 0;
			for (int i = 0; i < nbBits; ++i) {
				expected <<= 1;
				int o = (bitsPos + i) / 8;
				int b = (bitsPos + i) % 8;
				expected |= bits[o * 8 + 7 - b] ? 1 : 0;
			}
			long value = bio.readBits(nbBits);
			Assert.assertEquals("bits from " + bitsPos, expected, value);
			bitsPos += nbBits;
		}
		try {
			bio.readBits(nbBits);
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
		try {
			bio.readBits(nbBits);
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
	}
	
	@Test
	public void testBigEndianAsyncBy3Bits() throws Exception {
		testBigEndianAsyncBy(3);
	}
	
	@Test
	public void testBigEndianAsyncBy4Bits() throws Exception {
		testBigEndianAsyncBy(4);
	}
	
	@Test
	public void testBigEndianAsyncBy5Bits() throws Exception {
		testBigEndianAsyncBy(5);
	}
	
	@Test
	public void testBigEndianAsyncBy7Bits() throws Exception {
		testBigEndianAsyncBy(7);
	}
	
	@Test
	public void testBigEndianAsyncBy19Bits() throws Exception {
		testBigEndianAsyncBy(19);
	}
	
	private void testBigEndianAsyncBy(int nbBits) throws Exception {
		ByteArrayIO io = new ByteArrayIO(bytes, "test");
		BitIO.Readable bio = createBigEndian(io);
		int bitsPos = 0;
		while (bitsPos + nbBits <= bits.length) {
			long expected = 0;
			for (int i = 0; i < nbBits; ++i) {
				expected <<= 1;
				int o = (bitsPos + i) / 8;
				int b = (bitsPos + i) % 8;
				expected |= bits[o * 8 + 7 - b] ? 1 : 0;
			}
			long value = bio.readBitsAsync(nbBits).blockResult(0).longValue();
			Assert.assertEquals("bits from " + bitsPos, expected, value);
			bitsPos += nbBits;
		}
		try {
			bio.readBitsAsync(nbBits).blockThrow(0);
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
		try {
			bio.readBitsAsync(nbBits).blockThrow(0);
			throw new AssertionError("End of stream expected");
		} catch (EOFException e) {
			// ok
		}
	}

}
