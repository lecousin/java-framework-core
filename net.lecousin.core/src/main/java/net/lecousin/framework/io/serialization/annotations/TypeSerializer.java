package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.CustomSerializer;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.rules.CustomTypeSerializer;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/**
 * When declared on a class or interface, it means that when serializing or deserializing
 * objects this specified serializer should be used if the type matches.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Repeatable(TypeSerializers.class)
public @interface TypeSerializer {

	/** The serializer to use. */
	public Class<? extends CustomSerializer> value();
	
	public static class ToRule implements TypeAnnotationToRule<TypeSerializer>, AttributeAnnotationToRule<TypeSerializer> {
		
		@Override
		public SerializationRule createRule(TypeSerializer annotation, Class<?> type) {
			return createRule(annotation.value());
		}
		
		@Override
		public SerializationRule createRule(TypeSerializer annotation, Attribute attribute) {
			return createRule(annotation.value());
		}
		
		private static SerializationRule createRule(Class<? extends CustomSerializer> custom) {
			try {
				return new CustomTypeSerializer(custom.newInstance());
			} catch (Throwable t) {
				LCCore.getApplication().getDefaultLogger().error(
					"Error instantiating custom serializer " + custom.getName(), t
				);
				return null;
			}
		}
		
	}
	
}
