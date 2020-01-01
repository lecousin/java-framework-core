package net.lecousin.framework.core.tests.encoding;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import net.lecousin.framework.core.test.encoding.AbstractTestBytesEncoding;
import net.lecousin.framework.encoding.Base64Encoding;
import net.lecousin.framework.encoding.Base64Encoding.InvalidBase64Value;
import net.lecousin.framework.encoding.BytesDecoder;
import net.lecousin.framework.encoding.BytesEncoder;
import net.lecousin.framework.encoding.UnexpectedEndOfEncodedData;
import net.lecousin.framework.io.util.RawByteBuffer;
import net.lecousin.framework.util.Pair;

import org.junit.Assert;
import org.junit.Test;

public class TestBase64Encoding extends AbstractTestBytesEncoding {

	@Override
	protected List<Pair<byte[], byte[]>> getTestCases() {
		byte[] toEncode = new byte[128*1024];
		for (int i = 0; i < 1024; ++i) {
			for (int j = 0; j < 128; ++j) {
				toEncode[i * 128 + j] = (byte)((i * j % 300) + i + j - i/3);
			}
		}
		byte[] encoded = Base64.getEncoder().encode(toEncode);
		
		return Arrays.asList(
			new Pair<>("Hello World !".getBytes(StandardCharsets.UTF_8), "SGVsbG8gV29ybGQgIQ==".getBytes(StandardCharsets.UTF_8)),
			new Pair<>("Hello World!".getBytes(StandardCharsets.UTF_8), "SGVsbG8gV29ybGQh".getBytes(StandardCharsets.UTF_8)),
			new Pair<>("Hello World".getBytes(StandardCharsets.UTF_8), "SGVsbG8gV29ybGQ=".getBytes(StandardCharsets.UTF_8)),
			new Pair<>("This is a test".getBytes(StandardCharsets.UTF_8), "VGhpcyBpcyBhIHRlc3Q=".getBytes(StandardCharsets.UTF_8)),
			new Pair<>("That is a test!".getBytes(StandardCharsets.UTF_8), "VGhhdCBpcyBhIHRlc3Qh".getBytes(StandardCharsets.UTF_8)),
			new Pair<>("This is a test".getBytes(StandardCharsets.UTF_8), "VGhpcyBpcyBhIHRlc3Q=".getBytes(StandardCharsets.UTF_8)),
			new Pair<>(toEncode, encoded)
		);
	}
	
	@Override
	protected List<Pair<byte[], byte[]>> getErrorTestCases() {
		return Arrays.asList(
			new Pair<>(null, "SGVsG8g?V29y".getBytes(StandardCharsets.UTF_8)),
			new Pair<>(null, "SGVsG8g(V29y".getBytes(StandardCharsets.UTF_8)),
			new Pair<>(null, "SG]VsG8gV29y".getBytes(StandardCharsets.UTF_8)),
			new Pair<>(null, "SGVsG".getBytes(StandardCharsets.UTF_8)),
			new Pair<>(null, "SGVsG8gV29y}".getBytes(StandardCharsets.UTF_8))
		);
	}
	
	@Override
	protected BytesEncoder createEncoder() {
		return Base64Encoding.instance;
	}
	
	@Override
	protected BytesDecoder createDecoder() {
		return Base64Encoding.instance;
	}
	
