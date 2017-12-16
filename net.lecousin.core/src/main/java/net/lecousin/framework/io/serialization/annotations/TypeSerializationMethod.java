package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.CustomSerializer;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.TypeDefinition;
import net.lecousin.framework.io.serialization.rules.CustomAttributeSerializer;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Specify a method to use for serialization of an attribute.
 * The method must exist on the type of the attribute without parameter.
 * The type returned by the method will be used as the serialized type.
 * The type of the attribute must also have a contructor with a single parameter having
 * the same type as the type returned by the method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
public @interface TypeSerializationMethod {

	/** Method to use for serialization. */
	public String value();
	
	/** Convert an annotation into a CustomAttributeSerialization rule. */
	public static class ToRule implements AttributeAnnotationToRuleOnType<TypeSerializationMethod> {
		
		@Override
		public SerializationRule createRule(TypeSerializationMethod annotation, Attribute attribute) {
			try {
				TypeDefinition sourceType = attribute.getOriginalType();
				Method method = sourceType.getBase().getMethod(annotation.value());
				TypeDefinition targetType = new TypeDefinition(sourceType, method.getGenericReturnType());
				Constructor<?> ctor = sourceType.getBase().getConstructor(targetType.getBase());
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
								return method.invoke(src);
							} catch (Throwable t) {
								LCCore.getApplication().getDefaultLogger()
								.error("Error calling method " + method.getName()
									+ " on class " + method.getDeclaringClass().getName(), t);
								return null;
							}
						}
	
						@Override
						public Object deserialize(Object src, Object containerInstance) {
							if (src == null)
								return null;
							try {
								return ctor.newInstance(src);
							} catch (Throwable t) {
								LCCore.getApplication().getDefaultLogger()
									.error("Error instantiating type " + sourceType.getBase().getName(), t);
								return null;
							}
						}
					}
				);
			} catch (Throwable t) {
				LCCore.getApplication().getDefaultLogger().error(
					"Error creating CustomAttributeSerializer from annotation TypeSerializationMethod", t);
				return null;
			}
		}
		
	}
	
}
