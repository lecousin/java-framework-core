package net.lecousin.framework.core.tests.text;

import net.lecousin.framework.core.test.text.TestArrayStringBuffer;
import net.lecousin.framework.text.ByteArrayStringIso8859Buffer;

public class TestByteArrayStringIso8859Buffer extends TestArrayStringBuffer<ByteArrayStringIso8859Buffer> {

	@Override
	protected ByteArrayStringIso8859Buffer createString(String s) {
		return new ByteArrayStringIso8859Buffer(s);
	}
	
}
