package net.lecousin.framework.core.tests.encoding.charset;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.core.test.encoding.charset.AbstractTestCharsetDecoder;

public class TestUSAscii extends AbstractTestCharsetDecoder {

	public TestUSAscii() {
		super(StandardCharsets.US_ASCII);
	}
	
	@Override
	protected String[] getTestStrings() {
		return new String[] {
			"abcd012GH)I_V;"
		};
	}
	
}
