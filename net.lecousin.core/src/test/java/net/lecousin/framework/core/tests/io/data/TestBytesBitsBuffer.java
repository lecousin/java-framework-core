package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.data.BytesBitsBuffer;
import net.lecousin.framework.io.data.ByteArray;

import org.junit.Assert;
import org.junit.Test;

public class TestBytesBitsBuffer extends LCCoreAbstractTest {

	private byte[] bytes = new byte[] { (byte)0x00, (byte)0xFF, (byte)0x85, (byte)0x1E, (byte)0xDA, (byte)0x11 };
	private boolean[] bits = new boolean[] {
		false, false, false, false, false, false, false, false,
		true, true, true, true, true, true, true, true,
		true, false, true, false, false, false, false, true,
		false, true, true, true, true, false, false, false,
		false, true, false, true, true, false, true, true,
		true, false, false, false, true, false, false, false
	};
	
	@Test
	public void testReadBigEndian() {
		testRead(true);
	}
	
	@Test
	public void testReadLittleEndian() {
		testRead(false);
	}
	
	@SuppressWarnings("boxing")
	private void testRead(boolean useBigEndian) {
		BytesBitsBuffer.Readable buf = useBigEndian ? new BytesBitsBuffer.Readable.BigEndian(new ByteArray(bytes)) : new BytesBitsBuffer.Readable.LittleEndian(new ByteArray(bytes));
		for (int i = 0; i < bits.length; ++i) {
			Assert.assertEquals(bits.length - i, buf.remaining());
			Assert.assertTrue(buf.hasRemaining());
			Assert.assertEquals(bits[(i / 8) * 8 + (useBigEndian ? 7 - (i % 8) : (i % 8))], buf.get());
		}
		Assert.assertEquals(0, buf.remaining());
		Assert.assertFalse(buf.hasRemaining());
		
		buf = useBigEndian ? new BytesBitsBuffer.Readable.BigEndian(new ByteArray(bytes)) : new BytesBitsBuffer.Readable.LittleEndian(new ByteArray(bytes));
		Assert.assertEquals(bytes.length * 8, buf.remaining());
		Assert.assertTrue(buf.hasRemaining());
		buf.alignToNextByte();
		Assert.assertEquals(bytes.length * 8, buf.remaining());
		Assert.assertTrue(buf.hasRemaining());
		for (int i = 0; i < bytes.length; ++i) {
			Assert.assertEquals((bytes.length - i) * 8, buf.remaining());
			Assert.assertTrue(buf.hasRemaining());
			Assert.assertEquals(bits[i * 8 + (useBigEndian ? 7 : 0)], buf.get());
			buf.alignToNextByte();
		}
		Assert.assertEquals(0, buf.remaining());
		Assert.assertFalse(buf.hasRemaining());
		buf.alignToNextByte();
		Assert.assertEquals(0, buf.remaining());
		Assert.assertFalse(buf.hasRemaining());
	}
	
	@Test
	public void testWriteBigEndian() {
		testWrite(true);
	}
	
	@Test
	public void testWriteLittleEndian() {
		testWrite(false);
	}
	
	private void testWrite(boolean useBigEndian) {
		byte[] res = new byte[bytes.length];
		BytesBitsBuffer.Writable buf = useBigEndian ? new BytesBitsBuffer.Writable.BigEndian(new ByteArray.Writable(res, false)) : new BytesBitsBuffer.Writable.LittleEndian(new ByteArray.Writable(res, false));
		for (int i = 0; i < bits.length; ++i) {
			Assert.assertEquals(bits.length - i, buf.remaining());
			Assert.assertTrue(buf.hasRemaining());
			buf.put(bits[(i / 8) * 8 + (useBigEndian ? 7 - (i % 8) : (i % 8))]);
		}
		Assert.assertEquals(0, buf.remaining());
		Assert.assertFalse(buf.hasRemaining());
		Assert.assertArrayEquals(bytes, res);

		res = new byte[bytes.length];
		buf = useBigEndian ? new BytesBitsBuffer.Writable.BigEndian(new ByteArray.Writable(res, false)) : new BytesBitsBuffer.Writable.LittleEndian(new ByteArray.Writable(res, false));
		for (int i = 0; i < bytes.length; ++i) {
			Assert.assertEquals((bytes.length - i) * 8, buf.remaining());
			buf.alignToNextByte(true);
			Assert.assertEquals((bytes.length - i) * 8, buf.remaining());
			buf.put(true);
			buf.alignToNextByte(false);
		}
		Assert.assertEquals(0, buf.remaining());
		Assert.assertFalse(buf.hasRemaining());
		for (int i = 0; i < bytes.length; ++i)
			Assert.assertEquals(useBigEndian ? 0x80 : 0x01, res[i] & 0xFF);

		res = new byte[bytes.length];
		buf = useBigEndian ? new BytesBitsBuffer.Writable.BigEndian(new ByteArray.Writable(res, false)) : new BytesBitsBuffer.Writable.LittleEndian(new ByteArray.Writable(res, false));
		for (int i = 0; i < bytes.length; ++i) {
			Assert.assertEquals((bytes.length - i) * 8, buf.remaining());
			buf.alignToNextByte(true);
			Assert.assertEquals((bytes.length - i) * 8, buf.remaining());
			buf.put(false);
			buf.alignToNextByte(true);
		}
		Assert.assertEquals(0, buf.remaining());
		Assert.assertFalse(buf.hasRemaining());
		for (int i = 0; i < bytes.length; ++i)
			Assert.assertEquals(useBigEndian ? 0x7F : 0xFE, res[i] & 0xFF);
	}

}
