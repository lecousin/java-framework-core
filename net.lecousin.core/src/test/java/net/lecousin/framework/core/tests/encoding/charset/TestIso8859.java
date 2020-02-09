package net.lecousin.framework.core.tests.encoding.charset;

import java.nio.charset.StandardCharsets;

import net.lecousin.framework.core.test.encoding.charset.AbstractTestCharsetDecoder;

public class TestIso8859 extends AbstractTestCharsetDecoder {

	public TestIso8859() {
		super(StandardCharsets.ISO_8859_1);
	}
	
	@Override
	protected String[] getTestStrings() {
		return new String[] {
			"abcd012GH)I_V;"
		};
	}
	
}
