package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.io.serialization.rules.CustomSerializer;

/**
 * When declared on a class or interface, it means that when serializing or deserializing
 * objects this specified serializer should be used if the type matches.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Repeatable(TypeSerializers.class)
public @interface TypeSerializer {

	/** The serializer to use. */
	public Class<? extends CustomSerializer<?,?>> value();
	
}
