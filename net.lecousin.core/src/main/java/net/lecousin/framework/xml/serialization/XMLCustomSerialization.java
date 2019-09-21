package net.lecousin.framework.xml.serialization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Specify a custom XML serialization. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface XMLCustomSerialization {

	/** Custom serializer. */
	Class<? extends XMLCustomSerializer> value();
	
}
