package net.lecousin.framework.core.tests.text;

import java.nio.ByteBuffer;

import net.lecousin.framework.core.test.text.TestArrayStringBuffer;
import net.lecousin.framework.io.data.ByteArray;
import net.lecousin.framework.text.ByteArrayStringIso8859Buffer;

import org.junit.Assert;
import org.junit.Test;

public class TestByteArrayStringIso8859Buffer extends TestArrayStringBuffer<ByteArrayStringIso8859Buffer> {

	@Override
	protected ByteArrayStringIso8859Buffer createString(String s) {
		return new ByteArrayStringIso8859Buffer(s);
	}
	
	@Test
	public void testAppendBytes() {
		ByteArrayStringIso8859Buffer s = new ByteArrayStringIso8859Buffer((CharSequence)"ab");
		s.append((byte)'c');
		Assert.assertEquals("abc", s.asString());
		s = new ByteArrayStringIso8859Buffer(s);
		s.append((byte)'d');
		Assert.assertEquals("abcd", s.asString());
		s = new ByteArrayStringIso8859Buffer();
		s.setNewArrayStringCapacity(3);
		s.append((byte)'z');
		Assert.assertEquals("z", s.asString());
		s.append((byte)'y');
		Assert.assertEquals("zy", s.asString());
		s.append((byte)'x');
		Assert.assertEquals("zyx", s.asString());
		s.append((byte)'w');
		Assert.assertEquals("zyxw", s.asString());
		s.append((byte)'v');
		Assert.assertEquals("zyxwv", s.asString());
	}
	
	@Test
	public void testAsByteArrays() {
		check(createString("").asByteArrays(), "");
		check(createString("salut").asByteArrays(), "salut");
		check(new ByteArrayStringIso8859Buffer().asByteArrays(), "");
	}
	
	private static void check(ByteArray[] arrays, String expected) {
		StringBuilder s = new StringBuilder();
		for (ByteArray a : arrays)
			while (a.hasRemaining())
				s.append((char)(a.get() & 0xFF));
		Assert.assertEquals(expected, s.toString());
	}
	
	@Test
	public void testAsByteBuffers() {
		check(createString("").asByteBuffers(), "");
		check(createString("salut").asByteBuffers(), "salut");
		check(new ByteArrayStringIso8859Buffer().asByteBuffers(), "");
	}
	
	private static void check(ByteBuffer[] arrays, String expected) {
		StringBuilder s = new StringBuilder();
		for (ByteBuffer a : arrays)
			while (a.hasRemaining())
				s.append((char)(a.get() & 0xFF));
		Assert.assertEquals(expected, s.toString());
	}
	
}
