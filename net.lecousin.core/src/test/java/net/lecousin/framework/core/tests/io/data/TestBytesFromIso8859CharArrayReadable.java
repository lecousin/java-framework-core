package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.io.data.TestBytesReadable;
import net.lecousin.framework.io.data.BytesFromIso8859CharArray;
import net.lecousin.framework.io.data.CharArray;

public class TestBytesFromIso8859CharArrayReadable extends TestBytesReadable {

	public TestBytesFromIso8859CharArrayReadable(byte[] data, int initialPos, int length, boolean useSubBuffer) {
		super(create(data, initialPos, length), data, initialPos, length, useSubBuffer);
	}
	
	private static BytesFromIso8859CharArray create(byte[] data, int initialPos, int length) {
		char[] c = new char[data.length];
		for (int i = 0; i < c.length; ++i)
			c[i] = (char)(data[i] & 0xFF);
		return new BytesFromIso8859CharArray(new CharArray(c, initialPos, length), true);
	}
	
}
