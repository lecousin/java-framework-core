package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.io.serialization.CustomSerializer;
import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContextPattern;
import net.lecousin.framework.io.serialization.SerializationContextPattern.OnClass;
import net.lecousin.framework.io.serialization.SerializationContextPattern.OnClassAttribute;
import net.lecousin.framework.io.serialization.rules.CustomTypeSerializer;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/**
 * If isGeneral is true, the custom serializer will be applied generally, else
 * when declared on a class or interface the attributes of this class will use
 * the specified serializer if the type matches, and when declared on an
 * an attribute it applies only on this attribute.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Repeatable(TypeSerializers.class)
public @interface TypeSerializer {

	/** The serializer to use. */
	public Class<? extends CustomSerializer> value();
	
	/** Specify if the serializer has to be used more generally:
	 * I isGeneral is true the annotation will apply for any attribute in any class having the source type 
	 * of the serializer, else it only apply to attributes of the class if the annotation is on a class,
	 * or only to an attribute if the annotation is on an attribute.
	 */
	public boolean isGeneral() default false;
	
	/** Convert an annotation into a rule. */
	public static class ToRule implements TypeAnnotationToRule<TypeSerializer>, AttributeAnnotationToRuleOnAttribute<TypeSerializer> {
		
		@Override
		public SerializationRule createRule(TypeSerializer annotation, Class<?> type) {
			return createRule(annotation.value(), annotation.isGeneral() ? null : new OnClass(type));
		}
		
		@Override
		public SerializationRule createRule(TypeSerializer annotation, Attribute attribute) {
			return createRule(annotation.value(),
				annotation.isGeneral() ? null : new OnClassAttribute(attribute.getDeclaringClass(), attribute.getOriginalName()));
		}
		
		private static SerializationRule createRule(Class<? extends CustomSerializer> custom, SerializationContextPattern context) {
			try {
				return new CustomTypeSerializer(custom.newInstance(), context);
			} catch (Throwable t) {
				LCCore.getApplication().getDefaultLogger().error(
					"Error instantiating custom serializer " + custom.getName(), t
				);
				return null;
			}
		}
		
	}
	
}
