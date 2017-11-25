package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** List of serializers. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface TypeSerializers {

	/** Serializers. */
	public TypeSerializer[] value();
	
}
