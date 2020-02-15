package net.lecousin.framework.core.tests.text;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.math.RangeInteger;
import net.lecousin.framework.text.StringFormat;
import net.lecousin.framework.text.StringParser;

import org.junit.Assert;
import org.junit.Test;

public class TestStringParser extends LCCoreAbstractTest {

	private static class TestAttributes {
		
		@StringFormat(parser = RangeInteger.Parser.class)
		public RangeInteger integers1;
		
		public RangeInteger integers2;
	}
	
	@Test
	public void test() throws Exception {
		TestAttributes t = new TestAttributes();
		t.integers1 = StringParser.parse(RangeInteger.class, TestAttributes.class.getField("integers1").getAnnotation(StringFormat.class), "[3-85]");
		Assert.assertEquals(3, t.integers1.min);
		Assert.assertEquals(85, t.integers1.max);
		t.integers2 = StringParser.parse(RangeInteger.class, "]97-147[");
		Assert.assertEquals(98, t.integers2.min);
		Assert.assertEquals(146, t.integers2.max);
	}
	
}
