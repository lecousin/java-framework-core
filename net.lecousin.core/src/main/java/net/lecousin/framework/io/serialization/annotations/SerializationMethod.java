package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.SerializationUtil.Attribute;
import net.lecousin.framework.io.serialization.rules.CustomAttributeSerializer;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Specify a method to use for serialization. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
public @interface SerializationMethod {

	/** Method to use for serialization. */
	public String value();
	
	/** Convert an annotation into a CustomAttributeSerialization rule. */
	public static class ToRule implements AnnotationPlugin<SerializationMethod> {
		
		@Override
		public Class<SerializationMethod> getAnnotationType() {
			return SerializationMethod.class;
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public SerializationRule getRule(Attribute attribute, SerializationMethod annotation) {
			try {
				Class sourceType = attribute.getType();
				return new net.lecousin.framework.io.serialization.rules.CustomAttributeSerialization(
					attribute.getDeclaringClass(), attribute.getOriginalName(),
					new CustomAttributeSerializer() {
						@Override
						public Class sourceType() {
							return sourceType;
						}
						
						@Override
						public Class targetType() {
							try {
								Method m = attribute.getType().getMethod(annotation.value());
								return m.getReturnType();
							} catch (Throwable t) {
								LCCore.getApplication().getDefaultLogger()
									.error("Error getting return type of method " + annotation.value()
										+ " on class " + attribute.getType(), t);
								return Object.class;
							}
						}
	
						@Override
						public Object serialize(Object src) {
							if (src == null)
								return null;
							try {
								Method m = attribute.getType().getMethod(annotation.value());
								return m.invoke(src);
							} catch (Throwable t) {
								LCCore.getApplication().getDefaultLogger()
								.error("Error calling method " + annotation.value()
									+ " on class " + attribute.getType(), t);
								return null;
							}
						}
	
						@Override
						public Object deserialize(Object src) {
							if (src == null)
								return null;
							try {
								Class cl = sourceType();
								@SuppressWarnings("unchecked")
								Constructor ctor = cl.getConstructor(targetType());
								return ctor.newInstance(src);
							} catch (Throwable t) {
								LCCore.getApplication().getDefaultLogger()
									.error("Error instantiating type " + sourceType(), t);
								return null;
							}
						}
					}
				);
			} catch (Throwable t) {
				LCCore.getApplication().getDefaultLogger().error("Error creating CustomAttributeSerialization from annotation", t);
				return null;
			}
		}
		
	}
	
}
