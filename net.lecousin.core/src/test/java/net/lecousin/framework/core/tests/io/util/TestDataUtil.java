package net.lecousin.framework.core.tests.io.util;

import java.nio.ByteBuffer;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IOAsInputStream;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.util.DataUtil;

import org.junit.Assert;
import org.junit.Test;

public class TestDataUtil extends LCCoreAbstractTest {

	@Test
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
		DataUtil.writeUnsignedShortLittleEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortLittleEndian(buf, 1));
		DataUtil.writeUnsignedShortBigEndian(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortBigEndian(buf, 1));
		ByteArrayIO io = new ByteArrayIO(32, "TestDataUtil");
		DataUtil.writeShortLittleEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readShortLittleEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readShortLittleEndian(IOAsInputStream.get(io)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeShortBigEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readShortBigEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readShortBigEndian(IOAsInputStream.get(io)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeUnsignedShortLittleEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortLittleEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortLittleEndian(IOAsInputStream.get(io)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeUnsignedShortBigEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortBigEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedShortBigEndian(IOAsInputStream.get(io)));
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
		ByteArrayIO io = new ByteArrayIO(32, "TestDataUtil");
		DataUtil.writeIntegerLittleEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readIntegerLittleEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readIntegerLittleEndian(IOAsInputStream.get(io)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeIntegerBigEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readIntegerBigEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readIntegerBigEndian(IOAsInputStream.get(io)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeUnsignedIntegerLittleEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerLittleEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerLittleEndian(IOAsInputStream.get(io)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeUnsignedIntegerBigEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerBigEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.readUnsignedIntegerBigEndian(IOAsInputStream.get(io)));
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
		Assert.assertEquals(value, DataUtil.readLongLittleEndian(IOAsInputStream.get(io)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.writeLongBigEndian(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readLongBigEndian(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.readLongBigEndian(IOAsInputStream.get(io)));
		io.close();
	}
	
}
