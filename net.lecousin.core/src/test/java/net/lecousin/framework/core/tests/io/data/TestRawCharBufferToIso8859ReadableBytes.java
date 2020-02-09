package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.io.data.TestBytesReadable;
import net.lecousin.framework.io.data.Bytes;
import net.lecousin.framework.io.data.RawCharBuffer;

public class TestRawCharBufferToIso8859ReadableBytes extends TestBytesReadable {

	public TestRawCharBufferToIso8859ReadableBytes(byte[] data, int initialPos, int length, boolean useSubBuffer) {
		super(create(data, initialPos, length), data, initialPos, length, useSubBuffer);
	}
	
	private static Bytes.Readable create(byte[] data, int initialPos, int length) {
		char[] c = new char[data.length];
		for (int i = 0; i < c.length; ++i)
			c[i] = (char)(data[i] & 0xFF);
		return new RawCharBuffer(c, initialPos, length).iso8859AsReadableBytes();
	}
	
}
