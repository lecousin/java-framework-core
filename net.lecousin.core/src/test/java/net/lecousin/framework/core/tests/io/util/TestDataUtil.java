package net.lecousin.framework.core.tests.io.util;

import java.io.EOFException;
import java.nio.ByteBuffer;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.IOAsInputStream;
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
		DataUtil.Write16.LE.write(buf, 0, value);
		Assert.assertEquals(value, DataUtil.Read16.LE.read(buf, 0));
		DataUtil.Write16.LE.write(buf, 4, value);
		Assert.assertEquals(value, DataUtil.Read16.LE.read(buf, 4));
		DataUtil.Write16.BE.write(buf, 0, value);
		Assert.assertEquals(value, DataUtil.Read16.BE.read(buf, 0));
		DataUtil.Write16.BE.write(buf, 8, value);
		Assert.assertEquals(value, DataUtil.Read16.BE.read(buf, 8));
		DataUtil.Write16U.LE.write(buf, 0, value);
		Assert.assertEquals(unsigned, DataUtil.Read16U.LE.read(buf, 0));
		DataUtil.Write16U.BE.write(buf, 0, value);
		Assert.assertEquals(unsigned, DataUtil.Read16U.BE.read(buf, 0));
		DataUtil.Write16.LE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.Read16.LE.read(buf, 1));
		DataUtil.Write16.BE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.Read16.BE.read(buf, 1));
		DataUtil.Write16.BE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.Read16.BE.read(ByteBuffer.wrap(buf, 1, 30)));
		DataUtil.Write16U.LE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(unsigned, DataUtil.Read16U.LE.read(buf, 1));
		DataUtil.Write16U.BE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(unsigned, DataUtil.Read16U.BE.read(buf, 1));
		DataUtil.Write16U.BE.write(ByteBuffer.wrap(buf, 0, 30), value);
		Assert.assertEquals(unsigned, DataUtil.Read16U.BE.read(ByteBuffer.wrap(buf, 0, 30)));
		ByteArrayIO io = new ByteArrayIO(32, "TestDataUtil");
		DataUtil.Write16.LE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read16.LE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read16.LE.read(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.Write16.BE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read16.BE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read16.BE.read(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.Write16U.LE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.Read16U.LE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.Read16U.LE.read(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.Write16U.BE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.Read16U.BE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.Read16U.BE.read(IOAsInputStream.get(io, false)));
		
		// empty IO should throw EOFException
		io.setSizeSync(0);
		try { DataUtil.Read16.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read16.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read16U.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read16U.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read16.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read16.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read16U.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read16U.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 1 byte should throw EOFException
		io.setSizeSync(1);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read16.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read16.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read16U.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read16U.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read16.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read16.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read16U.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read16U.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		io.close();
	}
	
	protected void testInt(int value, long unsigned) throws Exception {
		byte[] buf = new byte[32];
		DataUtil.Write32.LE.write(buf, 0, value);
		Assert.assertEquals(value, DataUtil.Read32.LE.read(buf, 0));
		DataUtil.Write32.LE.write(buf, 4, value);
		Assert.assertEquals(value, DataUtil.Read32.LE.read(buf, 4));
		DataUtil.Write32.BE.write(buf, 0, value);
		Assert.assertEquals(value, DataUtil.Read32.BE.read(buf, 0));
		DataUtil.Write32.BE.write(buf, 8, value);
		Assert.assertEquals(value, DataUtil.Read32.BE.read(buf, 8));
		DataUtil.Write32U.LE.write(buf, 0, value);
		Assert.assertEquals(unsigned, DataUtil.Read32U.LE.read(buf, 0));
		DataUtil.Write32U.BE.write(buf, 0, value);
		Assert.assertEquals(unsigned, DataUtil.Read32U.BE.read(buf, 0));
		DataUtil.Write32.LE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.Read32.LE.read(buf, 1));
		DataUtil.Write32.BE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.Read32.BE.read(buf, 1));
		DataUtil.Write32U.LE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(unsigned, DataUtil.Read32U.LE.read(buf, 1));
		DataUtil.Write32U.BE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(unsigned, DataUtil.Read32U.BE.read(buf, 1));
		DataUtil.Write32U.BE.write(ByteBuffer.wrap(buf, 0, 30), value);
		Assert.assertEquals(unsigned, DataUtil.Read32U.BE.read(ByteBuffer.wrap(buf, 0, 30)));
		ByteArrayIO io = new ByteArrayIO(32, "TestDataUtil");
		DataUtil.Write32.LE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read32.LE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read32.LE.read(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.Write32.BE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read32.BE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read32.BE.read(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.Write32U.LE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.Read32U.LE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.Read32U.LE.read(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.Write32U.BE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.Read32U.BE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(unsigned, DataUtil.Read32U.BE.read(IOAsInputStream.get(io, false)));
		
		// empty IO should throw EOFException
		io.setSizeSync(0);
		try { DataUtil.Read32.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read32.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read32U.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read32U.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read32.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read32.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read32U.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read32U.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		// IO with  only 1 byte should throw EOFException
		io.setSizeSync(1);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		// IO with  only 2 bytes should throw EOFException
		io.setSizeSync(2);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		// IO with  only 3 bytes should throw EOFException
		io.setSizeSync(3);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read32U.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		io.close();
	}
	
	protected void testUnsignedInt24Bits(int value) throws Exception {
		byte[] buf = new byte[32];
		DataUtil.Write24U.LE.write(buf, 0, value);
		Assert.assertEquals(value, DataUtil.Read24U.LE.read(buf, 0));
		DataUtil.Write24U.BE.write(buf, 0, value);
		Assert.assertEquals(value, DataUtil.Read24U.BE.read(buf, 0));
		DataUtil.Write24U.LE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.Read24U.LE.read(buf, 1));
		DataUtil.Write24U.BE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.Read24U.BE.read(buf, 1));
		DataUtil.Write24U.BE.write(ByteBuffer.wrap(buf, 0, 30), value);
		Assert.assertEquals(value, DataUtil.Read24U.BE.read(ByteBuffer.wrap(buf, 0, 30)));
		ByteArrayIO io = new ByteArrayIO(32, "TestDataUtil");
		DataUtil.Write24U.LE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read24U.LE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read24U.LE.read(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.Write24U.BE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read24U.BE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read24U.BE.read(IOAsInputStream.get(io, false)));
		
		// empty IO should throw EOFException
		io.setSizeSync(0);
		try { DataUtil.Read24U.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read24U.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read24U.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read24U.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		// IO with  only 1 byte should throw EOFException
		io.setSizeSync(1);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read24U.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read24U.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read24U.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read24U.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		// IO with  only 2 bytes should throw EOFException
		io.setSizeSync(2);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read24U.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read24U.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read24U.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read24U.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		
		io.close();
	}
	
	protected void testLong(long value) throws Exception {
		byte[] buf = new byte[32];
		DataUtil.Write64.LE.write(buf, 0, value);
		Assert.assertEquals(value, DataUtil.Read64.LE.read(buf, 0));
		DataUtil.Write64.LE.write(buf, 4, value);
		Assert.assertEquals(value, DataUtil.Read64.LE.read(buf, 4));
		DataUtil.Write64.BE.write(buf, 0, value);
		Assert.assertEquals(value, DataUtil.Read64.BE.read(buf, 0));
		DataUtil.Write64.BE.write(buf, 8, value);
		Assert.assertEquals(value, DataUtil.Read64.BE.read(buf, 8));
		DataUtil.Write64.LE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.Read64.LE.read(buf, 1));
		DataUtil.Write64.BE.write(ByteBuffer.wrap(buf, 1, 30), value);
		Assert.assertEquals(value, DataUtil.Read64.BE.read(buf, 1));
		ByteArrayIO io = new ByteArrayIO(32, "TestDataUtil");
		DataUtil.Write64.LE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read64.LE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read64.LE.read(IOAsInputStream.get(io, false)));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		DataUtil.Write64.BE.write(io, value);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read64.BE.read(io));
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		Assert.assertEquals(value, DataUtil.Read64.BE.read(IOAsInputStream.get(io, false)));
		
		// empty IO should throw EOFException
		io.setSizeSync(0);
		try { DataUtil.Read64.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read64.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read64.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		try { DataUtil.Read64.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 1 byte should throw EOFException
		io.setSizeSync(1);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 2 bytes should throw EOFException
		io.setSizeSync(2);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 3 bytes should throw EOFException
		io.setSizeSync(3);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 4 bytes should throw EOFException
		io.setSizeSync(4);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 5 bytes should throw EOFException
		io.setSizeSync(5);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 6 bytes should throw EOFException
		io.setSizeSync(6);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		// IO with  only 7 bytes should throw EOFException
		io.setSizeSync(7);
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(io); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.LE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}
		io.seekSync(SeekType.FROM_BEGINNING, 0);
		try { DataUtil.Read64.BE.read(IOAsInputStream.get(io, false)); throw new AssertionError(); } catch (EOFException e) {}

		io.close();
	}
	
}
