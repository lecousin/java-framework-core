package net.lecousin.framework.core.tests.io.data;

import java.util.Arrays;
import java.util.Collection;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.core.test.runners.LCConcurrentRunner;
import net.lecousin.framework.io.data.SingleByteBitsBuffer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(LCConcurrentRunner.Parameterized.class) @org.junit.runners.Parameterized.UseParametersRunnerFactory(LCConcurrentRunner.ConcurrentParameterizedRunnedFactory.class)
public class TestSingleByteBitsBuffer extends LCCoreAbstractTest {

	@SuppressWarnings("boxing")
	@Parameters(name = "byte = {0}, useBigEndian = {2}")
	public static Collection<Object[]> parameters() {
		Collection<Object[]> tests = Arrays.asList(
			new Object[] { (byte)0x00, new boolean[] { false, false, false, false, false, false, false, false } },
			new Object[] { (byte)0xFF, new boolean[] { true, true, true, true, true, true, true, true } },
			new Object[] { (byte)0x85, new boolean[] { true, false, true, false, false, false, false, true} },
			new Object[] { (byte)0x1E, new boolean[] { false, true, true, true, true, false, false, false } },
			new Object[] { (byte)0xDA, new boolean[] { false, true, false, true, true, false, true, true } },
			new Object[] { (byte)0x11, new boolean[] { true, false, false, false, true, false, false, false } }
		);
		tests = addTestParameter(tests, Boolean.FALSE, Boolean.TRUE);
		return tests;
	}
	
	@Parameter(0)
	public byte b;
	
	@Parameter(1)
	public boolean[] bits;
	
	@Parameter(2)
	public boolean useBigEndian;
	
	@SuppressWarnings("boxing")
	@Test
	public void testRead() {
		SingleByteBitsBuffer.Readable buf = useBigEndian ? new SingleByteBitsBuffer.Readable.BigEndian(b) : new SingleByteBitsBuffer.Readable.LittleEndian(b);
		for (int i = 0; i < 8; ++i) {
			Assert.assertEquals(8 - i, buf.remaining());
			Assert.assertEquals(useBigEndian ? bits[7 - i] : bits[i], buf.get());
		}
		Assert.assertEquals(0, buf.remaining());
		Assert.assertFalse(buf.hasRemaining());
		
		buf = useBigEndian ? new SingleByteBitsBuffer.Readable.BigEndian(b) : new SingleByteBitsBuffer.Readable.LittleEndian(b);
		buf.get();
		Assert.assertTrue(buf.hasRemaining());
		buf.alignToNextByte();
		Assert.assertFalse(buf.hasRemaining());
	}
	
	@Test
	public void testWrite() {
		SingleByteBitsBuffer.Writable buf = useBigEndian ? new SingleByteBitsBuffer.Writable.BigEndian() : new SingleByteBitsBuffer.Writable.LittleEndian();
		for (int i = 0; i < 8; ++i) {
			Assert.assertEquals(8 - i, buf.remaining());
			buf.put(useBigEndian ? bits[7 - i] : bits[i]);
		}
		Assert.assertEquals(0, buf.remaining());
		Assert.assertFalse(buf.hasRemaining());
		Assert.assertEquals(b, buf.getByte());
		
		buf = useBigEndian ? new SingleByteBitsBuffer.Writable.BigEndian() : new SingleByteBitsBuffer.Writable.LittleEndian();
		buf.put(true);
		Assert.assertTrue(buf.hasRemaining());
		buf.alignToNextByte(false);
		Assert.assertFalse(buf.hasRemaining());
		buf.alignToNextByte(false);
		Assert.assertFalse(buf.hasRemaining());
		Assert.assertEquals(useBigEndian ? 0x80 : 0x01, buf.getByte() & 0xFF);
		
		buf = useBigEndian ? new SingleByteBitsBuffer.Writable.BigEndian() : new SingleByteBitsBuffer.Writable.LittleEndian();
		Assert.assertTrue(buf.hasRemaining());
		buf.alignToNextByte(true);
		Assert.assertTrue(buf.hasRemaining());
		buf.put(false);
		Assert.assertTrue(buf.hasRemaining());
		buf.alignToNextByte(true);
		Assert.assertFalse(buf.hasRemaining());
		buf.alignToNextByte(true);
		Assert.assertFalse(buf.hasRemaining());
		Assert.assertEquals(useBigEndian ? 0x7F : 0xFE, buf.getByte() & 0xFF);
	}
}
