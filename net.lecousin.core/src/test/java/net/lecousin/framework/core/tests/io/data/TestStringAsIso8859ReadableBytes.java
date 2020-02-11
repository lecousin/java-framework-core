package net.lecousin.framework.core.tests.io.data;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.core.test.io.data.TestBytesReadable;
import net.lecousin.framework.io.data.BytesFromIso8859String;

public class TestStringAsIso8859ReadableBytes extends TestBytesReadable {

	public TestStringAsIso8859ReadableBytes(byte[] data, int initialPos, int length, boolean useSubBuffer) {
		super(initialPos == 0 && length == data.length ? new BytesFromIso8859String(new String(data, StandardCharsets.ISO_8859_1)) : new BytesFromIso8859String(new String(data, StandardCharsets.ISO_8859_1), initialPos, length), data, initialPos, length, useSubBuffer);
	}
	
}
