package net.lecousin.framework.core.tests.encoding;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.encoding.QuotedPrintable;

import org.junit.Assert;
import org.junit.Test;

public class TestQuotedPrintable extends LCCoreAbstractTest {

	@Test(timeout=120000)
	public void testEncodeAndDecode() throws Exception {
		byte[] data;
		data = "1234567890123456789012345678901234567890123456789012345678901234567890 ".getBytes(StandardCharsets.US_ASCII);
		test(data);
		data = "12345678901234567890123456789012345678901234567890123456789012345678901 ".getBytes(StandardCharsets.US_ASCII);
		test(data);
		data = "123456789012345678901234567890123456789012345678901234567890123456789012 ".getBytes(StandardCharsets.US_ASCII);
		test(data);
		data = "1234567890123456789012345678901234567890123456789012345678901234567890123 ".getBytes(StandardCharsets.US_ASCII);
		test(data);
		data = "12345678901234567890123456789012345678901234567890123456789012345678901234 ".getBytes(StandardCharsets.US_ASCII);
		test(data);
		data = "123456789012345678901234567890123456789012345678901234567890123456789012345 ".getBytes(StandardCharsets.US_ASCII);
		test(data);
		data = "1234567890123456789012345678901234567890123456789012345678901234567890123456 ".getBytes(StandardCharsets.US_ASCII);
		test(data);
		data = new byte[512];
		for (int i = 0; i < 512; ++i)
			data[i] = (byte)(i * 3 - (i % 31) + i / 35);
		test(data);
	}
	
	private void test(byte[] data) throws Exception {
		ByteBuffer encoded = QuotedPrintable.encode(data);
		ByteBuffer decoded = QuotedPrintable.decode(encoded);
		for (int i = 0; i < data.length; ++i)
			Assert.assertEquals("At " + i, data[i], decoded.get());
	}
	
}
