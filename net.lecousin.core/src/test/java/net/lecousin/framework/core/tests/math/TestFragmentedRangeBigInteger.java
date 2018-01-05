package net.lecousin.framework.core.tests.math;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.math.FragmentedRangeBigInteger;
import net.lecousin.framework.math.RangeBigInteger;

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
	}
	
}
