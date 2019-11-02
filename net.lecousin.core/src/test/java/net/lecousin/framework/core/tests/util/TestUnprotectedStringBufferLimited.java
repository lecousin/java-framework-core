package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.UnprotectedStringBuffer;
import net.lecousin.framework.util.UnprotectedStringBufferLimited;

import org.junit.Assert;
import org.junit.Test;

public class TestUnprotectedStringBufferLimited extends LCCoreAbstractTest {

	@Test
	public void testAppendCharacter() {
		UnprotectedStringBufferLimited s = new UnprotectedStringBufferLimited(20);
		Assert.assertEquals(20, s.getMaxSize());
		
		s.append('a');
		Assert.assertEquals(1, s.length());
		Assert.assertEquals("a", s.toString());
		
		for (int i = 0; i < 12; ++i)
			s.append('b');
		Assert.assertEquals(13, s.length());
		Assert.assertEquals("abbbbbbbbbbbb", s.toString());
		
		for (int i = 0; i < 12; ++i)
			s.append('c');
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("abbbbbbbbbbbbccccccc", s.toString());
	}
	
	@Test
	public void testAppendChars() {
		UnprotectedStringBufferLimited s = new UnprotectedStringBufferLimited(20);
		
		s.append(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' });
		Assert.assertEquals(6, s.length());
		Assert.assertEquals("abcdef", s.toString());
		s.append(new char[] {});
		Assert.assertEquals(6, s.length());
		Assert.assertEquals("abcdef", s.toString());
		s.append(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' });
		Assert.assertEquals(12, s.length());
		Assert.assertEquals("abcdefabcdef", s.toString());
		s.append(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' });
		Assert.assertEquals(18, s.length());
		Assert.assertEquals("abcdefabcdefabcdef", s.toString());
		s.append(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' });
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("abcdefabcdefabcdefab", s.toString());
		s.append(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' });
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("abcdefabcdefabcdefab", s.toString());
	}
	
	@Test
	public void testAppendCharSequence() {
		UnprotectedStringBufferLimited s = new UnprotectedStringBufferLimited(20);
		
		s.append("abcdef");
		Assert.assertEquals(6, s.length());
		Assert.assertEquals("abcdef", s.toString());
		s.append("");
		Assert.assertEquals(6, s.length());
		Assert.assertEquals("abcdef", s.toString());
		s.append("abcdef");
		Assert.assertEquals(12, s.length());
		Assert.assertEquals("abcdefabcdef", s.toString());
		s.append("abcdef");
		Assert.assertEquals(18, s.length());
		Assert.assertEquals("abcdefabcdefabcdef", s.toString());
		s.append("abcdef");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("abcdefabcdefabcdefab", s.toString());
		s.append("abcdef");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("abcdefabcdefabcdefab", s.toString());
	}
	
	@Test
	public void testAddFirst() {
		UnprotectedStringBufferLimited s = new UnprotectedStringBufferLimited(20);
		
		s.addFirst("abcdef");
		Assert.assertEquals(6, s.length());
		Assert.assertEquals("abcdef", s.toString());
		s.addFirst("");
		Assert.assertEquals(6, s.length());
		Assert.assertEquals("abcdef", s.toString());
		s.addFirst("123456");
		Assert.assertEquals(12, s.length());
		Assert.assertEquals("123456abcdef", s.toString());
		s.addFirst("abcdef");
		Assert.assertEquals(18, s.length());
		Assert.assertEquals("abcdef123456abcdef", s.toString());
		s.addFirst("987654");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("98abcdef123456abcdef", s.toString());
		s.addFirst("abcdef");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("98abcdef123456abcdef", s.toString());
	}
	
	@Test
	public void testTrim() {
		UnprotectedStringBufferLimited s = new UnprotectedStringBufferLimited(20);
		
		s.append("  123456789  ");
		Assert.assertEquals(13, s.length());
		Assert.assertEquals("  123456789  ", s.toString());
		s.append("abcdefghij");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("  123456789  abcdefg", s.toString());
		s.trimBeginning();
		Assert.assertEquals(18, s.length());
		Assert.assertEquals("123456789  abcdefg", s.toString());
		s.trimEnd();
		Assert.assertEquals(18, s.length());
		Assert.assertEquals("123456789  abcdefg", s.toString());
		s.append("     ");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("123456789  abcdefg  ", s.toString());
		s.trimEnd();
		Assert.assertEquals(18, s.length());
		Assert.assertEquals("123456789  abcdefg", s.toString());
		s.addFirst(" $");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals(" $123456789  abcdefg", s.toString());
		s.trimBeginning();
		Assert.assertEquals(19, s.length());
		Assert.assertEquals("$123456789  abcdefg", s.toString());
	}
	
	@Test
	public void testRemoveChars() {
		UnprotectedStringBufferLimited s = new UnprotectedStringBufferLimited(20);
		
		s.append("abcdefghijklmnopqrstuvwxyz");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("abcdefghijklmnopqrst", s.toString());
		s.removeStartChars(4);
		Assert.assertEquals(16, s.length());
		Assert.assertEquals("efghijklmnopqrst", s.toString());
		s.removeStartChars(0);
		Assert.assertEquals(16, s.length());
		Assert.assertEquals("efghijklmnopqrst", s.toString());
		s.removeEndChars(5);
		Assert.assertEquals(11, s.length());
		Assert.assertEquals("efghijklmno", s.toString());
		s.removeEndChars(0);
		Assert.assertEquals(11, s.length());
		Assert.assertEquals("efghijklmno", s.toString());
		s.append("0123456789");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("efghijklmno012345678", s.toString());
		s.removeStartChars(100);
		Assert.assertEquals(0, s.length());
		Assert.assertEquals("", s.toString());
		s.append("abcdefghijklmnopqrstuvwxyz");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("abcdefghijklmnopqrst", s.toString());
		s.removeEndChars(100);
		Assert.assertEquals(0, s.length());
		Assert.assertEquals("", s.toString());
	}
	
	@Test
	public void testReplace() {
		UnprotectedStringBufferLimited s = new UnprotectedStringBufferLimited(20);
		
		s.append("0123456789");
		Assert.assertEquals(10, s.length());
		Assert.assertEquals("0123456789", s.toString());
		s.append("0123456789");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("01234567890123456789", s.toString());
		s.replace("23", "z");
		Assert.assertEquals(18, s.length());
		Assert.assertEquals("01z45678901z456789", s.toString());
		s.append("abcd");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("01z45678901z456789ab", s.toString());
		s.replace(5, 9, new UnprotectedStringBuffer("y"));
		Assert.assertEquals(16, s.length());
		Assert.assertEquals("01z45y1z456789ab", s.toString());
		s.replace("y", "0123456789");
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("01z4501234567891z456", s.toString());
		s.replace(10, 12, new UnprotectedStringBuffer("0123456789"));
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("01z45012340123456789", s.toString());
		s.searchAndReplace("z", "40", content -> new UnprotectedStringBuffer("tptptptp"));
		Assert.assertEquals(19, s.length());
		Assert.assertEquals("01tptptptp123456789", s.toString());
		s.searchAndReplace("12", "56", content -> new UnprotectedStringBuffer("abcdefghijklmnop"));
		Assert.assertEquals(20, s.length());
		Assert.assertEquals("01tptptptpabcdefghij", s.toString());
	}
	
}
