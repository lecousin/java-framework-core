package net.lecousin.framework.core.tests.io.util;

import java.io.EOFException;
import java.nio.ByteBuffer;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOAsInputStream;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.util.DataUtil;

import org.junit.Assert;
import org.junit.Test;

public class TestDataUtil extends LCCoreAbstractTest {

	@Test(timeout=120000)
	public void testBuffer() throws Exception {
		testShort((short)0x0000, 0);
		testShort((short)0x0001, 1);
		testShort((short)0x0123, 0x0123);
		testShort((short)0x2FCE, 0x2FCE);
		testShort((short)-1, 0xFFFF);
		testShort((short)-2, 0xFFFE);
		testShort(Short.MIN_VALUE, 0x8000);
		testShort(Short.MAX_VALUE, Short.MAX_VALUE);
		testInt(0, 0);
		testInt(1, 1);
		testInt(-1, 0xFFFFFFFFL);
		testInt(-2, 0xFFFFFFFEL);
		testInt(0xFFF, 0xFFF);
		testInt(Integer.MIN_VALUE, 0x80000000L);
		testInt(Integer.MAX_VALUE, Integer.MAX_VALUE);
		testUnsignedInt24Bits(0);
		testUnsignedInt24Bits(1);
		testUnsignedInt24Bits(0xFF);
		testUnsignedInt24Bits(0xFFF);
		testUnsignedInt24Bits(0xFFFF);
		testUnsignedInt24Bits(0xFFFFFF);
		testLong(0);
		testLong(-1);
		testLong(Long.MAX_VALUE);
		testLong(Long.MIN_VALUE);
	}
	
