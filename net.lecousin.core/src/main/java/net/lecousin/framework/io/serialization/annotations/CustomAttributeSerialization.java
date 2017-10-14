package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;
import net.lecousin.framework.io.serialization.rules.CustomAttributeSerializer;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Specify a {@link CustomAttributeSerializer} for the field. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
public @interface CustomAttributeSerialization {

	/** CustomAttributeSerializer to use. */
	public Class<? extends CustomAttributeSerializer<?,?>> value();
	
	/** Convert the annotation into a CustomAttributeSerialization rule. */
	public static class ToRule implements AnnotationPlugin<CustomAttributeSerialization> {
		
		@Override
		public Class<CustomAttributeSerialization> getAnnotationType() {
			return CustomAttributeSerialization.class;
		}
		
		@Override
		public SerializationRule getRule(Attribute attribute, CustomAttributeSerialization annotation) {
			try {
				return new net.lecousin.framework.io.serialization.rules.CustomAttributeSerialization(
					attribute.getDeclaringClass(), attribute.getOriginalName(), annotation.value().newInstance()
				);
			} catch (Throwable t) {
				LCCore.get().getApplication().getDefaultLogger()
					.error("Error creating CustomAttributeSerialization rule from annotation", t);
				return null;
			}
		}
		
	}
	
}
