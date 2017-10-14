package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;
import net.lecousin.framework.io.serialization.rules.IgnoreAttribute;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Indicate that a field should not be serialized.
 * This annotation can be used directly on the field, or on its getter or setter method(s).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
public @interface Transient {

	/** Convert the annotation Transient into a IgnoreAttribute rule. */
	public static class ToRule implements AnnotationPlugin<Transient> {
		
		@Override
		public Class<Transient> getAnnotationType() {
			return Transient.class;
		}
		
		@Override
		public SerializationRule getRule(Attribute attribute, Transient annotation) {
			try {
				return new IgnoreAttribute(attribute.getDeclaringClass(), attribute.getOriginalName());
			} catch (Throwable t) {
				LCCore.getApplication().getDefaultLogger().error("Error creating IgnoreAttribute from annotation", t);
				return null;
			}
		}
	}
	
}
