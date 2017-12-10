package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.RenameAttribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Specify a different name for serialization. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
public @interface SerializationName {

	/** The new name. */
	String value();
	
	/** Convert an annotation into a rule. */
	public static class ToRule implements AttributeAnnotationToRuleOnType<SerializationName> {
		
		@Override
		public SerializationRule createRule(SerializationName annotation, Attribute attribute) {
			return new RenameAttribute(attribute.getDeclaringClass(), attribute.getOriginalName(), annotation.value());
		}
		
	}
	
}
