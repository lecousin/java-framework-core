package net.lecousin.framework.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;

/**
 * Parser from a String into the specified type T.
 * @param <T> type
 */
public interface StringParser<T> {

	/** Parse from a String into type T. */
	T parse(String string) throws ParseException;
	
	/** Mark a constructor having a String as parameter to be able to parse a String. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.CONSTRUCTOR })
	public static @interface Parse {
		
	}
	
	/** Parse a string.
	 * @throws IllegalArgumentException if the class cannot be instantiated
	 * @throws ParseException if parsing failed
	 */
	@SuppressWarnings("unchecked")
	static <T> T parse(Class<T> type, StringFormat format, String string) throws ParseException {
		StringParser<?> parser;
		if (!format.pattern().isEmpty()) {
			try {
				parser = format.parser().getConstructor(String.class).newInstance(format.pattern());
			} catch (Exception e) {
				throw new IllegalArgumentException("Cannot instantiate StringParser class "
					+ format.parser().getName() + " with pattern " + format.pattern(), e);
			}
		} else {
			try {
				parser = format.parser().newInstance();
			} catch (Exception e) {
				throw new IllegalArgumentException("Cannot instantiate StringParser class " + format.parser().getName(), e);
			}
		}
		Object o = parser.parse(string);
		if (o != null && !type.isAssignableFrom(o.getClass()))
			throw new IllegalArgumentException("StringParser class " + format.parser().getName()
				+ " is not compatible with target type " + type.getName());
		return (T)o;
	}
	
	/** Parse a string.
	 * @throws IllegalArgumentException in case the parse method invocation failed
	 */
	@SuppressWarnings("unchecked")
	static <T> T parse(Class<T> type, String string) {
		// look for a static parse(String) method
		Method parse;
		try {
			parse = type.getMethod("parse", String.class);
			if ((parse.getModifiers() & Modifier.STATIC) == 0 || !type.isAssignableFrom(parse.getReturnType()))
				parse = null;
		} catch (Exception e) {
			parse = null;
		}
		Object o;
		if (parse != null) {
			try {
				o = parse.invoke(null, string);
			} catch (Exception e) {
				throw new IllegalArgumentException("Method parse on class " + type.getName() + " error", e);
			}
		} else {
			// look for a constructor with Parse annotation
			Constructor<T> ctor;
			try {
				ctor = type.getConstructor(String.class);
				if (ctor.getAnnotation(Parse.class) == null)
					ctor = null;
			} catch (Exception e) {
				ctor = null;
			}
			if (ctor == null)
				throw new IllegalArgumentException("Class " + type.getName()
					+ " does not provide a static parse method neither a Parse constructor");
			try {
				o = ctor.newInstance(string);
			} catch (Exception e) {
				throw new IllegalArgumentException("Constructor of class " + type.getName() + " error", e);
			}
		}
		return (T)o;
	}
	
}