	@Test
	public void testDecoding() throws Exception {
		byte[] out = new byte[1024];
		RawByteBuffer output = new RawByteBuffer(out);
		Base64Encoding.instance.decode(new RawByteBuffer("SGVsbG8gV29ybGQgIQ=".getBytes(StandardCharsets.UTF_8)), output, true);
		Assert.assertEquals("Hello World !", new String(output.array, output.arrayOffset, output.position(), StandardCharsets.UTF_8));
		Assert.assertEquals("Hello World !", new String(Base64Encoding.instance.decode("SGVsbG8gV29ybGQgIQ=".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		Assert.assertEquals("Hello World !", new String(Base64Encoding.instance.decode(new RawByteBuffer("SGVsbG8gV29ybGQgIQ=".getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8));
		
		byte[] buf = new byte[100];
		Assert.assertEquals(3, Base64Encoding.decode4BytesBase64("VGhp".getBytes("US-ASCII"), 0, buf, 10));
		Assert.assertEquals((byte)'T', buf[10]);
		Assert.assertEquals((byte)'h', buf[11]);
		Assert.assertEquals((byte)'i', buf[12]);
		
		Assert.assertArrayEquals("This is a test".getBytes(StandardCharsets.UTF_8), Base64Encoding.instance.decode("   VGhpcyBpcyBhIHRlc3Q=".getBytes(StandardCharsets.UTF_8), 3, 20));
		
		Assert.assertEquals(1, Base64Encoding.decode4BytesBase64("a0==".getBytes("US-ASCII"), 0, buf, 0));
		Assert.assertEquals((byte)'k', buf[0]);
		
		Assert.assertEquals(0, Base64Encoding.instance.decode(new byte[0]).length);
		Assert.assertEquals(0, Base64Encoding.instance.decode(new RawByteBuffer(new byte[0])).length);
		byte[] b2 = Base64Encoding.instance.decode("a0==".getBytes("US-ASCII"));
		Assert.assertEquals(1, b2.length);
		Assert.assertEquals((byte)'k', b2[0]);

		Assert.assertEquals(0, Base64Encoding.instance.decode(new byte[0], 0, 0).length);
		b2 = Base64Encoding.instance.decode("a0==".getBytes("US-ASCII"), 0, 4);
		Assert.assertEquals(1, b2.length);
		Assert.assertEquals((byte)'k', b2[0]);
		b2 = Base64Encoding.instance.decode("a0=".getBytes("US-ASCII"), 0, 3);
		Assert.assertEquals(1, b2.length);
		Assert.assertEquals((byte)'k', b2[0]);

		b2 = Base64Encoding.instance.decode("VGhp".getBytes("US-ASCII"), 0, 4);
		Assert.assertEquals(3, b2.length);
		Assert.assertEquals((byte)'T', b2[0]);
		Assert.assertEquals((byte)'h', b2[1]);
		Assert.assertEquals((byte)'i', b2[2]);
		
		try {
			Base64Encoding.instance.decode(new byte[] { 'a' });
			throw new AssertionError("Error expected");
		} catch (UnexpectedEndOfEncodedData e) {
			// ok
		}
		try {
			Base64Encoding.instance.decode(new RawByteBuffer(new byte[] { 'a' }));
			throw new AssertionError("Error expected");
		} catch (UnexpectedEndOfEncodedData e) {
			// ok
		}
		try {
			Base64Encoding.instance.decode(new byte[] { 'a', 'a' });
			throw new AssertionError("Error expected");
		} catch (UnexpectedEndOfEncodedData e) {
			// ok
		}
		try {
			Base64Encoding.instance.decode(new RawByteBuffer(new byte[] { 'a', 'a' }));
			throw new AssertionError("Error expected");
		} catch (UnexpectedEndOfEncodedData e) {
			// ok
		}
		try {
			Base64Encoding.instance.decode(new byte[] { 'a', 'a', 'a' });
			throw new AssertionError("Error expected");
		} catch (UnexpectedEndOfEncodedData e) {
			// ok
		}
		try {
			Base64Encoding.instance.decode(new RawByteBuffer(new byte[] { 'a', 'a', 'a' }));
			throw new AssertionError("Error expected");
		} catch (UnexpectedEndOfEncodedData e) {
			// ok
		}
		
		Assert.assertEquals('B', Base64Encoding.encodeBase64(1));
		try {
			Base64Encoding.encodeBase64(-1);
			throw new AssertionError("Error expected");
		} catch (InvalidBase64Value e) {
			// ok
		}
		try {
			Base64Encoding.encodeBase64(65);
			throw new AssertionError("Error expected");
		} catch (InvalidBase64Value e) {
			// ok
		}
	}

}
