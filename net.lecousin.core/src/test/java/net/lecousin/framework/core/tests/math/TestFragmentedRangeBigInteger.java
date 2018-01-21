package net.lecousin.framework.core.tests.math;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

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
		Assert.assertEquals(BigInteger.ZERO, f.getMin());
		Assert.assertEquals(BigInteger.ZERO, f.getMax());
		Assert.assertNull(f.removeFirstValue());
		// 12
		f.addValue(BigInteger.valueOf(12));
		Assert.assertEquals(1, f.size());
		// 10-15
		f.addRange(new RangeBigInteger(BigInteger.valueOf(10), BigInteger.valueOf(15)));
		Assert.assertEquals(1, f.size());
		f = new FragmentedRangeBigInteger();
		// 10-15
		f.addRange(new RangeBigInteger(BigInteger.valueOf(10), BigInteger.valueOf(15)));
		Assert.assertEquals(1, f.size());
		// 10-20
		f.addRange(new RangeBigInteger(BigInteger.valueOf(16), BigInteger.valueOf(20)));
		Assert.assertEquals(1, f.size());
		// 10-20, 22-30
		f.addRange(new RangeBigInteger(BigInteger.valueOf(22), BigInteger.valueOf(30)));
		Assert.assertEquals(2, f.size());
		// 10-30
		f.addValue(BigInteger.valueOf(21));
		Assert.assertEquals(1, f.size());
		// 9-30
		f.addValue(BigInteger.valueOf(9));
		Assert.assertEquals(1, f.size());
		// 9-31
		f.addValue(BigInteger.valueOf(31));
		Assert.assertEquals(1, f.size());

		// 9-31, 100-150, 200-250
		f.addRanges(Arrays.asList(new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(150)), new RangeBigInteger(BigInteger.valueOf(200), BigInteger.valueOf(250))));
		Assert.assertEquals(3, f.size());
		// 9-31, 100-150, 175-180, 200-250
		f.addRange(BigInteger.valueOf(175), BigInteger.valueOf(180));
		Assert.assertEquals(4, f.size());
		// 9-31, 100-150, 175-180, 190-250
		f.addRange(BigInteger.valueOf(190), BigInteger.valueOf(199));
		Assert.assertEquals(4, f.size());
		
		Assert.assertEquals(BigInteger.valueOf(9), f.getMin());
		Assert.assertEquals(BigInteger.valueOf(250), f.getMax());

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
		
		Assert.assertTrue(f.containsRange(BigInteger.valueOf(100), BigInteger.valueOf(150)));
		Assert.assertTrue(f.containsRange(BigInteger.valueOf(101), BigInteger.valueOf(150)));
		Assert.assertTrue(f.containsRange(BigInteger.valueOf(110), BigInteger.valueOf(120)));
		Assert.assertFalse(f.containsRange(BigInteger.valueOf(99), BigInteger.valueOf(120)));
		Assert.assertFalse(f.containsRange(BigInteger.valueOf(130), BigInteger.valueOf(160)));
		Assert.assertFalse(f.containsRange(BigInteger.valueOf(300), BigInteger.valueOf(400)));

		// 9-31, 100-155, 175-180, 190-250
		f.addRange(BigInteger.valueOf(151), BigInteger.valueOf(155));
		Assert.assertEquals(4, f.size());
		// 9-31, 100-155, 157, 175-180, 190-250
		f.addValue(BigInteger.valueOf(157));
		Assert.assertEquals(5, f.size());
		// 10-31, 100-155, 157, 175-180, 190-250
		Assert.assertEquals(9, f.removeFirstValue().intValue());
		Assert.assertEquals(5, f.size());
		Assert.assertFalse(f.containsValue(9));
		Assert.assertTrue(f.containsValue(10));
		// 11-31, 100-155, 157, 175-180, 190-250
		f.removeValue(BigInteger.valueOf(10));
		Assert.assertEquals(5, f.size());
		Assert.assertFalse(f.containsValue(10));
		Assert.assertTrue(f.containsValue(11));
		// 14-31, 100-155, 157, 175-180, 190-250
		f.removeRange(BigInteger.valueOf(11), BigInteger.valueOf(13));
		Assert.assertEquals(5, f.size());
		Assert.assertFalse(f.containsValue(13));
		Assert.assertTrue(f.containsValue(14));
		// 14-31, 100-155, 157, 175-250
		f.addRange(BigInteger.valueOf(181), BigInteger.valueOf(189));
		check(f, new RangeBigInteger(BigInteger.valueOf(14), BigInteger.valueOf(31)),
			new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(155)),
			new RangeBigInteger(BigInteger.valueOf(157), BigInteger.valueOf(157)),
			new RangeBigInteger(BigInteger.valueOf(175), BigInteger.valueOf(250)));
		// 14-31, 100-155, 157-300
		f.addRange(BigInteger.valueOf(158), BigInteger.valueOf(300));
		check(f, new RangeBigInteger(BigInteger.valueOf(14), BigInteger.valueOf(31)),
			new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(155)),
			new RangeBigInteger(BigInteger.valueOf(157), BigInteger.valueOf(300)));
		// 14-31, 100-155, 157-199, 251-300
		f.removeRange(BigInteger.valueOf(200), BigInteger.valueOf(250));
		check(f, new RangeBigInteger(BigInteger.valueOf(14), BigInteger.valueOf(31)),
			new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(155)),
			new RangeBigInteger(BigInteger.valueOf(157), BigInteger.valueOf(199)),
			new RangeBigInteger(BigInteger.valueOf(251), BigInteger.valueOf(300)));
		// 14-31, 100-155, 157-300
		f.addRange(BigInteger.valueOf(200), BigInteger.valueOf(270));
		check(f, new RangeBigInteger(BigInteger.valueOf(14), BigInteger.valueOf(31)),
			new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(155)),
			new RangeBigInteger(BigInteger.valueOf(157), BigInteger.valueOf(300)));
		// 14-31, 100-155, 157-199, 251-300
		f.removeRange(BigInteger.valueOf(200), BigInteger.valueOf(250));
		check(f, new RangeBigInteger(BigInteger.valueOf(14), BigInteger.valueOf(31)),
			new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(155)),
			new RangeBigInteger(BigInteger.valueOf(157), BigInteger.valueOf(199)),
			new RangeBigInteger(BigInteger.valueOf(251), BigInteger.valueOf(300)));
		// 14-31, 100-155, 157-400
		f.addRange(BigInteger.valueOf(180), BigInteger.valueOf(400));
		check(f, new RangeBigInteger(BigInteger.valueOf(14), BigInteger.valueOf(31)),
			new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(155)),
			new RangeBigInteger(BigInteger.valueOf(157), BigInteger.valueOf(400)));
		// 14-31, 100-155, 157-400, 500-500
		f.addValue(BigInteger.valueOf(500));
		check(f, new RangeBigInteger(BigInteger.valueOf(14), BigInteger.valueOf(31)),
			new RangeBigInteger(BigInteger.valueOf(100), BigInteger.valueOf(155)),
			new RangeBigInteger(BigInteger.valueOf(157), BigInteger.valueOf(400)),
			new RangeBigInteger(BigInteger.valueOf(500), BigInteger.valueOf(500)));
	}
	
	private static void check(List<RangeBigInteger> list, RangeBigInteger... expected) {
		Assert.assertEquals(expected.length, list.size());
		for (int i = 0; i < expected.length; ++i) {
			Assert.assertEquals("Range " + i + " start", expected[i].min, list.get(i).min);
			Assert.assertEquals("Range " + i + " end", expected[i].max, list.get(i).max);
		}
	}
	
}
