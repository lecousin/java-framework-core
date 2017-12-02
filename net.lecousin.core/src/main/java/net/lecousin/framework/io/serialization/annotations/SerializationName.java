package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.RenameAttribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
public @interface SerializationName {

	String value();
	
	public static class ToRule implements AttributeAnnotationToRuleOnType<SerializationName> {
		
		@Override
		public SerializationRule createRule(SerializationName annotation, Attribute attribute) {
			return new RenameAttribute(attribute.getDeclaringClass(), attribute.getOriginalName(), annotation.value());
		}
		
	}
	
}
