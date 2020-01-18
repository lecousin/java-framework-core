package net.lecousin.framework.core.test.text;

import net.lecousin.framework.text.ArrayString;
import net.lecousin.framework.text.CharArrayStringBuffer;

import org.junit.Assert;
import org.junit.Test;

public abstract class TestArrayString extends TestIString {

	@Override
	protected abstract ArrayString createString(String s);
	
	
	@Test
	public void testSimple() {
		ArrayString s = createString("");
		Assert.assertEquals(-1, s.firstChar());
		Assert.assertEquals(-1, s.lastChar());
		
		s.append("Hello").append(' ').append("World").append('!');
		Assert.assertEquals('H', s.firstChar());
		Assert.assertEquals('!', s.lastChar());
		
		s.trimToSize();
		Assert.assertEquals("Hello World!", s.asString());
		Assert.assertEquals(2, s.countChar('o'));
		Assert.assertEquals(3, s.countChar('l'));
		Assert.assertEquals(1, s.countChar('d'));
		Assert.assertEquals(0, s.countChar('x'));
		
		s = createString("");
		s.append('a');
		Assert.assertEquals(1, s.length());
		s.reset();
		Assert.assertEquals(0, s.length());
		s.append("abcdef");
		Assert.assertEquals(6, s.length());
		s.moveForward(2);
		Assert.assertEquals(4, s.length());
		Assert.assertEquals("cdef", s.toString());
	}

	@Override
	@Test
	public void testAppend() {
		super.testAppend();
		ArrayString s = createString("");
		ArrayString s1 = createString("Hello");
		CharArrayStringBuffer s2 = new CharArrayStringBuffer(" World");
		s2.append('!');
		s.append(s1);
		s.append(s2);
		Assert.assertEquals("Hello World!", s.asString());
	}

}
