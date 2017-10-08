package net.lecousin.framework.properties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Property annotation allowing to associate a name to a value.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Property {

	/** Name of the property. */
	String name();
	
	/** Value of the property. */
	String value();
	
}
