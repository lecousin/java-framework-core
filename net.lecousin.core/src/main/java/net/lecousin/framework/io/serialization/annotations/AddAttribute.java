package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.io.serialization.rules.AddAttributeToType;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/**
 * Add a custom attribute to a class.
 * The serializer and deserializer are methods on the class that can be used for this attribute.
 * An empty method is allowed in case the serialization or deserialization is not used.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(AddAttributes.class)
public @interface AddAttribute {

	/** Name of the attribute. */
	String name();
	
	/** Method to get the attribute's value, or empty string. */
	String serializer() default "";
	
	/** Method to set the attribute's value, or empty string. */
	String deserializer() default "";
	
	/** Convert the annotation into a serialization rule. */
	public static class ToRule implements TypeAnnotationToRule<AddAttribute> {
		
		@Override
		public SerializationRule createRule(AddAttribute annotation, Class<?> type) {
			return new AddAttributeToType(type, annotation.name(), annotation.serializer(), annotation.deserializer());
		}
		
	}
	
}
