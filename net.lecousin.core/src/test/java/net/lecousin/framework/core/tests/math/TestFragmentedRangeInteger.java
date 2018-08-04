package net.lecousin.framework.core.tests.math;

import java.util.Arrays;
import java.util.List;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.math.FragmentedRangeInteger;
import net.lecousin.framework.math.RangeInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestFragmentedRangeInteger extends LCCoreAbstractTest {

	@Test(timeout=30000)
	public void test() throws Exception {
		FragmentedRangeInteger f = new FragmentedRangeInteger();
		Assert.assertEquals(0, f.size());
		Assert.assertEquals(Integer.MAX_VALUE, f.getMin());
		Assert.assertEquals(Integer.MIN_VALUE, f.getMax());
		Assert.assertNull(f.removeFirstValue());
		Assert.assertEquals(0, FragmentedRangeInteger.intersect(new FragmentedRangeInteger(), new FragmentedRangeInteger()).size());
		Assert.assertNull(f.removeBiggestRange());
		f.remove(10, 20);
		// 12
		f.addValue(12);
		Assert.assertEquals(1, f.size());
		Assert.assertEquals(0, FragmentedRangeInteger.intersect(f, new FragmentedRangeInteger()).size());
		Assert.assertEquals(0, FragmentedRangeInteger.intersect(new FragmentedRangeInteger(), f).size());
		Assert.assertEquals(12, f.removeFirstValue().intValue());
		Assert.assertEquals(0, f.size());
		f.addValue(12);
		Assert.assertEquals(1, f.size());
		// 10-15
		f.addRange(new RangeInteger(10, 15));
		Assert.assertEquals(1, f.size());
		f = new FragmentedRangeInteger();
		f.addRange(new RangeInteger(10, 15));
		Assert.assertEquals(1, f.size());
		// remove
		Assert.assertEquals(new RangeInteger(10, 15), f.removeBiggestRange());
		Assert.assertEquals(0, f.size());
		// 10-15
		f.addRange(new RangeInteger(10, 15));
		Assert.assertEquals(1, f.size());
		// 10-20
		f.addRange(new RangeInteger(16, 20));
		Assert.assertEquals(1, f.size());
		// 10-22, 22-30
		f.addRange(new RangeInteger(22, 30));
		Assert.assertEquals(2, f.size());
		// 10-30
		f.addValue(21);
		Assert.assertEquals(1, f.size());
		// 9-30
		f.addValue(9);
		Assert.assertEquals(1, f.size());
		// 9-31
		f.addValue(31);
		Assert.assertEquals(1, f.size());
		// 9-31, 100-150, 200-250
		f.addRanges(Arrays.asList(new RangeInteger(100, 150), new RangeInteger(200, 250)));
		Assert.assertEquals(3, f.size());
		// 9-31, 100-150, 175-180, 200-250
		f.addRange(175, 180);
		Assert.assertEquals(4, f.size());
		// 9-31, 100-150, 175-180, 190-250
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
		Assert.assertTrue(f.containsRange(500, 120));

		// 9-31, 100-155, 175-180, 190-250
		f.addRange(151, 155);
		Assert.assertEquals(4, f.size());
		// 9-31, 100-155, 157, 175-180, 190-250
		f.addValue(157);
		Assert.assertEquals(5, f.size());
		// 10-31, 100-155, 157, 175-180, 190-250
		Assert.assertEquals(9, f.removeFirstValue().intValue());
		Assert.assertEquals(5, f.size());
		Assert.assertFalse(f.containsValue(9));
		Assert.assertTrue(f.containsValue(10));
		// 11-31, 100-155, 157, 175-180, 190-250
		f.removeValue(10);
		Assert.assertEquals(5, f.size());
		Assert.assertFalse(f.containsValue(10));
		Assert.assertTrue(f.containsValue(11));
		// 14-31, 100-155, 157, 175-180, 190-250
		f.remove(11, 13);
		Assert.assertEquals(5, f.size());
		Assert.assertFalse(f.containsValue(13));
		Assert.assertTrue(f.containsValue(14));
		// 14-31, 100-155, 157, 175-250
		f.addRange(181, 189);
		check(f, new RangeInteger(14, 31), new RangeInteger(100, 155), new RangeInteger(157, 157), new RangeInteger(175, 250));
		// 14-31, 100-155, 157-300
		f.addRange(158, 300);
		check(f, new RangeInteger(14, 31), new RangeInteger(100, 155), new RangeInteger(157, 300));
		// 14-31, 100-155, 157-199, 251-300
		f.remove(200, 250);
		check(f, new RangeInteger(14, 31), new RangeInteger(100, 155), new RangeInteger(157, 199), new RangeInteger(251, 300));
		// 14-31, 100-155, 157-300
		f.addRange(200, 270);
		check(f, new RangeInteger(14, 31), new RangeInteger(100, 155), new RangeInteger(157, 300));
		// 14-31, 100-155, 157-199, 251-300
		f.remove(200, 250);
		check(f, new RangeInteger(14, 31), new RangeInteger(100, 155), new RangeInteger(157, 199), new RangeInteger(251, 300));
		// 14-31, 100-155, 157-400
		f.addRange(180, 400);
		check(f, new RangeInteger(14, 31), new RangeInteger(100, 155), new RangeInteger(157, 400));
		// 14-31, 100-155, 157-400, 500-500
		f.addValue(500);
		check(f, new RangeInteger(14, 31), new RangeInteger(100, 155), new RangeInteger(157, 400), new RangeInteger(500, 500));
		
		f.addRange(159, 162);
		check(f, new RangeInteger(14, 31), new RangeInteger(100, 155), new RangeInteger(157, 400), new RangeInteger(500, 500));
		
		check(FragmentedRangeInteger.intersect(f, new FragmentedRangeInteger(new RangeInteger(90, 450))),
			new RangeInteger(100, 155), new RangeInteger(157, 400));
		FragmentedRangeInteger f2 = new FragmentedRangeInteger();
		f2.add(new RangeInteger(18, 20));
		f2.add(new RangeInteger(200, 300));
		f2.add(new RangeInteger(350, 450));
		check(FragmentedRangeInteger.intersect(f, f2), new RangeInteger(18, 20), new RangeInteger(200, 300), new RangeInteger(350, 400));
		check(f.copy(), new RangeInteger(14, 31), new RangeInteger(100, 155), new RangeInteger(157, 400), new RangeInteger(500, 500));
		
		Assert.assertNull(f.removeBestRangeForSize(1000));
		Assert.assertEquals(new RangeInteger(100, 155), f.removeBestRangeForSize(56));
		Assert.assertEquals(new RangeInteger(157, 356), f.removeBestRangeForSize(200));
		check(f, new RangeInteger(14, 31), new RangeInteger(357, 400), new RangeInteger(500, 500));
		Assert.assertEquals(new RangeInteger(357, 400), f.removeBiggestRange());
		check(f, new RangeInteger(14, 31), new RangeInteger(500, 500));
		Assert.assertEquals(19, f.getTotalSize());
		f.addCopy(Arrays.asList(new RangeInteger(100, 120), new RangeInteger(130, 140)));
		check(f, new RangeInteger(14, 31), new RangeInteger(100, 120), new RangeInteger(130, 140), new RangeInteger(500, 500));
		f.toString();
		// so far = 14-31, 100-120, 130-140, 500-500
		// 14-31, 100-117, 130-140, 500-500
		f.remove(118, 125);
		check(f, new RangeInteger(14, 31), new RangeInteger(100, 117), new RangeInteger(130, 140), new RangeInteger(500, 500));
		// 14-31, 104-117, 130-140, 500-500
		f.remove(80, 103);
		check(f, new RangeInteger(14, 31), new RangeInteger(104, 117), new RangeInteger(130, 140), new RangeInteger(500, 500));
		// 14-31, 130-140, 500-500
		f.remove(80, 117);
		check(f, new RangeInteger(14, 31), new RangeInteger(130, 140), new RangeInteger(500, 500));
		// 14-31, 500-500
		f.remove(125, 145);
		check(f, new RangeInteger(14, 31), new RangeInteger(500, 500));
		
		f = new FragmentedRangeInteger();
		f.addRange(10, 20);
		f.addRange(30, 40);
		f.addRange(50, 60);
		f.addRange(70, 80);
		f.addRange(90, 100);
		f.addRange(25, 75);
		check(f, new RangeInteger(10, 20), new RangeInteger(25, 80), new RangeInteger(90, 100));
		f.addRange(24, 85);
		check(f, new RangeInteger(10, 20), new RangeInteger(24, 85), new RangeInteger(90, 100));
		f.addValue(21);
		check(f, new RangeInteger(10, 21), new RangeInteger(24, 85), new RangeInteger(90, 100));
		f.addValue(15);
		check(f, new RangeInteger(10, 21), new RangeInteger(24, 85), new RangeInteger(90, 100));
		f.remove(19, 21);
		check(f, new RangeInteger(10, 18), new RangeInteger(24, 85), new RangeInteger(90, 100));
		f.remove(10, 18);
		check(f, new RangeInteger(24, 85), new RangeInteger(90, 100));
		f.remove(24, 87);
		check(f, new RangeInteger(90, 100));
		
		try { f = new FragmentedRangeInteger.Parser().parse("{hello}"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { f = new FragmentedRangeInteger.Parser().parse("world"); throw new AssertionError("Error case"); } catch (Exception e) {}
		try { f = new FragmentedRangeInteger.Parser().parse("{[10-20]"); throw new AssertionError("Error case"); } catch (Exception e) {}
		f = new FragmentedRangeInteger.Parser().parse(null);
		check(f);
		f = new FragmentedRangeInteger.Parser().parse("");
		check(f);
		f = new FragmentedRangeInteger.Parser().parse("[10-20]");
		check(f, new RangeInteger(10, 20));
		f = new FragmentedRangeInteger.Parser().parse("[10-20],[30-40]");
		check(f, new RangeInteger(10, 20), new RangeInteger(30, 40));
		f = new FragmentedRangeInteger.Parser().parse("[10-20],");
		check(f, new RangeInteger(10, 20));
		f = new FragmentedRangeInteger.Parser().parse("{[10-20], ,[30-40],}");
		check(f, new RangeInteger(10, 20), new RangeInteger(30, 40));
		f = new FragmentedRangeInteger.Parser().parse("{[10-20],[30-40],[12-18]},[50-60]");
		check(f, new RangeInteger(10, 20), new RangeInteger(30, 40));
	}
	
	private static void check(List<RangeInteger> list, RangeInteger... expected) {
		Assert.assertEquals(expected.length, list.size());
		for (int i = 0; i < expected.length; ++i) {
			Assert.assertEquals("Range " + i + " start", expected[i].min, list.get(i).min);
			Assert.assertEquals("Range " + i + " end", expected[i].max, list.get(i).max);
		}
	}
	
}
