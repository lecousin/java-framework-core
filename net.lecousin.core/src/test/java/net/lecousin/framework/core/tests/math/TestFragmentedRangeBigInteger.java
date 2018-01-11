package net.lecousin.framework.core.tests.math;

import java.math.BigInteger;
import java.util.Arrays;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.math.FragmentedRangeBigInteger;
import net.lecousin.framework.math.RangeBigInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestFragmentedRangeBigInteger extends LCCoreAbstractTest {

	@Test
	public void test() {
		FragmentedRangeBigInteger f = new FragmentedRangeBigInteger();
		Assert.assertEquals(0, f.size());
		f.addRange(new RangeBigInteger(BigInteger.valueOf(10), BigInteger.valueOf(15)));
		Assert.assertEquals(1, f.size());
		f.addRange(new RangeBigInteger(BigInteger.valueOf(16), BigInteger.valueOf(20)));
		Assert.assertEquals(1, f.size());
		f.addRange(new RangeBigInteger(BigInteger.valueOf(22), BigInteger.valueOf(30)));
		Assert.assertEquals(2, f.size());
		f.addValue(BigInteger.valueOf(21));
		Assert.assertEquals(1, f.size());
		f.addValue(BigInteger.valueOf(9));
		Assert.assertEquals(1, f.size());
		f.addValue(BigInteger.valueOf(31));
		Assert.assertEquals(1, f.size());

		f.addRanges(Arrays.asList(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(150)), new RangeBigInteger(BigInteger.valueOf(200), BigInteger.valueOf(250))));
		Assert.assertEquals(3, f.size());
		f.addRange(BigInteger.valueOf(175), BigInteger.valueOf(180));
		Assert.assertEquals(4, f.size());
		
		Assert.assertTrue(f.containsValue(9));
		Assert.assertTrue(f.containsValue(10));
		Assert.assertTrue(f.containsValue(100));
		Assert.assertTrue(f.containsValue(110));
		Assert.assertFalse(f.containsValue(0));
		Assert.assertFalse(f.containsValue(8));
		Assert.assertFalse(f.containsValue(50));
		Assert.assertFalse(f.containsValue(160));
		
		Assert.assertTrue(f.containsRange(BigInteger.valueOf(100), BigInteger.valueOf(150)));
		Assert.assertTrue(f.containsRange(BigInteger.valueOf(101), BigInteger.valueOf(150)));
		Assert.assertTrue(f.containsRange(BigInteger.valueOf(110), BigInteger.valueOf(120)));
		Assert.assertFalse(f.containsRange(BigInteger.valueOf(99), BigInteger.valueOf(120)));
		Assert.assertFalse(f.containsRange(BigInteger.valueOf(130), BigInteger.valueOf(160)));
	}
	
}
