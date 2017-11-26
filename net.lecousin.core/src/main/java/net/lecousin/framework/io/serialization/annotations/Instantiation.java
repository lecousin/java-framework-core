package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.AbstractAttributeInstantiation;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.util.Factory;

/**
 * Allow to use another field as discriminator to know which class to instantiate when deserializing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
public @interface Instantiation {

	/** Path of the field to use as discriminator. */
	public String discriminator();
	
	/** Factory to create an instance based on the discriminator. */
	@SuppressWarnings("rawtypes")
	public Class<? extends Factory> factory();
	
	/** Convert an annotation into an AttributeInstantiation rule. */
	public static class ToRule implements AttributeAnnotationToRule<Instantiation> {
		
		@Override
		public SerializationRule createRule(Instantiation annotation, Attribute attribute) {
			try {
				return new AbstractAttributeInstantiation(
					attribute.getDeclaringClass(), attribute.getOriginalName(), annotation.discriminator(), annotation.factory()
				);
			} catch (Throwable t) {
				LCCore.get().getApplication().getDefaultLogger()
					.error("Error creating AbstractAttributeInstantiation from annotation Instantiation", t);
				return null;
			}
		}
	}
	
}
