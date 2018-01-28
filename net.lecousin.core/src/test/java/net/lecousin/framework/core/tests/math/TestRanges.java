package net.lecousin.framework.core.tests.math;

import java.math.BigInteger;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.math.RangeBigInteger;
import net.lecousin.framework.math.RangeInteger;
import net.lecousin.framework.math.RangeLong;

import org.junit.Assert;
import org.junit.Test;

public class TestRanges extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void testRangeInteger() {
		RangeInteger r1 = new RangeInteger(10, 20);
		RangeInteger r2 = new RangeInteger(15, 25);
		
		Assert.assertEquals(10, r1.min);
		Assert.assertEquals(20, r1.max);

		// copy
		RangeInteger cr1 = new RangeInteger(r1);
		Assert.assertEquals(10, cr1.min);
		Assert.assertEquals(20, cr1.max);
		
		// equals
		Assert.assertTrue(r1.equals(cr1));
		Assert.assertFalse(r1.equals(r2));
		Assert.assertFalse(r1.equals(new Object()));
		Assert.assertFalse(r1.equals(null));
		
		// hashCode
		Assert.assertEquals(r1.hashCode(), cr1.hashCode());
		
		// contains
		for (int i = 0; i < 50; ++i)
			if (i <10 || i > 20)
				Assert.assertFalse(Integer.toString(i), r1.contains(i));
			else
				Assert.assertTrue(Integer.toString(i), r1.contains(i));
		
		// intersect
		RangeInteger inter = r1.intersect(r2);
		Assert.assertEquals(15, inter.min);
		Assert.assertEquals(20, inter.max);
		inter = r2.intersect(r1);
		Assert.assertEquals(15, inter.min);
		Assert.assertEquals(20, inter.max);
		inter = r1.intersect(cr1);
		Assert.assertEquals(10, inter.min);
		Assert.assertEquals(20, inter.max);
		inter = r1.intersect(new RangeInteger(100, 200));
		Assert.assertNull(inter);
		
		// length
		Assert.assertEquals(11, r1.getLength());
		Assert.assertEquals(11, r2.getLength());
		
		// toString
		Assert.assertEquals("[10-20]", r1.toString());
	}

	@Test(timeout=30000)
	public void testRangeLong() {
		RangeLong r1 = new RangeLong(10, 20);
		RangeLong r2 = new RangeLong(15, 25);
		
		Assert.assertEquals(10, r1.min);
		Assert.assertEquals(20, r1.max);

		// copy
		RangeLong cr1 = new RangeLong(r1);
		Assert.assertEquals(10, cr1.min);
		Assert.assertEquals(20, cr1.max);
		
		// equals
		Assert.assertTrue(r1.equals(cr1));
		Assert.assertFalse(r1.equals(r2));
		Assert.assertFalse(r1.equals(new Object()));
		Assert.assertFalse(r1.equals(null));
		
		// hashCode
		Assert.assertEquals(r1.hashCode(), cr1.hashCode());
		
		// contains
		for (int i = 0; i < 50; ++i)
			if (i <10 || i > 20)
				Assert.assertFalse(Integer.toString(i), r1.contains(i));
			else
				Assert.assertTrue(Integer.toString(i), r1.contains(i));
		
		// intersect
		RangeLong inter = r1.intersect(r2);
		Assert.assertEquals(15, inter.min);
		Assert.assertEquals(20, inter.max);
		inter = r2.intersect(r1);
		Assert.assertEquals(15, inter.min);
		Assert.assertEquals(20, inter.max);
		inter = r1.intersect(cr1);
		Assert.assertEquals(10, inter.min);
		Assert.assertEquals(20, inter.max);
		inter = r1.intersect(new RangeLong(100, 200));
		Assert.assertNull(inter);
		
		// length
		Assert.assertEquals(11, r1.getLength());
		Assert.assertEquals(11, r2.getLength());
		
		// toString
		Assert.assertEquals("[10-20]", r1.toString());
	}

	@Test(timeout=30000)
	public void testRangeBigInteger() {
		RangeBigInteger r1 = new RangeBigInteger(BigInteger.valueOf(10), BigInteger.valueOf(20));
		RangeBigInteger r2 = new RangeBigInteger(BigInteger.valueOf(15), BigInteger.valueOf(25));
		
		Assert.assertEquals(10, r1.min.longValue());
		Assert.assertEquals(20, r1.max.longValue());

		// copy
		RangeBigInteger cr1 = new RangeBigInteger(r1);
		Assert.assertEquals(10, cr1.min.longValue());
		Assert.assertEquals(20, cr1.max.longValue());
		
		// equals
		Assert.assertTrue(r1.equals(cr1));
		Assert.assertFalse(r1.equals(r2));
		Assert.assertFalse(r1.equals(new Object()));
		Assert.assertFalse(r1.equals(null));
		
		// hashCode
		Assert.assertEquals(r1.hashCode(), cr1.hashCode());
		
		// contains
		for (int i = 0; i < 50; ++i)
			if (i <10 || i > 20)
				Assert.assertFalse(Integer.toString(i), r1.contains(BigInteger.valueOf(i)));
			else
				Assert.assertTrue(Integer.toString(i), r1.contains(BigInteger.valueOf(i)));
		
		// intersect
		RangeBigInteger inter = r1.intersect(r2);
		Assert.assertEquals(15, inter.min.longValue());
		Assert.assertEquals(20, inter.max.longValue());
		inter = r2.intersect(r1);
		Assert.assertEquals(15, inter.min.longValue());
		Assert.assertEquals(20, inter.max.longValue());
		inter = r1.intersect(cr1);
		Assert.assertEquals(10, inter.min.longValue());
		Assert.assertEquals(20, inter.max.longValue());
		inter = r1.intersect(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)));
		Assert.assertNull(inter);
		
		// length
		Assert.assertEquals(11, r1.getLength().longValue());
		Assert.assertEquals(11, r2.getLength().longValue());
		
		// toString
		Assert.assertEquals("[10-20]", r1.toString());
	}
	
}
