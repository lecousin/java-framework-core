package net.lecousin.framework.core.tests.math;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.math.RangeBigInteger;
import net.lecousin.framework.math.RangeInteger;
import net.lecousin.framework.math.RangeLong;
import net.lecousin.framework.util.Pair;

public class TestRanges extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void testRangeInteger() throws Exception {
		RangeInteger r1 = new RangeInteger(10, 20);
		RangeInteger r2 = new RangeInteger(15, 25);
		
		Assert.assertEquals(10, r1.min);
		Assert.assertEquals(20, r1.max);

		// copy
		RangeInteger cr1 = new RangeInteger(r1);
		Assert.assertEquals(10, cr1.min);
		Assert.assertEquals(20, cr1.max);
		cr1 = r1.copy();
		Assert.assertEquals(10, cr1.min);
		Assert.assertEquals(20, cr1.max);
		
		// equals
		Assert.assertTrue(r1.equals(cr1));
		Assert.assertFalse(r1.equals(r2));
		Assert.assertFalse(r1.equals(new RangeInteger(10, 21)));
		Assert.assertFalse(r1.equals(new RangeInteger(11, 20)));
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
		inter = r1.intersect(new RangeInteger(3, 5));
		Assert.assertNull(inter);
		
		// removeIntersect
		Assert.assertEquals(new Pair<>(null, new RangeInteger(151, 200)), new RangeInteger(100, 200).removeIntersect(new RangeInteger(50, 150)));
		Assert.assertEquals(new Pair<>(new RangeInteger(100, 149), null), new RangeInteger(100, 200).removeIntersect(new RangeInteger(150, 220)));
		Assert.assertEquals(new Pair<>(new RangeInteger(100, 200), null), new RangeInteger(100, 200).removeIntersect(new RangeInteger(50, 75)));
		Assert.assertEquals(new Pair<>(new RangeInteger(100, 200), null), new RangeInteger(100, 200).removeIntersect(new RangeInteger(300, 400)));
		Assert.assertEquals(new Pair<>(new RangeInteger(100, 124), new RangeInteger(151, 200)), new RangeInteger(100, 200).removeIntersect(new RangeInteger(125, 150)));
		Assert.assertEquals(new Pair<>(null, new RangeInteger(151, 200)), new RangeInteger(100, 200).removeIntersect(new RangeInteger(100, 150)));
		Assert.assertEquals(new Pair<>(new RangeInteger(100, 149), null), new RangeInteger(100, 200).removeIntersect(new RangeInteger(150, 200)));
		Assert.assertEquals(new Pair<>(null, null), new RangeInteger(100, 200).removeIntersect(new RangeInteger(100, 200)));
		
		// length
		Assert.assertEquals(11, r1.getLength());
		Assert.assertEquals(11, r2.getLength());
		
		// toString
		Assert.assertEquals("[10-20]", r1.toString());
		
