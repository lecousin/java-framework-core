package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.util.TestIString;
import net.lecousin.framework.util.IString;
import net.lecousin.framework.util.UnprotectedStringBuffer;

public class TestUnprotectedStringBuffer extends TestIString {

	@Override
	protected IString createString(String s) {
		return new UnprotectedStringBuffer(s);
	}
	
}
