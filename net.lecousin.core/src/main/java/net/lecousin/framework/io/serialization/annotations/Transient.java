package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.IgnoreAttribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Indicate that an attribute should not be serialized.
 * This annotation can be used directly on the field, or on its getter or setter method(s).
 * This is equivalent to declare the field with the transient modifier.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
public @interface Transient {

	/** Convert a Transient annotation on a field to a {@link SerializationRule}. */
	public static class ToRule implements AttributeAnnotationToRule<Transient> {
		
		@Override
		public SerializationRule createRule(Transient annotation, Attribute attribute) {
			return new IgnoreAttribute(attribute.getDeclaringClass(), attribute.getOriginalName());
		}
		
	}
	
}
