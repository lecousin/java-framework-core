package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Allow for multiple AddAttribute annotations. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AddAttributes {

	/** List of AddAttribute. */
	AddAttribute[] value();
	
}
