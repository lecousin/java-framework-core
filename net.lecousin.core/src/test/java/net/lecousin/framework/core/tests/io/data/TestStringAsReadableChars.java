package net.lecousin.framework.core.tests.io.data;

import net.lecousin.framework.core.test.io.data.TestCharsReadable;
import net.lecousin.framework.io.data.StringAsReadableChars;

public class TestStringAsReadableChars extends TestCharsReadable {

	public TestStringAsReadableChars(char[] data, int initialPos, int length, boolean useSubBuffer) {
		super(initialPos == 0 ? new StringAsReadableChars(new String(data, initialPos, length)) : new StringAsReadableChars(new String(data), initialPos, length), data, initialPos, length, useSubBuffer);
	}
	
}
