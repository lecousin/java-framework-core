package net.lecousin.framework.locale.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * List of LocalizableProperty.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalizableProperties {

	/** List. */
	LocalizableProperty[] value();
	
}
