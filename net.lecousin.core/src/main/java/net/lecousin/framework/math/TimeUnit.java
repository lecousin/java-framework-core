package net.lecousin.framework.math;

/** Unit of time. */
public interface TimeUnit extends IntegerUnit {

	/** Milliseconds time unit. */
	public static final class Millisecond implements TimeUnit {
		private Millisecond() { /* no instance. */ }
	}
	
	/** Seconds time unit. */
	public static final class Second implements TimeUnit {
		private Second() { /* no instance. */ }
	}
	
	/** Minutes time unit. */
	public static final class Minute implements TimeUnit {
		private Minute() { /* no instance. */ }
	}
	
	/** Hour time unit. */
	public static final class Hour implements TimeUnit {
		private Hour() { /* no instance. */ }
	}
	
	/** Day time unit. */
	public static final class Day implements TimeUnit {
		private Day() { /* no instance. */ }
	}
	
	/** Converter between time units. */
	public static class Converter implements IntegerUnit.Converter {

		@Override
		@SuppressWarnings("squid:S1126")
		public boolean supportConversion(Class<? extends IntegerUnit> from, Class<? extends IntegerUnit> to) {
			if (!from.equals(Millisecond.class) &&
				!from.equals(Second.class) &&
				!from.equals(Minute.class) &&
				!from.equals(Hour.class) &&
				!from.equals(Day.class)
				)
				return false;
			if (!to.equals(Millisecond.class) &&
				!to.equals(Second.class) &&
				!to.equals(Minute.class) &&
				!to.equals(Hour.class) &&
				!to.equals(Day.class)
				)
				return false;
			return true;
		}

		@Override
		public long convert(long value, Class<? extends IntegerUnit> from, Class<? extends IntegerUnit> to)
		throws UnitConversionException {
			return convertFromMilliseconds(convertToMilliseconds(value, from), to);
		}
		
		/** Convert a value into milliseconds. */
		public long convertToMilliseconds(long value, Class<? extends IntegerUnit> unit) throws UnitConversionException {
			if (unit.equals(Millisecond.class))
				return value;
			if (unit.equals(Second.class))
				return value * 1000;
			if (unit.equals(Minute.class))
				return value * 60 * 1000;
			if (unit.equals(Hour.class))
				return value * 60 * 60 * 1000;
			if (unit.equals(Day.class))
				return value * 24 * 60 * 60 * 1000;
			throw new UnitConversionException(unit, Millisecond.class, value);
		}
		
		/** Convert a value from milliseconds. */
		public long convertFromMilliseconds(long value, Class<? extends IntegerUnit> unit) throws UnitConversionException {
			if (unit.equals(Millisecond.class))
				return value;
			if (unit.equals(Second.class))
				return value / 1000;
			if (unit.equals(Minute.class))
				return value / (60 * 1000);
			if (unit.equals(Hour.class))
				return value / (60 * 60 * 1000);
			if (unit.equals(Day.class))
				return value / (24 * 60 * 60 * 1000);
			throw new UnitConversionException(Millisecond.class, unit, value);
		}
		
	}
	
}
