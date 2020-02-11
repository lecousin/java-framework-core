package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.io.data.TestCharsReadable;
import net.lecousin.framework.io.data.CharsFromIso8859Bytes;
import net.lecousin.framework.io.data.ByteArray;

public class TestCharsFromIso8859Bytes extends TestCharsReadable {

	public TestCharsFromIso8859Bytes(char[] data, int initialPos, int length, boolean useSubBuffer) {
		super(create(data, initialPos, length), data, initialPos, length, useSubBuffer);
	}
	
	private static CharsFromIso8859Bytes create(char[] data, int initialPos, int length) {
		byte[] b = new byte[data.length];
		for (int i = 0; i < data.length; ++i) {
			b[i] = (byte)data[i];
			data[i] = (char)(b[i] & 0xFF);
		}
		return new CharsFromIso8859Bytes(new ByteArray(b, initialPos, length), true);
	}
		
}
