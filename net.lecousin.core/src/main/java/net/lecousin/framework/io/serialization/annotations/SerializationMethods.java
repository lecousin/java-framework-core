package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.CustomSerializer;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.rules.CustomAttributeSerializer;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Specify methods to use for serialization and deserialization of an attribute.
 * The methods must exist on the same class as the attribute.
 * The serialization method must not have parameters, and its returned type will be used as the serialized type.
 * The deserialization method must have a single parameter having the same type as the returned type of
 * the serialization method, and must return a type of the attribute.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
public @interface SerializationMethods {

	/** Method to use for serialization. */
	public String serialization();

	/** Method to use for deserialization. */
	public String deserialization();
	
	/** Convert an annotation into a CustomAttributeSerialization rule. */
	public static class ToRule implements AttributeAnnotationToRuleOnType<SerializationMethods> {
		
		@Override
		public SerializationRule createRule(SerializationMethods annotation, Attribute attribute) {
			try {
				TypeDefinition sourceType = attribute.getOriginalType();
				Method serializationMethod = sourceType.getBase().getMethod(annotation.serialization());
				TypeDefinition targetType = new TypeDefinition(serializationMethod.getGenericReturnType());
				Method deserializationMethod = sourceType.getBase().getMethod(annotation.deserialization(), targetType.getBase());
				if (!deserializationMethod.getReturnType().equals(sourceType.getBase()))
					throw new Exception("Deserialization method " + deserializationMethod.getName()
						+ " must return a value of type " + sourceType.getBase().getName());
				return new CustomAttributeSerializer(
					attribute.getDeclaringClass(), attribute.getOriginalName(),
					new CustomSerializer() {
						@Override
						public TypeDefinition sourceType() {
							return sourceType;
						}
						
						@Override
						public TypeDefinition targetType() {
							return targetType;
						}
	
						@Override
						public Object serialize(Object src, Object containerInstance) {
							if (src == null)
								return null;
							try {
								if ((serializationMethod.getModifiers() & Modifier.STATIC) != 0)
									return serializationMethod.invoke(null);
								return serializationMethod.invoke(containerInstance);
							} catch (Throwable t) {
								LCCore.getApplication().getDefaultLogger()
								.error("Error calling method " + serializationMethod.getName()
									+ " on class " + serializationMethod.getDeclaringClass().getName(), t);
								return null;
							}
						}
	
						@Override
						public Object deserialize(Object src, Object containerInstance) {
							if (src == null)
								return null;
							try {
								if ((deserializationMethod.getModifiers() & Modifier.STATIC) != 0)
									return deserializationMethod.invoke(null, src);
								return deserializationMethod.invoke(containerInstance, src);
							} catch (Throwable t) {
								LCCore.getApplication().getDefaultLogger()
								.error("Error calling method " + deserializationMethod.getName()
									+ " on class " + deserializationMethod.getDeclaringClass().getName(), t);
								return null;
							}
						}
					}
				);
			} catch (Throwable t) {
				LCCore.getApplication().getDefaultLogger().error(
					"Error creating CustomAttributeSerializer from annotation SerializationMethods", t);
				return null;
			}
		}
		
	}
	
}
