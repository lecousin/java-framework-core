package net.lecousin.framework.locale.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Set the default localization namespace for a class.
 * When this annotation is set on a class, elements of this class may omit to specify a namespace on other
 * annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LocalizableNamespace {

	/** Default localization namespace. */
	String value();
	
}
