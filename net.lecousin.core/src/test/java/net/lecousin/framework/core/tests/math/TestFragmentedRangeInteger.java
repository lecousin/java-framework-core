package net.lecousin.framework.core.tests.math;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.math.FragmentedRangeInteger;
import net.lecousin.framework.math.RangeInteger;

public class TestFragmentedRangeInteger extends LCCoreAbstractTest {

	@Test
	public void test() {
		FragmentedRangeInteger f = new FragmentedRangeInteger();
		Assert.assertEquals(0, f.size());
		f.addValue(12);
		Assert.assertEquals(1, f.size());
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
		f.addRanges(Arrays.asList(new RangeInteger(100, 150), new RangeInteger(200, 250)));
		Assert.assertEquals(3, f.size());
		f.addRange(175, 180);
		Assert.assertEquals(4, f.size());
		
		Assert.assertTrue(f.containsValue(9));
		Assert.assertTrue(f.containsValue(10));
		Assert.assertTrue(f.containsValue(100));
		Assert.assertTrue(f.containsValue(110));
		Assert.assertFalse(f.containsValue(0));
		Assert.assertFalse(f.containsValue(8));
		Assert.assertFalse(f.containsValue(50));
		Assert.assertFalse(f.containsValue(160));
		
		Assert.assertTrue(f.containsRange(100, 150));
		Assert.assertTrue(f.containsRange(101, 150));
		Assert.assertTrue(f.containsRange(110, 120));
		Assert.assertFalse(f.containsRange(99, 120));
		Assert.assertFalse(f.containsRange(130, 160));
	}
	
}
