package net.lecousin.framework.text;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declare how to parse from a String, and how to generate a String from an Object.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
public @interface StringFormat {

	/** Parser. */
	Class<? extends StringParser<?>> parser();

	/** Optional pattern: the format class must have a constructor with only one parameter which is a String. */
	String pattern() default "";
	
}
