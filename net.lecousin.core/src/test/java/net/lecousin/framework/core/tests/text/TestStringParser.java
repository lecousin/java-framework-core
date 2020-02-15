package net.lecousin.framework.core.tests.text;

import java.text.ParseException;

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

		@StringFormat(parser=CustomParser.class)
		public Integer custom1;

		@StringFormat(parser=CustomParser.class, pattern = "salut")
		public Integer custom2;
	}
	
	
	public static class CustomParser implements StringParser<Integer> {
		public CustomParser() {
			this("hello");
		}
		
		public CustomParser(String pattern) {
			this.pattern = pattern;
		}
		
		private String pattern;
		
		@Override
		public Integer parse(String string) throws ParseException {
			return pattern.equals(string) ? Integer.valueOf(51) : Integer.valueOf(0);
		}
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
		t.custom1 = StringParser.parse(Integer.class, TestAttributes.class.getField("custom1").getAnnotation(StringFormat.class), "hello");
		Assert.assertEquals(51, t.custom1.intValue());
		t.custom1 = StringParser.parse(Integer.class, TestAttributes.class.getField("custom1").getAnnotation(StringFormat.class), "salut");
		Assert.assertEquals(0, t.custom1.intValue());
		t.custom2 = StringParser.parse(Integer.class, TestAttributes.class.getField("custom2").getAnnotation(StringFormat.class), "hello");
		Assert.assertEquals(0, t.custom2.intValue());
		t.custom2 = StringParser.parse(Integer.class, TestAttributes.class.getField("custom2").getAnnotation(StringFormat.class), "salut");
		Assert.assertEquals(51, t.custom2.intValue());
	}
	
}
