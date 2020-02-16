package net.lecousin.framework.core.tests.text;

import java.util.function.UnaryOperator;

import net.lecousin.framework.core.test.text.TestArrayStringBuffer;
import net.lecousin.framework.text.CharArrayString;
import net.lecousin.framework.text.CharArrayStringBuffer;

import org.junit.Assert;
import org.junit.Test;

public class TestCharArrayStringBuffer extends TestArrayStringBuffer<CharArrayString, CharArrayStringBuffer> {

	@Override
	protected CharArrayStringBuffer createString(String s) {
		return s.length() > 0 ? new CharArrayStringBuffer(s) : new CharArrayStringBuffer();
	}
	
	@Override
	protected CharArrayString createSimpleString(String s) {
		return new CharArrayString(s);
	}
	
	@Override
	@Test
	public void testModifications() {
		super.testModifications();
		UnaryOperator<CharArrayStringBuffer> provider = value -> new CharArrayStringBuffer(Integer.toString(Integer.parseInt(value.asString()) * 2));
		CharArrayStringBuffer s = new CharArrayStringBuffer(new CharArrayStringBuffer("abcd${123}efgh${200}ijk"));
		s.searchAndReplace("${", "}", provider);
		Assert.assertEquals("abcd246efgh400ijk", s.asString());
		s = new CharArrayStringBuffer("${34}abcd${123}efgh${200}ijk${99}");
		s.searchAndReplace("${", "}", provider);
		Assert.assertEquals("68abcd246efgh400ijk198", s.asString());
		s = new CharArrayStringBuffer("${3");
		s.searchAndReplace("${", "}", provider);
		Assert.assertEquals("${3", s.asString());
		Assert.assertTrue(s.equals(s));
		CharArrayStringBuffer s2 = new CharArrayStringBuffer();
		Assert.assertFalse(s.equals(s2));
		s2.append("${3");
		Assert.assertTrue(s.equals(s2));
		s.setNewArrayStringCapacity(16);
		s.reset();
		Assert.assertFalse(s.equals(s2));
		s2.reset();
		Assert.assertTrue(s.equals(s2));
	}
	
}
