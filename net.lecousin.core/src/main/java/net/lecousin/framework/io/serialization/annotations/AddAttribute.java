package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.io.serialization.rules.AddAttributeToType;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(AddAttributes.class)
public @interface AddAttribute {

	String name();
	
	String serializer() default "";
	
	String deserializer() default "";
	
	public static class ToRule implements TypeAnnotationToRule<AddAttribute> {
		
		@Override
		public SerializationRule createRule(AddAttribute annotation, Class<?> type) {
			return new AddAttributeToType(type, annotation.name(), annotation.serializer(), annotation.deserializer());
		}
		
	}
	
}
