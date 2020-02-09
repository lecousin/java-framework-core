package net.lecousin.framework.core.tests.encoding;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.encoding.Base64Encoding;
import net.lecousin.framework.encoding.EncodingException;
import net.lecousin.framework.encoding.StringEncoding;
import net.lecousin.framework.encoding.StringEncoding.EncodedLong;

import org.junit.Assert;
import org.junit.Test;

public class TestStringEncoding extends LCCoreAbstractTest {

	@Test
	public void testSimpleInteger() throws Exception {
		Assert.assertEquals("9876", new StringEncoding.SimpleInteger().encode(Integer.valueOf(9876)));
		Assert.assertEquals("-852", new StringEncoding.SimpleInteger().encode(Integer.valueOf(-852)));
		Assert.assertEquals(9876, new StringEncoding.SimpleInteger().decode("9876").intValue());
		Assert.assertEquals(-852, new StringEncoding.SimpleInteger().decode("-852").intValue());
		try {
			new StringEncoding.SimpleInteger().decode(")");
			throw new AssertionError("Exception expected");
		} catch (EncodingException e) {
			// ok
		}
	}

	@Test
	public void testSimpleLong() throws Exception {
		Assert.assertEquals("9876", new StringEncoding.SimpleLong().encode(Long.valueOf(9876)));
		Assert.assertEquals("-852", new StringEncoding.SimpleLong().encode(Long.valueOf(-852)));
		Assert.assertEquals(9876, new StringEncoding.SimpleLong().decode("9876").intValue());
		Assert.assertEquals(-852, new StringEncoding.SimpleLong().decode("-852").intValue());
		try {
			new StringEncoding.SimpleLong().decode(")");
			throw new AssertionError("Exception expected");
		} catch (EncodingException e) {
			// ok
		}
	}
	
	@Test
	public void testEncodedLong() throws Exception {
		EncodedLong e = new StringEncoding.EncodedLong(Base64Encoding.instance, Base64Encoding.instance);
		String s = e.encode(Long.valueOf(8425671L));
		Assert.assertEquals(8425671L, e.decode(s).longValue());
		s = e.encode(Long.valueOf(-662211L));
		Assert.assertEquals(-662211L, e.decode(s).longValue());
	}
	
}
