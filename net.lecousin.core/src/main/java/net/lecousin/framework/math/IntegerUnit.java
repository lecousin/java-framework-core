package net.lecousin.framework.math;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;

import net.lecousin.framework.plugins.ExtensionPoint;
import net.lecousin.framework.plugins.Plugin;

/** Represent an integer unit. */
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
	
	/** Convert a value. */
	public static interface Converter extends Plugin {
		/** Return true if the conversion is supported. */
		boolean supportConversion(Class<? extends IntegerUnit> from, Class<? extends IntegerUnit> to);
		
		/** Convert a value. */
		long convert(long value, Class<? extends IntegerUnit> from, Class<? extends IntegerUnit> to) throws UnitConversionException;
	}
	
	/** Extension point from converters. */
	public static class ConverterRegistry implements ExtensionPoint<Converter> {

		/** Constructor. */
		public ConverterRegistry() {
			instance = this;
			converters.add(new TimeUnit.Converter());
		}

		private static ConverterRegistry instance;
		private ArrayList<Converter> converters = new ArrayList<>();
		
		@Override
		public Class<Converter> getPluginClass() { return Converter.class; }

		@Override
		public void addPlugin(Converter plugin) { converters.add(plugin); }

		@Override
		public void allPluginsLoaded() {
			// nothing to do
		}
		
		/** Conversion. */
		public static long convert(long value, Class<? extends IntegerUnit> from, Class<? extends IntegerUnit> to)
		throws UnitConversionException {
			for (Converter c : instance.converters)
				if (c.supportConversion(from, to))
					return c.convert(value, from, to);
			throw new UnitConversionException(from, to, value);
		}
		
	}
	
}
