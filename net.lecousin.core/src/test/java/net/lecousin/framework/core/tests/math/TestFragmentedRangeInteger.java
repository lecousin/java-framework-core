package net.lecousin.framework.core.tests.math;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.math.FragmentedRangeInteger;
import net.lecousin.framework.math.RangeInteger;

public class TestFragmentedRangeInteger extends LCCoreAbstractTest {

	@Test
	public void test() {
		FragmentedRangeInteger f = new FragmentedRangeInteger();
		Assert.assertEquals(0, f.size());
		f.addRange(new RangeInteger(10, 15));
		Assert.assertEquals(1, f.size());
		f.addRange(new RangeInteger(16, 20));
		Assert.assertEquals(1, f.size());
		f.addRange(new RangeInteger(22, 30));
		Assert.assertEquals(2, f.size());
		f.addValue(21);
		Assert.assertEquals(1, f.size());
		f.addValue(9);
		Assert.assertEquals(1, f.size());
		f.addValue(31);
		Assert.assertEquals(1, f.size());
	}
	
}