		// Parser
		Assert.assertEquals(new RangeInteger(100, 100), new RangeInteger.Parser().parse("100"));
		Assert.assertEquals(new RangeInteger(100, 200), new RangeInteger.Parser().parse("[100-200]"));
		Assert.assertEquals(new RangeInteger(101, 200), new RangeInteger.Parser().parse("]100-200]"));
		Assert.assertEquals(new RangeInteger(100, 199), new RangeInteger.Parser().parse("[100-200["));
		Assert.assertEquals(new RangeInteger(101, 199), new RangeInteger.Parser().parse("]100-200["));
		Assert.assertEquals(new RangeInteger(50, 150), new RangeInteger.Parser().parse("[150-50]"));
		try { new RangeInteger.Parser().parse(null); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeInteger.Parser().parse(""); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeInteger.Parser().parse("hello"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeInteger.Parser().parse("[150"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeInteger.Parser().parse("[150-"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeInteger.Parser().parse("[150-]"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeInteger.Parser().parse("[150-200"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeInteger.Parser().parse("[150-hello]"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeInteger.Parser().parse("[hello-200]"); throw new AssertionError("Error case"); } catch (Exception e) {}
	}

	@Test(timeout=30000)
	public void testRangeLong() throws Exception {
		RangeLong r1 = new RangeLong(10, 20);
		RangeLong r2 = new RangeLong(15, 25);
		
		Assert.assertEquals(10, r1.min);
		Assert.assertEquals(20, r1.max);

		// copy
		RangeLong cr1 = new RangeLong(r1);
		Assert.assertEquals(10, cr1.min);
		Assert.assertEquals(20, cr1.max);
		cr1 = r1.copy();
		Assert.assertEquals(10, cr1.min);
		Assert.assertEquals(20, cr1.max);
		
		// equals
		Assert.assertTrue(r1.equals(cr1));
		Assert.assertFalse(r1.equals(new RangeLong(10, 21)));
		Assert.assertFalse(r1.equals(new RangeLong(11, 20)));
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
		inter = r1.intersect(new RangeLong(3, 5));
		Assert.assertNull(inter);
		
		// removeIntersect
		Assert.assertEquals(new Pair<>(null, new RangeLong(151, 200)), new RangeLong(100, 200).removeIntersect(new RangeLong(50, 150)));
		Assert.assertEquals(new Pair<>(new RangeLong(100, 149), null), new RangeLong(100, 200).removeIntersect(new RangeLong(150, 220)));
		Assert.assertEquals(new Pair<>(new RangeLong(100, 200), null), new RangeLong(100, 200).removeIntersect(new RangeLong(50, 75)));
		Assert.assertEquals(new Pair<>(new RangeLong(100, 200), null), new RangeLong(100, 200).removeIntersect(new RangeLong(300, 400)));
		Assert.assertEquals(new Pair<>(new RangeLong(100, 124), new RangeLong(151, 200)), new RangeLong(100, 200).removeIntersect(new RangeLong(125, 150)));
		Assert.assertEquals(new Pair<>(null, new RangeLong(151, 200)), new RangeLong(100, 200).removeIntersect(new RangeLong(100, 150)));
		Assert.assertEquals(new Pair<>(new RangeLong(100, 149), null), new RangeLong(100, 200).removeIntersect(new RangeLong(150, 200)));
		Assert.assertEquals(new Pair<>(null, null), new RangeLong(100, 200).removeIntersect(new RangeLong(100, 200)));
		
		// length
		Assert.assertEquals(11, r1.getLength());
		Assert.assertEquals(11, r2.getLength());
		
		// toString
		Assert.assertEquals("[10-20]", r1.toString());
		
		// Parser
		Assert.assertEquals(new RangeLong(100, 100), new RangeLong.Parser().parse("100"));
		Assert.assertEquals(new RangeLong(100, 200), new RangeLong.Parser().parse("[100-200]"));
		Assert.assertEquals(new RangeLong(101, 200), new RangeLong.Parser().parse("]100-200]"));
		Assert.assertEquals(new RangeLong(100, 199), new RangeLong.Parser().parse("[100-200["));
		Assert.assertEquals(new RangeLong(101, 199), new RangeLong.Parser().parse("]100-200["));
		Assert.assertEquals(new RangeLong(50, 150), new RangeLong.Parser().parse("[150-50]"));
		try { new RangeLong.Parser().parse(null); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeLong.Parser().parse(""); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeLong.Parser().parse("hello"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeLong.Parser().parse("[150"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeLong.Parser().parse("[150-"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeLong.Parser().parse("[150-]"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeLong.Parser().parse("[150-200"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeLong.Parser().parse("[150-hello]"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeLong.Parser().parse("[hello-200]"); throw new AssertionError("Error case"); } catch (Exception e) {}
	}

	@Test(timeout=30000)
	public void testRangeBigInteger() throws Exception {
		RangeBigInteger r1 = new RangeBigInteger(BigInteger.valueOf(10), BigInteger.valueOf(20));
		RangeBigInteger r2 = new RangeBigInteger(BigInteger.valueOf(15), BigInteger.valueOf(25));
		
		Assert.assertEquals(10, r1.min.longValue());
		Assert.assertEquals(20, r1.max.longValue());

		// copy
		RangeBigInteger cr1 = new RangeBigInteger(r1);
		Assert.assertEquals(10, cr1.min.longValue());
		Assert.assertEquals(20, cr1.max.longValue());
		cr1 = r1.copy();
		Assert.assertEquals(10, cr1.min.longValue());
		Assert.assertEquals(20, cr1.max.longValue());
		
		// equals
		Assert.assertTrue(r1.equals(cr1));
		Assert.assertFalse(r1.equals(new RangeBigInteger(BigInteger.valueOf(10), BigInteger.valueOf(21))));
		Assert.assertFalse(r1.equals(new RangeBigInteger(BigInteger.valueOf(11), BigInteger.valueOf(20))));
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
		inter = r1.intersect(new RangeBigInteger(BigInteger.valueOf(3), BigInteger.valueOf(5)));
		Assert.assertNull(inter);
		
		// removeIntersect
		Assert.assertEquals(new Pair<>(null, new RangeBigInteger(BigInteger.valueOf(151), BigInteger.valueOf(200))), new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)).removeIntersect(new RangeBigInteger(BigInteger.valueOf(50), BigInteger.valueOf(150))));
		Assert.assertEquals(new Pair<>(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(149)), null), new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)).removeIntersect(new RangeBigInteger(BigInteger.valueOf(150), BigInteger.valueOf(220))));
		Assert.assertEquals(new Pair<>(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)), null), new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)).removeIntersect(new RangeBigInteger(BigInteger.valueOf(50), BigInteger.valueOf(75))));
		Assert.assertEquals(new Pair<>(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)), null), new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)).removeIntersect(new RangeBigInteger(BigInteger.valueOf(300), BigInteger.valueOf(400))));
		Assert.assertEquals(new Pair<>(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(124)), new RangeBigInteger(BigInteger.valueOf(151), BigInteger.valueOf(200))), new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)).removeIntersect(new RangeBigInteger(BigInteger.valueOf(125), BigInteger.valueOf(150))));
		Assert.assertEquals(new Pair<>(null, new RangeBigInteger(BigInteger.valueOf(151), BigInteger.valueOf(200))), new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)).removeIntersect(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(150))));
		Assert.assertEquals(new Pair<>(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(149)), null), new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)).removeIntersect(new RangeBigInteger(BigInteger.valueOf(150), BigInteger.valueOf(200))));
		Assert.assertEquals(new Pair<>(null, null), new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)).removeIntersect(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200))));
		
		// length
		Assert.assertEquals(11, r1.getLength().longValue());
		Assert.assertEquals(11, r2.getLength().longValue());
		
		// toString
		Assert.assertEquals("[10-20]", r1.toString());
		
		// Parser
		Assert.assertEquals(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(100)), new RangeBigInteger.Parser().parse("100"));
		Assert.assertEquals(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(200)), new RangeBigInteger.Parser().parse("[100-200]"));
		Assert.assertEquals(new RangeBigInteger(BigInteger.valueOf(101), BigInteger.valueOf(200)), new RangeBigInteger.Parser().parse("]100-200]"));
		Assert.assertEquals(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(199)), new RangeBigInteger.Parser().parse("[100-200["));
		Assert.assertEquals(new RangeBigInteger(BigInteger.valueOf(101), BigInteger.valueOf(199)), new RangeBigInteger.Parser().parse("]100-200["));
		Assert.assertEquals(new RangeBigInteger(BigInteger.valueOf(50), BigInteger.valueOf(150)), new RangeBigInteger.Parser().parse("[150-50]"));
		try { new RangeBigInteger.Parser().parse(null); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeBigInteger.Parser().parse(""); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeBigInteger.Parser().parse("hello"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeBigInteger.Parser().parse("[150"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeBigInteger.Parser().parse("[150-"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeBigInteger.Parser().parse("[150-]"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeBigInteger.Parser().parse("[150-200"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeBigInteger.Parser().parse("[150-hello]"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { new RangeBigInteger.Parser().parse("[hello-200]"); throw new AssertionError("Error case"); } catch (Exception e) {}
	}
	
}
