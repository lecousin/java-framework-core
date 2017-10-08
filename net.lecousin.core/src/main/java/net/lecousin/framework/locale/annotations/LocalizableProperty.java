package net.lecousin.framework.locale.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Attach a property, with a localizable string. */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(LocalizableProperties.class)
public @interface LocalizableProperty {

	/** Name of the property. */
	String name();
	
	/** Namespace for localization. */
	String namespace() default "";
	
	/** Key for localization. */
	String key();
	
	/** Values for localization. */
	String[] values() default {};
	
}
