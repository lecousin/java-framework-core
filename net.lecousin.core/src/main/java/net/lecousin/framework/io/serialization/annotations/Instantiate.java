package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.AttributeInstantiation;
import net.lecousin.framework.io.serialization.rules.SerializationRule;
import net.lecousin.framework.util.Factory;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Instantiate {

	/** Factory to create an instance based on the container instance. */
	@SuppressWarnings("rawtypes")
	public Class<? extends Factory> factory();
	
	public static class ToRule implements AttributeAnnotationToRuleOnType<Instantiate> {
		
		@Override
		public SerializationRule createRule(Instantiate annotation, Attribute attribute) {
			try {
				return new AttributeInstantiation(
					attribute.getDeclaringClass(), attribute.getOriginalName(), annotation.factory()
				);
			} catch (Throwable t) {
				LCCore.get().getApplication().getDefaultLogger()
					.error("Error creating AttributeInstantiation from annotation Instantiate", t);
				return null;
			}
		}
		
	}
	
}
