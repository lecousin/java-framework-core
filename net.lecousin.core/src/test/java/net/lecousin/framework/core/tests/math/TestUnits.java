package net.lecousin.framework.core.tests.math;

import org.junit.Assert;
import org.junit.Test;

import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.math.IntegerUnit;
import net.lecousin.framework.math.IntegerUnit.UnitConversionException;
import net.lecousin.framework.math.TimeUnit;
import net.lecousin.framework.math.TimeUnit.Day;
import net.lecousin.framework.math.TimeUnit.Hour;
import net.lecousin.framework.math.TimeUnit.Millisecond;
import net.lecousin.framework.math.TimeUnit.Minute;
import net.lecousin.framework.math.TimeUnit.Second;

public class TestUnits extends LCCoreAbstractTest {
	
	@Test
	public void testTimeUnitsConversions() throws UnitConversionException {
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Millisecond.class, Millisecond.class));
		Assert.assertEquals(10, IntegerUnit.ConverterRegistry.convert(10, Millisecond.class, Millisecond.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Millisecond.class, Second.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(10, Millisecond.class, Second.class));
		Assert.assertEquals(2, IntegerUnit.ConverterRegistry.convert(2875, Millisecond.class, Second.class));
		Assert.assertEquals(851, IntegerUnit.ConverterRegistry.convert(851472, Millisecond.class, Second.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Millisecond.class, Minute.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(100, Millisecond.class, Minute.class));
		Assert.assertEquals(2, IntegerUnit.ConverterRegistry.convert(159874, Millisecond.class, Minute.class));
		Assert.assertEquals(-1, IntegerUnit.ConverterRegistry.convert(-112000, Millisecond.class, Minute.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Millisecond.class, Hour.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(189324, Millisecond.class, Hour.class));
		Assert.assertEquals(3, IntegerUnit.ConverterRegistry.convert(11800000, Millisecond.class, Hour.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Millisecond.class, Day.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(20800000, Millisecond.class, Day.class));
		Assert.assertEquals(2, IntegerUnit.ConverterRegistry.convert(200000000, Millisecond.class, Day.class));
		
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Second.class, Millisecond.class));
		Assert.assertEquals(10000, IntegerUnit.ConverterRegistry.convert(10, Second.class, Millisecond.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Second.class, Second.class));
		Assert.assertEquals(10, IntegerUnit.ConverterRegistry.convert(10, Second.class, Second.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Second.class, Minute.class));
		Assert.assertEquals(2, IntegerUnit.ConverterRegistry.convert(150, Second.class, Minute.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Second.class, Hour.class));
		Assert.assertEquals(7, IntegerUnit.ConverterRegistry.convert(25200, Second.class, Hour.class));
		Assert.assertEquals(7, IntegerUnit.ConverterRegistry.convert(26000, Second.class, Hour.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Second.class, Day.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(26000, Second.class, Day.class));
		Assert.assertEquals(3, IntegerUnit.ConverterRegistry.convert(260000, Second.class, Day.class));

		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Minute.class, Millisecond.class));
		Assert.assertEquals(600000, IntegerUnit.ConverterRegistry.convert(10, Minute.class, Millisecond.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Minute.class, Second.class));
		Assert.assertEquals(180, IntegerUnit.ConverterRegistry.convert(3, Minute.class, Second.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Minute.class, Minute.class));
		Assert.assertEquals(10, IntegerUnit.ConverterRegistry.convert(10, Minute.class, Minute.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Minute.class, Hour.class));
		Assert.assertEquals(2, IntegerUnit.ConverterRegistry.convert(150, Minute.class, Hour.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Minute.class, Day.class));
		Assert.assertEquals(1, IntegerUnit.ConverterRegistry.convert(1440, Minute.class, Day.class));
		Assert.assertEquals(2, IntegerUnit.ConverterRegistry.convert(3000, Minute.class, Day.class));

		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Hour.class, Millisecond.class));
		Assert.assertEquals(10800000, IntegerUnit.ConverterRegistry.convert(3, Hour.class, Millisecond.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Hour.class, Second.class));
		Assert.assertEquals(21600, IntegerUnit.ConverterRegistry.convert(6, Hour.class, Second.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Hour.class, Minute.class));
		Assert.assertEquals(120, IntegerUnit.ConverterRegistry.convert(2, Hour.class, Minute.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Hour.class, Hour.class));
		Assert.assertEquals(36, IntegerUnit.ConverterRegistry.convert(36, Hour.class, Hour.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Hour.class, Day.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(21, Hour.class, Day.class));
		Assert.assertEquals(3, IntegerUnit.ConverterRegistry.convert(73, Hour.class, Day.class));

		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Day.class, Millisecond.class));
		Assert.assertEquals(345600000, IntegerUnit.ConverterRegistry.convert(4, Day.class, Millisecond.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Day.class, Second.class));
		Assert.assertEquals(259200, IntegerUnit.ConverterRegistry.convert(3, Day.class, Second.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Day.class, Minute.class));
		Assert.assertEquals(10080, IntegerUnit.ConverterRegistry.convert(7, Day.class, Minute.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Day.class, Hour.class));
		Assert.assertEquals(336, IntegerUnit.ConverterRegistry.convert(14, Day.class, Hour.class));
		Assert.assertEquals(0, IntegerUnit.ConverterRegistry.convert(0, Day.class, Day.class));
		Assert.assertEquals(500, IntegerUnit.ConverterRegistry.convert(500, Day.class, Day.class));
	}
	
	public static class FakeUnit implements IntegerUnit {
		
	}
	
	@Test
	public void testFakeUnit() {
		try {
			IntegerUnit.ConverterRegistry.convert(125, Second.class, FakeUnit.class);
			throw new AssertionError("conversion with FakeUnit should not be possible");
		} catch (UnitConversionException e) {}
		try {
			IntegerUnit.ConverterRegistry.convert(862, FakeUnit.class, Hour.class);
			throw new AssertionError("conversion with FakeUnit should not be possible");
		} catch (UnitConversionException e) {}
		try {
			new TimeUnit.Converter().convertFromMilliseconds(147, FakeUnit.class);
			throw new AssertionError("conversion with FakeUnit should not be possible");
		} catch (UnitConversionException e) {}
		try {
			new TimeUnit.Converter().convertToMilliseconds(852, FakeUnit.class);
			throw new AssertionError("conversion with FakeUnit should not be possible");
		} catch (UnitConversionException e) {}
	}

}
