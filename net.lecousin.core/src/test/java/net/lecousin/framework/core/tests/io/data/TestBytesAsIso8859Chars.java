package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.io.data.TestCharsReadable;
import net.lecousin.framework.io.data.BytesAsIso8859Chars;
import net.lecousin.framework.io.data.RawByteBuffer;

public class TestBytesAsIso8859Chars extends TestCharsReadable {

	public TestBytesAsIso8859Chars(char[] data, int initialPos, int length, boolean useSubBuffer) {
		super(create(data, initialPos, length), data, initialPos, length, useSubBuffer);
	}
	
	private static BytesAsIso8859Chars create(char[] data, int initialPos, int length) {
		byte[] b = new byte[data.length];
		for (int i = 0; i < data.length; ++i) {
			b[i] = (byte)data[i];
			data[i] = (char)(b[i] & 0xFF);
		}
		return new BytesAsIso8859Chars(new RawByteBuffer(b, initialPos, length));
	}
		
}
