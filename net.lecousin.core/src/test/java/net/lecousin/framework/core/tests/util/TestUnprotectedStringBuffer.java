package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.util.TestIString;
import net.lecousin.framework.util.IString;
import net.lecousin.framework.util.Provider.FromValue;
import net.lecousin.framework.util.UnprotectedStringBuffer;

import org.junit.Assert;
import org.junit.Test;

public class TestUnprotectedStringBuffer extends TestIString {

	@Override
	protected IString createString(String s) {
		return new UnprotectedStringBuffer(s);
	}
	
	@Test
	public void testModifications() {
		UnprotectedStringBuffer s = new UnprotectedStringBuffer("Hello");
		s.addFirst(' ');
		Assert.assertEquals(" Hello", s.asString());
		s.addFirst("World");
		Assert.assertEquals("World Hello", s.asString());
		Assert.assertEquals("Worxxd Hexxxxo", s.replace('l', "xx").asString());
		Assert.assertEquals("Worxxyzyexxxxo", s.replace(5, 7, new UnprotectedStringBuffer("yzy")).asString());
		FromValue<UnprotectedStringBuffer, UnprotectedStringBuffer> provider = new FromValue<UnprotectedStringBuffer, UnprotectedStringBuffer>() {
			@Override
			public UnprotectedStringBuffer provide(UnprotectedStringBuffer value) {
				return new UnprotectedStringBuffer(Integer.toString(Integer.parseInt(value.asString()) * 2));
			}
		}; 
		s = new UnprotectedStringBuffer("abcd${123}efgh${200}ijk");
		s.searchAndReplace("${", "}", provider);
		Assert.assertEquals("abcd246efgh400ijk", s.asString());
		s = new UnprotectedStringBuffer("${34}abcd${123}efgh${200}ijk${99}");
		s.searchAndReplace("${", "}", provider);
		Assert.assertEquals("68abcd246efgh400ijk198", s.asString());
		s = new UnprotectedStringBuffer("${3");
		s.searchAndReplace("${", "}", provider);
		Assert.assertEquals("${3", s.asString());
	}
	
}