	protected void testShort(short value, int unsigned) throws Exception {
		byte[] buf = new byte[32];
		DataUtil.writeShortLittleEndian(buf, 0, value);
		Assert.assertEquals(value, DataUtil.readShortLittleEndian(buf, 0));
		DataUtil.writeShortLittleEndian(buf, 4, value);
		Assert.assertEquals(value, DataUtil.readShortLittleEndian(buf, 4));
		DataUtil.writeShortBigEndian(buf, 0, value);
		Assert.assertEquals(value, DataUtil.readShortBigEndian(buf, 0));
		DataUtil.writeShortBigEndian(buf, 8, value);
		Assert.assertEquals(value, DataUtil.readShortBigEndian(buf, 8));
		DataUtil.writeUnsignedShortLittleEndian(buf, 0, value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortLittleEndian(buf, 0));
		DataUtil.writeUnsignedShortBigEndian(buf, 0, value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortBigEndian(buf, 0));
		DataUtil.writeShortLittleEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.readShortLittleEndian(buf, 1));
		DataUtil.writeShortBigEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.readShortBigEndian(buf, 1));
		DataUtil.writeShortBigEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.readShortBigEndian(ByteBuffer.wrap(buf, 1, 30)));
		DataUtil.writeUnsignedShortLittleEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortLittleEndian(buf, 1));
		DataUtil.writeUnsignedShortBigEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortBigEndian(buf, 1));
		DataUtil.writeUnsignedShortBigEndian(ByteBuffer.wrap(buf, 0, 30), value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortBigEndian(ByteBuffer.wrap(buf, 0, 30)));
		ByteArrayIO io = new ByteArrayIO(32, "TestDataUtil");
		DataUtil.writeShortLittleEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readShortLittleEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readShortLittleEndian(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeShortBigEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readShortBigEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readShortBigEndian(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeUnsignedShortLittleEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortLittleEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortLittleEndian(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeUnsignedShortBigEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortBigEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortBigEndian(IOAsInputStream.get(io, false)));
		
		// empty IO should throw EOFException
		io.setSizeSync(0);
		try { DataUtil.readShortLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readShortBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readUnsignedShortLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readUnsignedShortBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readShortLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readShortBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readUnsignedShortLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readUnsignedShortBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 1 byte should throw EOFException
		io.setSizeSync(1);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readShortLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readShortBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedShortLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedShortBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readShortLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readShortBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedShortLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedShortBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		io.close();
	}
	
	protected void testInt(int value, long unsigned) throws Exception {
		byte[] buf = new byte[32];
		DataUtil.writeIntegerLittleEndian(buf, 0, value);
		Assert.assertEquals(value, DataUtil.readIntegerLittleEndian(buf, 0));
		DataUtil.writeIntegerLittleEndian(buf, 4, value);
		Assert.assertEquals(value, DataUtil.readIntegerLittleEndian(buf, 4));
		DataUtil.writeIntegerBigEndian(buf, 0, value);
		Assert.assertEquals(value, DataUtil.readIntegerBigEndian(buf, 0));
		DataUtil.writeIntegerBigEndian(buf, 8, value);
		Assert.assertEquals(value, DataUtil.readIntegerBigEndian(buf, 8));
		DataUtil.writeUnsignedIntegerLittleEndian(buf, 0, value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerLittleEndian(buf, 0));
		DataUtil.writeUnsignedIntegerBigEndian(buf, 0, value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerBigEndian(buf, 0));
		DataUtil.writeIntegerLittleEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.readIntegerLittleEndian(buf, 1));
		DataUtil.writeIntegerBigEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.readIntegerBigEndian(buf, 1));
		DataUtil.writeUnsignedIntegerLittleEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerLittleEndian(buf, 1));
		DataUtil.writeUnsignedIntegerBigEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerBigEndian(buf, 1));
		DataUtil.writeUnsignedIntegerBigEndian(ByteBuffer.wrap(buf, 0, 30), value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerBigEndian(ByteBuffer.wrap(buf, 0, 30)));
		ByteArrayIO io = new ByteArrayIO(32, "TestDataUtil");
		DataUtil.writeIntegerLittleEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readIntegerLittleEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readIntegerLittleEndian(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeIntegerBigEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readIntegerBigEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readIntegerBigEndian(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeUnsignedIntegerLittleEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerLittleEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerLittleEndian(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeUnsignedIntegerBigEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerBigEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerBigEndian(IOAsInputStream.get(io, false)));
		
		// empty IO should throw EOFException
		io.setSizeSync(0);
		try { DataUtil.readIntegerLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readIntegerBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readUnsignedIntegerLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readUnsignedIntegerBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readIntegerLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readIntegerBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readUnsignedIntegerLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readUnsignedIntegerBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		// IO with  only 1 byte should throw EOFException
		io.setSizeSync(1);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		// IO with  only 2 bytes should throw EOFException
		io.setSizeSync(2);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		// IO with  only 3 bytes should throw EOFException
		io.setSizeSync(3);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readIntegerBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedIntegerBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		io.close();
	}
	
	protected void testUnsignedInt24Bits(int value) throws Exception {
		byte[] buf = new byte[32];
		DataUtil.writeUnsignedInteger24BitsLittleEndian(buf, 0, value);
		Assert.assertEquals(value, DataUtil.readUnsignedInteger24BitsLittleEndian(buf, 0));
		DataUtil.writeUnsignedInteger24BitsBigEndian(buf, 0, value);
		Assert.assertEquals(value, DataUtil.readUnsignedInteger24BitsBigEndian(buf, 0));
		DataUtil.writeUnsignedInteger24BitsLittleEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.readUnsignedInteger24BitsLittleEndian(buf, 1));
		DataUtil.writeUnsignedInteger24BitsBigEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.readUnsignedInteger24BitsBigEndian(buf, 1));
		DataUtil.writeUnsignedInteger24BitsBigEndian(ByteBuffer.wrap(buf, 0, 30), value);
		Assert.assertEquals(value, DataUtil.readUnsignedInteger24BitsBigEndian(ByteBuffer.wrap(buf, 0, 30)));
		ByteArrayIO io = new ByteArrayIO(32, "TestDataUtil");
		DataUtil.writeUnsignedInteger24BitsLittleEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readUnsignedInteger24BitsLittleEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readUnsignedInteger24BitsLittleEndian(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeUnsignedInteger24BitsBigEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readUnsignedInteger24BitsBigEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readUnsignedInteger24BitsBigEndian(IOAsInputStream.get(io, false)));
		
		// empty IO should throw EOFException
		io.setSizeSync(0);
		try { DataUtil.readUnsignedInteger24BitsLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readUnsignedInteger24BitsBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readUnsignedInteger24BitsLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readUnsignedInteger24BitsBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		// IO with  only 1 byte should throw EOFException
		io.setSizeSync(1);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedInteger24BitsLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedInteger24BitsBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedInteger24BitsLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedInteger24BitsBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		// IO with  only 2 bytes should throw EOFException
		io.setSizeSync(2);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedInteger24BitsLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedInteger24BitsBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedInteger24BitsLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readUnsignedInteger24BitsBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		io.close();
	}
	
	protected void testLong(long value) throws Exception {
		byte[] buf = new byte[32];
		DataUtil.writeLongLittleEndian(buf, 0, value);
		Assert.assertEquals(value, DataUtil.readLongLittleEndian(buf, 0));
		DataUtil.writeLongLittleEndian(buf, 4, value);
		Assert.assertEquals(value, DataUtil.readLongLittleEndian(buf, 4));
		DataUtil.writeLongBigEndian(buf, 0, value);
		Assert.assertEquals(value, DataUtil.readLongBigEndian(buf, 0));
		DataUtil.writeLongBigEndian(buf, 8, value);
		Assert.assertEquals(value, DataUtil.readLongBigEndian(buf, 8));
		DataUtil.writeLongLittleEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.readLongLittleEndian(buf, 1));
		DataUtil.writeLongBigEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.readLongBigEndian(buf, 1));
		ByteArrayIO io = new ByteArrayIO(32, "TestDataUtil");
		DataUtil.writeLongLittleEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readLongLittleEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readLongLittleEndian(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeLongBigEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readLongBigEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readLongBigEndian(IOAsInputStream.get(io, false)));
		
		DataUtil.writeLongLittleEndian(buf, 0, value);
		byte[] b = DataUtil.getBytesLittleEndian(value);
		Assert.assertEquals(0, ArrayUtil.compare(b, 0, buf, 0, b.length));
		
		// empty IO should throw EOFException
		io.setSizeSync(0);
		try { DataUtil.readLongLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readLongBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readLongLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.readLongBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 1 byte should throw EOFException
		io.setSizeSync(1);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 2 bytes should throw EOFException
		io.setSizeSync(2);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 3 bytes should throw EOFException
		io.setSizeSync(3);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 4 bytes should throw EOFException
		io.setSizeSync(4);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 5 bytes should throw EOFException
		io.setSizeSync(5);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 6 bytes should throw EOFException
		io.setSizeSync(6);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 7 bytes should throw EOFException
		io.setSizeSync(7);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongLittleEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.readLongBigEndian(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		io.close();
	}
	
}
