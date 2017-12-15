package net.lecousin.framework.core.tests.encoding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.buffering.MemoryIO;
import net.lecousin.framework.io.encoding.Base64;
import net.lecousin.framework.io.encoding.Base64Decoder;

public class TestBase64 extends LCCoreAbstractTest {

	@Test
	public void testEncoding() {
		Assert.assertEquals("SGVsbG8gV29ybGQgIQ==", new String(Base64.encodeBase64("Hello World !".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		Assert.assertEquals("SGVsbG8gV29ybGQh", new String(Base64.encodeBase64("Hello World!".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		Assert.assertEquals("SGVsbG8gV29ybGQ=", new String(Base64.encodeBase64("Hello World".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}
	
	@Test
	public void testDecoding() throws IOException {
		Assert.assertEquals("This is a test", new String(Base64.decode("VGhpcyBpcyBhIHRlc3Q=".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		Assert.assertEquals("That is a test!", new String(Base64.decode("VGhhdCBpcyBhIHRlc3Qh".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testEncodeAndDecodeBytes() throws Exception {
		byte[] data = new byte[128*1024];
		for (int i = 0; i < 1024; ++i) {
			for (int j = 0; j < 128; ++j) {
				data[i * 128 + j] = (byte)((i * j % 300) + i + j - i/3);
			}
		}
		MemoryIO encoded = new MemoryIO(8192, "base64_encoded");
		Base64.encodeAsync(new ByteArrayIO(data, "base64_src"), encoded).blockException(0);
		encoded.seekSync(SeekType.FROM_BEGINNING, 0);
		MemoryIO decoded = new MemoryIO(8192, "base64_decoded");
		Base64Decoder decoder = new Base64Decoder(decoded);
		do {
			ByteBuffer b = ByteBuffer.allocate(123);
			int nb = encoded.readFullySync(b);
			if (nb <= 0) break;
			b.flip();
			decoder.decode(b).blockException(0);
		} while (true);
		decoder.flush().blockException(0);
		decoded.seekSync(SeekType.FROM_BEGINNING, 0);
		byte[] b = new byte[128];
		for (int i = 0; i < 1024; ++i) {
			int nb = decoded.readFully(b);
			Assert.assertEquals(128, nb);
			Assert.assertTrue(ArrayUtil.equals(data, i * 128, b, 0, 128));
		}
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testDecodeChars() throws Exception {
		CharBuffer cb = CharBuffer.wrap("VGhhdCBpcyBhIHRlc3QgdG8gZGVjb2RlIGJhc2UgNjQgd2l0aCBjaGFyYWN0ZXIgc3RyZWFt");
		ByteArrayIO decoded = new ByteArrayIO(512, "base64_decoded");
		Base64Decoder decoder = new Base64Decoder(decoded);
		decoder.decode(cb).blockException(0);
		decoder.flush().blockException(0);
		Assert.assertEquals("That is a test to decode base 64 with character stream", decoded.getAsString(StandardCharsets.US_ASCII));
	}
	
}
