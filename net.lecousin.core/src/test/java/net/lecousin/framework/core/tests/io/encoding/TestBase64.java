package net.lecousin.framework.core.tests.io.encoding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.io.IO.Seekable.SeekType;
import net.lecousin.framework.io.buffering.ByteArrayIO;
import net.lecousin.framework.io.buffering.ByteBuffersIO;
import net.lecousin.framework.io.buffering.MemoryIO;
import net.lecousin.framework.io.encoding.Base64;
import net.lecousin.framework.io.encoding.Base64Decoder;

public class TestBase64 extends LCCoreAbstractTest {

	@Test(timeout=120000)
	public void testEncoding() {
		Assert.assertEquals("SGVsbG8gV29ybGQgIQ==", new String(Base64.encodeBase64("Hello World !".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		Assert.assertEquals("SGVsbG8gV29ybGQh", new String(Base64.encodeBase64("Hello World!".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		Assert.assertEquals("SGVsbG8gV29ybGQ=", new String(Base64.encodeBase64("Hello World".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}
	
	@Test(timeout=120000)
	public void testDecoding() throws IOException {
		Assert.assertEquals("This is a test", new String(Base64.decode("VGhpcyBpcyBhIHRlc3Q=".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		Assert.assertEquals("That is a test!", new String(Base64.decode("VGhhdCBpcyBhIHRlc3Qh".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		Assert.assertEquals("This is a test", new String(Base64.decode("VGhpcyBpcyBhIHRlc3Q=")));
		Assert.assertEquals("That is a test!", new String(Base64.decode("VGhhdCBpcyBhIHRlc3Qh=")));
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
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
	@Test(timeout=120000)
	public void testEncodeAndDecodeBuffers() throws Exception {
		ByteBuffersIO input = new ByteBuffersIO(false, "base64_src", Task.PRIORITY_NORMAL);
		for (int i = 0; i < 1024; ++i) {
			byte[] data = new byte[128];
			for (int j = 0; j < 128; ++j)
				data[j] = (byte)((i * j % 300) + i + j - i/3);
			input.addBuffer(data, 0, 1);
			input.addBuffer(data, 1, 1);
			input.addBuffer(data, 2, 1);
			input.addBuffer(data, 3, 125);
		}
		MemoryIO encoded = new MemoryIO(8192, "base64_encoded");
		Base64.encodeAsync(input, encoded).blockException(0);
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
			byte[] data = new byte[128];
			for (int j = 0; j < 128; ++j)
				data[j] = (byte)((i * j % 300) + i + j - i/3);
			Assert.assertArrayEquals("Buffer " + i, data, b);
		}
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testEncodeAndDecodeBytesSmall() throws Exception {
		byte[] data = new byte[128*64];
		for (int i = 0; i < 64; ++i) {
			for (int j = 0; j < 128; ++j) {
				data[i * 128 + j] = (byte)((i * j % 300) + i + j - i/3);
			}
		}
		MemoryIO encoded = new MemoryIO(1024, "base64_encoded");
		Base64.encodeAsync(new ByteArrayIO(data, "base64_src"), encoded).blockException(0);
		encoded.seekSync(SeekType.FROM_BEGINNING, 0);
		MemoryIO decoded = new MemoryIO(1024, "base64_decoded");
		Base64Decoder decoder = new Base64Decoder(decoded);
		do {
			ByteBuffer b = ByteBuffer.allocate(3);
			int nb = encoded.readFullySync(b);
			if (nb <= 0) break;
			b.flip();
			decoder.decode(b).blockException(0);
		} while (true);
		decoder.flush().blockException(0);
		decoded.seekSync(SeekType.FROM_BEGINNING, 0);
		byte[] b = new byte[128];
		for (int i = 0; i < 64; ++i) {
			int nb = decoded.readFully(b);
			Assert.assertEquals(128, nb);
			Assert.assertTrue(ArrayUtil.equals(data, i * 128, b, 0, 128));
		}
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testDecodeChars() throws Exception {
		CharBuffer cb = CharBuffer.wrap("VGhhdCBpcyBhIHRlc3QgdG8gZGVjb2RlIGJhc2UgNjQgd2l0aCBjaGFyYWN0ZXIgc3RyZWFt");
		ByteArrayIO decoded = new ByteArrayIO(512, "base64_decoded");
		Base64Decoder decoder = new Base64Decoder(decoded);
		decoder.decode(cb).blockException(0);
		decoder.flush().blockException(0);
		Assert.assertEquals("That is a test to decode base 64 with character stream", decoded.getAsString(StandardCharsets.US_ASCII));
	}
	
	@SuppressWarnings("resource")
	@Test(timeout=120000)
	public void testDecodeCharsSmall() throws Exception {
		CharBuffer cb1 = CharBuffer.wrap("V");
		CharBuffer cb2 = CharBuffer.wrap("G");
		CharBuffer cb3 = CharBuffer.wrap("h");
		CharBuffer cb4 = CharBuffer.wrap("h");
		CharBuffer cb5 = CharBuffer.wrap("dC");
		CharBuffer cb6 = CharBuffer.wrap("BpcyB");
		CharBuffer cb7 = CharBuffer.wrap("hIHRlc3Qg");
		CharBuffer cb8 = CharBuffer.wrap("dG8gZGVjb2R");
		CharBuffer cb9 = CharBuffer.wrap("lIGJhc2UgNjQgd2l0aCBjaGFyYWN0ZXIgc3RyZWFt");
		ByteArrayIO decoded = new ByteArrayIO(512, "base64_decoded");
		Base64Decoder decoder = new Base64Decoder(decoded);
		decoder.decode(cb1).blockException(0);
		decoder.decode(cb2).blockException(0);
		decoder.decode(cb3).blockException(0);
		decoder.decode(cb4).blockException(0);
		decoder.decode(cb5).blockException(0);
		decoder.decode(cb6).blockException(0);
		decoder.decode(cb7).blockException(0);
		decoder.decode(cb8).blockException(0);
		decoder.decode(cb9).blockException(0);
		decoder.flush().blockException(0);
		Assert.assertEquals("That is a test to decode base 64 with character stream", decoded.getAsString(StandardCharsets.US_ASCII));
	}
	
}
