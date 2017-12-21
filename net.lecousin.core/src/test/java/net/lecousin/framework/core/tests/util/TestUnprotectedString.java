package net.lecousin.framework.core.tests.util;

import java.util.Arrays;
import java.util.Collection;

import net.lecousin.framework.core.test.util.TestIString;
import net.lecousin.framework.util.IString;
import net.lecousin.framework.util.UnprotectedString;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestUnprotectedString extends TestIString {

	@Parameters(name = "start = {0}, end = {1}")
	public static Collection<Object[]> parameters() {
		return Arrays.asList(
			new Object[] { 0, 0 },
			new Object[] { 1, 0 },
			new Object[] { 0, 1 },
			new Object[] { 1, 1 },
			new Object[] { 10, 0 },
			new Object[] { 0, 10 },
			new Object[] { 10, 10 }
		);
	}
	
	public TestUnprotectedString(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	protected int start;
	protected int end;
	
	@Override
	protected IString createString(String s) {
		char[] chars = new char[s.length() + start + end];
		char[] c = s.toCharArray();
		for (int i = 0; i < c.length; ++i)
			chars[i + start] = c[i];
		return new UnprotectedString(chars, start, c.length, c.length + end);
	}
	
}
