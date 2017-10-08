package net.lecousin.framework.math;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Represent an integer unit. */
// TODO review this, and create individual unit converters by using an extension point.
public interface IntegerUnit {

	/** Annotation to specify in which unit a field is expressed. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD})
	public @interface Unit {
		/** Integer unit. */
		public Class<? extends IntegerUnit> value();
	}
	
	/** Error when converting from a unit to another. */
	public static class UnitConversionException extends Exception {
		
		private static final long serialVersionUID = -4651595394407140514L;

		/** Constructor. */
		public UnitConversionException(Class<? extends IntegerUnit> from, Class<? extends IntegerUnit> to, long value) {
			super("Cannot convert value " + value + " from " + from.getSimpleName() + " to " + to.getSimpleName());
		}
		
	}
	
	/** Convert a value into this unit. */
	public long convertFrom(long value, Class<? extends IntegerUnit> unit) throws UnitConversionException;
	
	/** Unit of time. */
	public interface Time extends IntegerUnit {
		
		/** Milliseconds time unit. */
		public static class Millisecond implements Time {
			@Override
			public long convertFrom(long value, Class<? extends IntegerUnit> unit) throws UnitConversionException {
				if (Millisecond.class.equals(unit))
					return value;
				if (Second.class.equals(unit))
					return value * 1000;
				if (Minute.class.equals(unit))
					return value * 60 * 1000;
				if (Hour.class.equals(unit))
					return value * 60 * 60 * 1000;
				if (Day.class.equals(unit))
					return value * 24 * 60 * 60 * 1000;
				throw new UnitConversionException(getClass(), unit, value);
			}
		}
		
		/** Seconds time unit. */
		public static class Second implements Time {
			@Override
			public long convertFrom(long value, Class<? extends IntegerUnit> unit) throws UnitConversionException {
				if (Millisecond.class.equals(unit))
					return value / 1000;
				if (Second.class.equals(unit))
					return value;
				if (Minute.class.equals(unit))
					return value * 60;
				if (Hour.class.equals(unit))
					return value * 60 * 60;
				if (Day.class.equals(unit))
					return value * 24 * 60 * 60;
				throw new UnitConversionException(getClass(), unit, value);
			}
		}
		
		/** Minutes time unit. */
		public static class Minute implements Time {
			@Override
			public long convertFrom(long value, Class<? extends IntegerUnit> unit) throws UnitConversionException {
				if (Millisecond.class.equals(unit))
					return value / (60 * 1000);
				if (Second.class.equals(unit))
					return value / 60;
				if (Minute.class.equals(unit))
					return value;
				if (Hour.class.equals(unit))
					return value * 60;
				if (Day.class.equals(unit))
					return value * 24 * 60;
				throw new UnitConversionException(getClass(), unit, value);
			}
		}
		
		/** Hour time unit. */
		public static class Hour implements Time {
			@Override
			public long convertFrom(long value, Class<? extends IntegerUnit> unit) throws UnitConversionException {
				if (Millisecond.class.equals(unit))
					return value / (60 * 60 * 1000);
				if (Second.class.equals(unit))
					return value / (60 * 60);
				if (Minute.class.equals(unit))
					return value / 60;
				if (Hour.class.equals(unit))
					return value;
				if (Day.class.equals(unit))
					return value * 24;
				throw new UnitConversionException(getClass(), unit, value);
			}
		}
		
		/** Day time unit. */
		public static class Day implements Time {
			@Override
			public long convertFrom(long value, Class<? extends IntegerUnit> unit) throws UnitConversionException {
				if (Millisecond.class.equals(unit))
					return value / (24 * 60 * 60 * 1000);
				if (Second.class.equals(unit))
					return value / (24 * 60 * 60);
				if (Minute.class.equals(unit))
					return value / (24 * 60);
				if (Hour.class.equals(unit))
					return value / 24;
				if (Day.class.equals(unit))
					return value;
				throw new UnitConversionException(getClass(), unit, value);
			}
		}
		
	}
	
}
