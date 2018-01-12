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
		Assert.assertEquals(Integer.MAX_VALUE, f.getMin());
		Assert.assertEquals(Integer.MIN_VALUE, f.getMax());
		Assert.assertNull(f.removeFirstValue());
		f.addValue(12);
		Assert.assertEquals(1, f.size());
		f.addRange(new RangeInteger(10, 15));
		Assert.assertEquals(1, f.size());
		f = new FragmentedRangeInteger();
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
		f.addRange(190, 199);
		Assert.assertEquals(4, f.size());
		
		Assert.assertEquals(9, f.getMin());
		Assert.assertEquals(250, f.getMax());
		
		Assert.assertTrue(f.containsValue(9));
		Assert.assertTrue(f.containsValue(10));
		Assert.assertTrue(f.containsValue(100));
		Assert.assertTrue(f.containsValue(110));
		Assert.assertTrue(f.containsValue(195));
		Assert.assertFalse(f.containsValue(0));
		Assert.assertFalse(f.containsValue(8));
		Assert.assertFalse(f.containsValue(50));
		Assert.assertFalse(f.containsValue(160));
		Assert.assertFalse(f.containsValue(300));
		
		Assert.assertTrue(f.containsRange(100, 150));
		Assert.assertTrue(f.containsRange(101, 150));
		Assert.assertTrue(f.containsRange(110, 120));
		Assert.assertFalse(f.containsRange(99, 120));
		Assert.assertFalse(f.containsRange(130, 160));
		Assert.assertFalse(f.containsRange(300, 400));

		f.addRange(151, 155);
		Assert.assertEquals(4, f.size());
		f.addValue(157);
		Assert.assertEquals(5, f.size());
		Assert.assertEquals(9, f.removeFirstValue().intValue());
		Assert.assertEquals(5, f.size());
		Assert.assertFalse(f.containsValue(9));
		Assert.assertTrue(f.containsValue(10));
		f.removeValue(10);
		Assert.assertEquals(5, f.size());
		Assert.assertFalse(f.containsValue(10));
		Assert.assertTrue(f.containsValue(11));
		f.remove(11, 13);
		Assert.assertEquals(5, f.size());
		Assert.assertFalse(f.containsValue(13));
		Assert.assertTrue(f.containsValue(14));
	}
	
}
