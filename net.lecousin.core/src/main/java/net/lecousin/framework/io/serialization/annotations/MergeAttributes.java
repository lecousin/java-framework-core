package net.lecousin.framework.io.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.framework.io.serialization.SerializationClass.Attribute;
import net.lecousin.framework.io.serialization.SerializationContextPattern;
import net.lecousin.framework.io.serialization.rules.MergeTypeAttributes;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

/** Merge into an attribute all other attributes to create a single object. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD,ElementType.TYPE})
public @interface MergeAttributes {

	/** Type to merge from. */
	Class<?> type();
	
	/** Target attribute. */
	String target();
	
	/** Convert an annotation into a rule. */
	public static class ToRule implements AttributeAnnotationToRuleOnType<MergeAttributes> {
		
		@Override
		public SerializationRule createRule(MergeAttributes annotation, Attribute attribute) {
			return new MergeTypeAttributes(new SerializationContextPattern.OnClassAttribute(attribute.getDeclaringClass(), attribute.getOriginalName()), annotation.type(), annotation.target());
		}
		
	}
	
}
